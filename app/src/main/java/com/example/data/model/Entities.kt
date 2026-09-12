package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class UserRole {
    CITIZEN,
    LAW_ENFORCER,
    ADMIN
}

@Entity(tableName = "users")
@Serializable
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isApproved: Boolean = false,
    val profileImageUrl: String? = null,
    val biometricEnabled: Boolean = false,
    val badgeNumber: String? = null,
    val idCardUrl: String? = null,
    val contact: String? = null,
    val address: String? = null,
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED, BANNED
    val reputationScore: Int = 100
)

@Entity(tableName = "audit_logs")
@Serializable
data class AuditLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val action: String,
    val description: String,
    val userId: String,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Serializable
enum class ReportType {
    MISSING_PERSON,
    WANTED_INDIVIDUAL,
    SUSPICIOUS_ACTIVITY,
    CRIME_REPORT,
    DRUGS_DEALER,
    HIT_AND_RUN,
    THEFT,
    BURGLARY,
    ARMED_ROBBERY,
    KIDNAPPING,
    FRAUD,
    VANDALISM,
    RAPE,
    CAR_ACCIDENT,
    SUICIDE,
    DOMESTIC_VIOLENCE,
    ASSAULT
}

@Entity(tableName = "reports")
@Serializable
data class Report(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val type: ReportType,
    val county: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val reporterId: String,
    val isAnonymous: Boolean = false,
    val status: String = "PENDING", // PENDING, UNDER_INVESTIGATION, RESOLVED, ARRESTED, CLOSED
    val imageUrls: List<String> = emptyList(),
    val videoUrls: List<String> = emptyList(),
    val audioUrls: List<String> = emptyList(),
    val incidentTimestamp: Long = System.currentTimeMillis(),
    val aiScreeningStatus: String = "NOT_SCREENED", // NOT_SCREENED, CLEAN, FLAGGED
    val aiFlaggedReasons: List<String> = emptyList(),
    val internalNotes: String? = null,
    val assignedInvestigator: String? = null,
    val isEscalated: Boolean = false,
    val contactInfo: String? = null,
    val isDeleted: Boolean = false
)

@Entity(tableName = "tips")
@Serializable
data class Tip(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val reportId: String,
    val content: String,
    val isAnonymous: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val submitterId: String? = null,
    val isReviewed: Boolean = false,
    val reviewTimestamp: Long? = null,
    val aiScreeningStatus: String = "NOT_SCREENED", // NOT_SCREENED, CLEAN, FLAGGED
    val aiFlaggedReasons: List<String> = emptyList(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "alerts")
@Serializable
data class Alert(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val urgency: Int, // 1: Low, 2: Medium, 3: High
    val locationName: String,
    val county: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wanted_criminals")
@Serializable
data class WantedCriminal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val lastSeen: String,
    val county: String? = null,
    val reward: String? = null,
    val imageUrls: List<String> = emptyList(),
    val isArrested: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val category: String = "Wanted person notices",
    val status: String = "SUBMITTED", // SUBMITTED, VERIFIED, DISMISSED
    val isVerified: Boolean = false
)

@Entity(tableName = "help_messages")
@Serializable
data class HelpMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val senderEmail: String,
    val senderContact: String = "",
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
