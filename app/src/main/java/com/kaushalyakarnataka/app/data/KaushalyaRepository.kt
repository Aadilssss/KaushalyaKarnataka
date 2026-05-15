package com.kaushalyakarnataka.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.snapshots
import com.kaushalyakarnataka.app.FirebaseBootstrap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID

object FirestoreCollections {
    const val USERS = "users"
    const val REQUESTS = "requests"
    const val CHATS = "chats"
    const val MESSAGES = "messages"
    const val REVIEWS = "reviews"
    const val SERVICES = "services"
    const val PORTFOLIO = "portfolio"
}

object FirestoreFields {
    const val USER_ID = "userId"
    const val CUSTOMER_ID = "customerId"
    const val WORKER_ID = "workerId"
    const val STATUS = "status"
    const val ROLE = "role"
    const val CREATED_AT = "createdAt"
    const val COMPLETED_AT = "completedAt"
    const val PARTICIPANTS = "participants"
    const val RATING = "rating"
    const val NAME = "name"
    const val PHONE = "phone"
    const val LOCATION = "location"
    const val CATEGORY = "category"
    const val JOBS_COMPLETED = "jobsCompleted"
    const val PROFILE_IMAGE = "profileImage"
    const val RATING_COUNT = "ratingCount"
    const val TOTAL_RATING_VALUE = "totalRatingValue"
}

/**
 * Result wrapper to handle cache/server synchronization professionally.
 */
data class ProfileSyncResult(
    val profile: UserProfile?,
    val isConfirmedNewUser: Boolean,
    val isFromCache: Boolean
)

