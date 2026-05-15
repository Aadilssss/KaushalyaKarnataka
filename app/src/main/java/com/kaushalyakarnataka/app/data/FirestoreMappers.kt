package com.kaushalyakarnataka.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Senior Developer Note: Case-insensitive enum mapping is a best practice.
 * It prevents returning users from being treated as new users due to 
 * minor data formatting differences in the database.
 */

private fun String?.toUserRole(): UserRole {
    val raw = this?.lowercase() ?: "customer"
    return try {
        UserRole.valueOf(raw)
    } catch (e: Exception) {
        UserRole.customer
    }
}

private fun String?.toRequestStatus(): RequestStatus {
    val raw = this?.lowercase() ?: "pending"
    return try {
        RequestStatus.valueOf(raw)
    } catch (e: Exception) {
        RequestStatus.pending
    }
}

fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    
    return UserProfile(
        userId = id,
        name = getString(FirestoreFields.NAME) ?: "User",
        phone = getString(FirestoreFields.PHONE) ?: "",
        role = getString(FirestoreFields.ROLE).toUserRole(),
        category = getString(FirestoreFields.CATEGORY),
        location = getString(FirestoreFields.LOCATION) ?: "",
        profileImage = getString(FirestoreFields.PROFILE_IMAGE),
        rating = getDouble(FirestoreFields.RATING) ?: 0.0,
        ratingCount = getLong(FirestoreFields.RATING_COUNT),
        totalRatingValue = getDouble(FirestoreFields.TOTAL_RATING_VALUE),
        jobsCompleted = getLong(FirestoreFields.JOBS_COMPLETED) ?: 0,
        bio = getString("bio"),
        fcmToken = getString("fcmToken"),
    )
}

fun DocumentSnapshot.toJobRequest(): JobRequest =
    JobRequest(
        requestId = id,
        workerId = getString(FirestoreFields.WORKER_ID) ?: "",
        customerId = getString(FirestoreFields.CUSTOMER_ID) ?: "",
        status = getString(FirestoreFields.STATUS).toRequestStatus(),
        chatEnabled = getBoolean("chatEnabled") ?: false,
        callEnabled = getBoolean("callEnabled") ?: false,
        createdAt = getTimestamp(FirestoreFields.CREATED_AT),
        completedAt = getTimestamp(FirestoreFields.COMPLETED_AT),
    )

fun DocumentSnapshot.toServiceItem(): ServiceItem =
    ServiceItem(
        serviceId = id,
        userId = getString(FirestoreFields.USER_ID) ?: "",
        title = getString("title") ?: "",
        price = getDouble("price") ?: 0.0,
        description = getString("description") ?: "",
    )

fun DocumentSnapshot.toPortfolioItem(): PortfolioItem =
    PortfolioItem(
        portfolioId = id,
        userId = getString(FirestoreFields.USER_ID) ?: "",
        imageUrl = getString("imageUrl") ?: "",
        description = getString("description") ?: "",
    )

fun DocumentSnapshot.toChatMessage(chatId: String): ChatMessage =
    ChatMessage(
        messageId = id,
        chatId = chatId,
        senderId = getString("senderId") ?: "",
        message = getString("message") ?: "",
        timestamp = getTimestamp("timestamp"),
    )

fun DocumentSnapshot.toReview(): ReviewEntry =
    ReviewEntry(
        reviewId = id,
        workerId = getString(FirestoreFields.WORKER_ID) ?: "",
        customerId = getString(FirestoreFields.CUSTOMER_ID) ?: "",
        rating = (getLong(FirestoreFields.RATING) ?: 0L).toInt(),
        comment = getString("comment") ?: "",
        createdAt = getTimestamp(FirestoreFields.CREATED_AT),
    )

fun Timestamp?.millisOrZero(): Long = this?.toDate()?.time ?: 0L
