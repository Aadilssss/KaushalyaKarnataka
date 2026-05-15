package com.kaushalyakarnataka.app.data

import com.google.firebase.Timestamp

enum class UserRole {
    worker,
    customer,
}

enum class RequestStatus {
    pending,
    accepted,
    completed,
    rejected,
}

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.customer,
    val category: String? = null,
    val location: String = "",
    val profileImage: String? = null,
    val rating: Double = 0.0,
    val ratingCount: Long? = null,
    val totalRatingValue: Double? = null,
    val jobsCompleted: Long = 0,
    val bio: String? = null,
    val fcmToken: String? = null,
)

data class ServiceItem(
    val serviceId: String,
    val userId: String,
    val title: String,
    val price: Double,
    val description: String,
)

data class PortfolioItem(
    val portfolioId: String,
    val userId: String,
    val imageUrl: String,
    val description: String,
)

data class JobRequest(
    val requestId: String,
    val workerId: String,
    val customerId: String,
    val status: RequestStatus,
    val chatEnabled: Boolean = false,
    val callEnabled: Boolean = false,
    val createdAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
)

data class ChatThread(
    val chatId: String,
    val participants: List<String>,
)

data class ChatMessage(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val message: String,
    val timestamp: Timestamp? = null,
)

data class ReviewEntry(
    val reviewId: String = "",
    val workerId: String = "",
    val customerId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Timestamp? = null,
)