class KaushalyaRepository(
    private val auth: FirebaseAuth = FirebaseBootstrap.auth,
    private val db: FirebaseFirestore = FirebaseBootstrap.firestore,
) {
    private val TAG = "KaushalyaRepo"

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun signInWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    fun signOut() {
        auth.signOut()
    }

    fun observeAuthUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            trySend(fbAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Resilient real-time profile observer. 
     * Correctly handles cache vs server transitions using Metadata.
     */
    fun observeProfileResilient(uid: String): Flow<ProfileSyncResult> =
        db.collection(FirestoreCollections.USERS).document(uid)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap ->
                val exists = snap.exists()
                val isFromCache = snap.metadata.isFromCache
                
                Log.d(TAG, "Sync Update: uid=$uid, exists=$exists, isFromCache=$isFromCache")

                ProfileSyncResult(
                    profile = if (exists) snap.toUserProfile() else null,
                    // User is confirmed NEW only when server (non-cache) confirms no document.
                    isConfirmedNewUser = !exists && !isFromCache,
                    isFromCache = isFromCache
                )
            }

    suspend fun mergeUserProfile(uid: String, fields: Map<String, Any?>) {
        try {
            val cleaned = fields.filterValues { it != null }.mapValues { it.value!! }
            val withId = cleaned + (FirestoreFields.USER_ID to uid)
            db.collection(FirestoreCollections.USERS).document(uid)
                .set(withId, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error merging profile", e)
            throw e
        }
    }

    suspend fun fetchWorkers(): List<UserProfile> {
        val snap = db.collection(FirestoreCollections.USERS)
            .whereEqualTo(FirestoreFields.ROLE, UserRole.worker.name)
            .get()
            .await()
        return snap.documents.mapNotNull { it.toUserProfile() }
    }

    suspend fun fetchUser(userId: String): UserProfile? {
        val snap = db.collection(FirestoreCollections.USERS).document(userId).get().await()
        return if (snap.exists()) snap.toUserProfile() else null
    }

    suspend fun fetchReviewsForWorker(workerId: String, limit: Long = 5): List<ReviewEntry> {
        val snap = db.collection(FirestoreCollections.REVIEWS)
            .whereEqualTo(FirestoreFields.WORKER_ID, workerId)
            .orderBy(FirestoreFields.CREATED_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snap.documents.map { it.toReview() }
    }

    suspend fun fetchServices(workerId: String): List<ServiceItem> {
        val snap = db.collection(FirestoreCollections.SERVICES)
            .whereEqualTo(FirestoreFields.USER_ID, workerId)
            .get()
            .await()
        return snap.documents.map { it.toServiceItem() }
    }

    suspend fun fetchPortfolio(workerId: String): List<PortfolioItem> {
        val snap = db.collection(FirestoreCollections.PORTFOLIO)
            .whereEqualTo(FirestoreFields.USER_ID, workerId)
            .get()
            .await()
        return snap.documents.map { it.toPortfolioItem() }
    }

    suspend fun hireWorker(customerId: String, workerId: String): Result<Unit> = runCatching {
        val existing = db.collection(FirestoreCollections.REQUESTS)
            .whereEqualTo(FirestoreFields.CUSTOMER_ID, customerId)
            .whereEqualTo(FirestoreFields.WORKER_ID, workerId)
            .whereIn(FirestoreFields.STATUS, listOf(RequestStatus.pending.name, RequestStatus.accepted.name))
            .get()
            .await()
        
        if (!existing.isEmpty) throw Exception("duplicate_request")

        db.collection(FirestoreCollections.REQUESTS).add(
            mapOf(
                FirestoreFields.CUSTOMER_ID to customerId,
                FirestoreFields.WORKER_ID to workerId,
                FirestoreFields.STATUS to RequestStatus.pending.name,
                FirestoreFields.CREATED_AT to FieldValue.serverTimestamp(),
                "chatEnabled" to false,
                "callEnabled" to false,
            ),
        ).await()
    }

    fun observeRequestsForWorker(workerId: String): Flow<List<JobRequest>> =
        db.collection(FirestoreCollections.REQUESTS)
            .whereEqualTo(FirestoreFields.WORKER_ID, workerId)
            .snapshots()
            .map { qs ->
                qs.documents.map { it.toJobRequest() }
                    .sortedByDescending { it.createdAt.millisOrZero() }
            }

    fun observeAcceptedJobs(workerId: String): Flow<List<JobRequest>> =
        db.collection(FirestoreCollections.REQUESTS)
            .whereEqualTo(FirestoreFields.WORKER_ID, workerId)
            .whereEqualTo(FirestoreFields.STATUS, RequestStatus.accepted.name)
            .snapshots()
            .map { qs -> qs.documents.map { it.toJobRequest() } }

    fun observeRequestsForCustomer(customerId: String): Flow<List<JobRequest>> =
        db.collection(FirestoreCollections.REQUESTS)
            .whereEqualTo(FirestoreFields.CUSTOMER_ID, customerId)
            .snapshots()
            .map { qs ->
                qs.documents.map { it.toJobRequest() }
                    .sortedByDescending { it.createdAt.millisOrZero() }
            }

    fun observeReviewsByCustomer(customerId: String): Flow<List<ReviewEntry>> =
        db.collection(FirestoreCollections.REVIEWS)
            .whereEqualTo(FirestoreFields.CUSTOMER_ID, customerId)
            .snapshots()
            .map { qs ->
                qs.documents.map { it.toReview() }
            }

    suspend fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        enableChatCall: Boolean = false,
    ) {
        val updates = mutableMapOf<String, Any>(FirestoreFields.STATUS to status.name)
        if (enableChatCall) {
            updates["chatEnabled"] = true
            updates["callEnabled"] = true
        }
        if (status == RequestStatus.completed) {
            updates[FirestoreFields.COMPLETED_AT] = FieldValue.serverTimestamp()
            updates["chatEnabled"] = false
            updates["callEnabled"] = false
        }
        db.collection(FirestoreCollections.REQUESTS).document(requestId).update(updates).await()
    }

    suspend fun ensureChat(workerId: String, customerId: String) {
        val existing = db.collection(FirestoreCollections.CHATS)
            .whereArrayContains(FirestoreFields.PARTICIPANTS, workerId)
            .get()
            .await()
        
        val already = existing.documents.any { doc ->
            @Suppress("UNCHECKED_CAST")
            (doc.get(FirestoreFields.PARTICIPANTS) as? List<String>)?.contains(customerId) == true
        }
        
        if (!already) {
            db.collection(FirestoreCollections.CHATS).add(
                mapOf(
                    FirestoreFields.PARTICIPANTS to listOf(workerId, customerId),
                    FirestoreFields.CREATED_AT to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    suspend fun acceptRequest(requestId: String, workerId: String, customerId: String) {
        ensureChat(workerId, customerId)
        updateRequestStatus(requestId, RequestStatus.accepted, enableChatCall = true)
    }

    suspend fun rejectRequest(requestId: String) {
        updateRequestStatus(requestId, RequestStatus.rejected)
    }

    suspend fun completeJob(requestId: String, workerId: String) {
        db.collection(FirestoreCollections.REQUESTS).document(requestId).update(
            mapOf(
                FirestoreFields.STATUS to RequestStatus.completed.name,
                FirestoreFields.COMPLETED_AT to FieldValue.serverTimestamp(),
                "chatEnabled" to false,
                "callEnabled" to false,
            ),
        ).await()
        
        db.collection(FirestoreCollections.USERS).document(workerId).update(
            FirestoreFields.JOBS_COMPLETED,
            FieldValue.increment(1),
        ).await()
    }

    fun observeChats(uid: String): Flow<List<ChatThread>> =
        db.collection(FirestoreCollections.CHATS)
            .whereArrayContains(FirestoreFields.PARTICIPANTS, uid)
            .snapshots()
            .map { qs ->
                qs.documents.map { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val parts = (doc.get(FirestoreFields.PARTICIPANTS) as? List<String>).orEmpty()
                    ChatThread(chatId = doc.id, participants = parts)
                }
            }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
        db.collection(FirestoreCollections.CHATS).document(chatId)
            .collection(FirestoreCollections.MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { qs ->
                qs.documents.map { it.toChatMessage(chatId) }
            }

    suspend fun sendMessage(chatId: String, senderId: String, text: String) {
        db.collection(FirestoreCollections.CHATS).document(chatId)
            .collection(FirestoreCollections.MESSAGES).add(
                mapOf(
                    "chatId" to chatId,
                    "senderId" to senderId,
                    "message" to text,
                    "timestamp" to FieldValue.serverTimestamp(),
                ),
            ).await()
    }

    suspend fun fetchChatParticipantIds(chatId: String): List<String> {
        val doc = db.collection(FirestoreCollections.CHATS).document(chatId).get().await()
        @Suppress("UNCHECKED_CAST")
        return (doc.get(FirestoreFields.PARTICIPANTS) as? List<String>).orEmpty()
    }

    suspend fun findChatIdBetween(uid: String, otherUid: String): String? {
        val snap = db.collection(FirestoreCollections.CHATS).whereArrayContains(FirestoreFields.PARTICIPANTS, uid).get().await()
        return snap.documents.firstOrNull { doc ->
            @Suppress("UNCHECKED_CAST")
            val parts = (doc.get(FirestoreFields.PARTICIPANTS) as? List<String>).orEmpty()
            parts.contains(otherUid)
        }?.id
    }

    suspend fun submitReview(
        workerId: String,
        customerId: String,
        rating: Int,
        comment: String,
    ) {
        val dup = db.collection(FirestoreCollections.REVIEWS)
            .whereEqualTo(FirestoreFields.WORKER_ID, workerId)
            .whereEqualTo(FirestoreFields.CUSTOMER_ID, customerId)
            .get()
            .await()
        
        if (!dup.isEmpty) throw Exception("already_rated")

        db.collection(FirestoreCollections.REVIEWS).add(
            mapOf(
                FirestoreFields.WORKER_ID to workerId,
                FirestoreFields.CUSTOMER_ID to customerId,
                FirestoreFields.RATING to rating,
                "comment" to comment,
                FirestoreFields.CREATED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()

        val workerRef = db.collection(FirestoreCollections.USERS).document(workerId)
        val workerSnap = workerRef.get().await()
        val count = (workerSnap.getLong(FirestoreFields.RATING_COUNT) ?: 0L) + 1
        val total = (workerSnap.getDouble(FirestoreFields.TOTAL_RATING_VALUE) ?: 0.0) + rating
        val avg = String.format(Locale.US, "%.1f", total / count).toDouble()

        workerRef.update(
            mapOf(
                FirestoreFields.RATING to avg,
                FirestoreFields.RATING_COUNT to FieldValue.increment(1),
                FirestoreFields.TOTAL_RATING_VALUE to FieldValue.increment(rating.toDouble()),
            ),
        ).await()
    }

    fun observeServices(uid: String): Flow<List<ServiceItem>> =
        db.collection(FirestoreCollections.SERVICES)
            .whereEqualTo(FirestoreFields.USER_ID, uid)
            .snapshots()
            .map { qs ->
                qs.documents.map { it.toServiceItem() }
            }

    fun observePortfolio(uid: String): Flow<List<PortfolioItem>> =
        db.collection(FirestoreCollections.PORTFOLIO)
            .whereEqualTo(FirestoreFields.USER_ID, uid)
            .snapshots()
            .map { qs ->
                qs.documents.map { it.toPortfolioItem() }
            }

    suspend fun addService(uid: String, title: String, price: Double, description: String) {
        db.collection(FirestoreCollections.SERVICES).add(
            mapOf(
                FirestoreFields.USER_ID to uid,
                "title" to title,
                "price" to price,
                "description" to description,
            ),
        ).await()
    }

    suspend fun deleteService(serviceId: String) {
        db.collection(FirestoreCollections.SERVICES).document(serviceId).delete().await()
    }

    suspend fun addPortfolioItem(uid: String, imageUrl: String, description: String) {
        db.collection(FirestoreCollections.PORTFOLIO).add(
            mapOf(
                FirestoreFields.USER_ID to uid,
                "imageUrl" to imageUrl,
                "description" to description,
            ),
        ).await()
    }

    suspend fun deletePortfolio(portfolioId: String) {
        db.collection(FirestoreCollections.PORTFOLIO).document(portfolioId).delete().await()
    }

    /**
     * Professional image upload with automatic compression.
     * Reduces large phone photos (5MB+) to optimized professional sizes (approx 200KB).
     */
    suspend fun uploadImage(bytes: ByteArray, pathPrefix: String): String {
        try {
            // COMPRESSION STEP: Resize and compress image for fast upload
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            
            // Standard professional size: Max 1024px
            var sampleSize = 1
            while (options.outWidth / (sampleSize * 2) >= 1024) { sampleSize *= 2 }
            
            val compressedOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, compressedOptions)
            
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val optimizedBytes = outputStream.toByteArray()

            // UPLOAD STEP
            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = FirebaseBootstrap.storage.reference.child("$pathPrefix/$fileName")
            ref.putBytes(optimizedBytes).await()
            
            return ref.getDownloadUrl().await().toString()
        } catch (e: Exception) {
            Log.e(TAG, "Image upload and compression failed", e)
            throw e
        }
    }
}
