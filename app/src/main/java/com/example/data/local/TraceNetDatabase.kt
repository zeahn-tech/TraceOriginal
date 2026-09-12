package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Dao
interface TraceNetDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserByIdOnce(userId: String): User?

    @Query("SELECT * FROM reports WHERE id = :reportId")
    suspend fun getReportByIdOnce(reportId: String): Report?

    @Query("SELECT * FROM tips WHERE id = :tipId")
    suspend fun getTipByIdOnce(tipId: String): Tip?

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun getAlertByIdOnce(id: String): Alert?

    @Query("SELECT * FROM wanted_criminals WHERE id = :id")
    suspend fun getWantedCriminalByIdOnce(id: String): WantedCriminal?

    @Query("SELECT * FROM audit_logs WHERE id = :id")
    suspend fun getAuditLogByIdOnce(id: String): AuditLog?

    @Query("SELECT * FROM help_messages WHERE id = :id")
    suspend fun getHelpMessageByIdOnce(id: String): HelpMessage?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("UPDATE reports SET title = :title, description = :description WHERE id = :reportId")
    suspend fun updateReport(reportId: String, title: String, description: String)

    @Query("SELECT * FROM reports WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getActiveReports(): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedReports(): Flow<List<Report>>

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<Report>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report)

    @Update
    suspend fun updateReport(report: Report)

    @Query("UPDATE reports SET isDeleted = 1 WHERE id = :reportId")
    suspend fun softDeleteReport(reportId: String)

    @Query("UPDATE reports SET isDeleted = 0 WHERE id = :reportId")
    suspend fun restoreReport(reportId: String)

    @Query("DELETE FROM reports WHERE id = :reportId")
    suspend fun permanentlyDeleteReport(reportId: String)

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<Alert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: Alert)

    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: String)

    @Query("UPDATE alerts SET title = :title, content = :content, urgency = :urgency, locationName = :locationName, latitude = :latitude, longitude = :longitude WHERE id = :id")
    suspend fun updateAlert(id: String, title: String, content: String, urgency: Int, locationName: String, latitude: Double, longitude: Double)

    @Query("SELECT * FROM wanted_criminals ORDER BY timestamp DESC")
    fun getAllWantedCriminals(): Flow<List<WantedCriminal>>

    @Query("SELECT * FROM wanted_criminals WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getActiveWantedCriminals(): Flow<List<WantedCriminal>>

    @Query("SELECT * FROM wanted_criminals WHERE isDeleted = 0 AND isVerified = 1 ORDER BY timestamp DESC")
    fun getVerifiedWantedCriminals(): Flow<List<WantedCriminal>>

    @Query("SELECT * FROM wanted_criminals WHERE isDeleted = 0 AND isVerified = 0 ORDER BY timestamp DESC")
    fun getPendingWantedCriminals(): Flow<List<WantedCriminal>>

    @Query("SELECT * FROM wanted_criminals WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedWantedCriminals(): Flow<List<WantedCriminal>>

    @Query("UPDATE wanted_criminals SET isVerified = :isVerified, status = :status WHERE id = :id")
    suspend fun updateWantedCriminalVerification(id: String, isVerified: Boolean, status: String)

    @Query("UPDATE wanted_criminals SET status = :status WHERE id = :id")
    suspend fun updateWantedCriminalStatus(id: String, status: String)

    @Query("UPDATE wanted_criminals SET name = :name, description = :description, lastSeen = :lastSeen, reward = :reward WHERE id = :id")
    suspend fun updateWantedCriminal(id: String, name: String, description: String, lastSeen: String, reward: String?)

    @Query("UPDATE wanted_criminals SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteWantedCriminal(id: String)

    @Query("UPDATE wanted_criminals SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreWantedCriminal(id: String)

    @Query("DELETE FROM wanted_criminals WHERE id = :id")
    suspend fun permanentlyDeleteWantedCriminal(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWantedCriminal(criminal: WantedCriminal)

    @Query("SELECT * FROM tips WHERE reportId = :reportId ORDER BY timestamp DESC")
    fun getTipsForReport(reportId: String): Flow<List<Tip>>

    @Query("SELECT * FROM tips WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTips(): Flow<List<Tip>>

    @Query("SELECT * FROM tips WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedTips(): Flow<List<Tip>>

    @Query("SELECT * FROM tips WHERE submitterId = :userId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTipsForUser(userId: String): Flow<List<Tip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTip(tip: Tip)

    @Query("UPDATE tips SET isDeleted = 1 WHERE id = :tipId")
    suspend fun softDeleteTip(tipId: String)

    @Query("UPDATE tips SET isDeleted = 0 WHERE id = :tipId")
    suspend fun restoreTip(tipId: String)

    @Query("DELETE FROM tips WHERE id = :tipId")
    suspend fun permanentlyDeleteTip(tipId: String)

    @Query("UPDATE tips SET isReviewed = 1, reviewTimestamp = :timestamp WHERE id = :tipId")
    suspend fun markTipAsReviewed(tipId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String)

    @Query("SELECT * FROM users WHERE role = 'LAW_ENFORCER' AND isApproved = 0")
    fun getPendingLawEnforcers(): Flow<List<User>>

    @Query("UPDATE users SET isApproved = 1 WHERE id = :userId")
    suspend fun approveLawEnforcer(userId: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun rejectLawEnforcer(userId: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String)

    @Query("UPDATE users SET role = :role, isApproved = :isApproved WHERE id = :userId")
    suspend fun updateUserRole(userId: String, role: UserRole, isApproved: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM audit_logs WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedAuditLogs(): Flow<List<AuditLog>>

    @Query("UPDATE audit_logs SET isDeleted = 1 WHERE id = :logId")
    suspend fun softDeleteAuditLog(logId: String)

    @Query("UPDATE audit_logs SET isDeleted = 0 WHERE id = :logId")
    suspend fun restoreAuditLog(logId: String)

    @Query("DELETE FROM audit_logs WHERE id = :logId")
    suspend fun permanentlyDeleteAuditLog(logId: String)

    @Query("SELECT * FROM help_messages ORDER BY timestamp DESC")
    fun getAllHelpMessages(): Flow<List<HelpMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelpMessage(message: HelpMessage)

    @Query("DELETE FROM help_messages WHERE id = :id")
    suspend fun deleteHelpMessage(id: String)

    @Query("UPDATE help_messages SET isRead = :isRead WHERE id = :id")
    suspend fun updateHelpMessageReadStatus(id: String, isRead: Boolean)
}

class Converters {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            // Fallback for old comma-separated data if needed
            value.split(",")
        }
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return try {
            json.encodeToString(list)
        } catch (e: Exception) {
            list.filter { it.isNotBlank() }.joinToString(",")
        }
    }

    @TypeConverter
    fun fromReportType(value: ReportType): String {
        return value.name
    }

    @TypeConverter
    fun toReportType(value: String): ReportType {
        return ReportType.valueOf(value)
    }

    @TypeConverter
    fun fromUserRole(value: UserRole): String {
        return value.name
    }

    @TypeConverter
    fun toUserRole(value: String): UserRole {
        return UserRole.valueOf(value)
    }
}

@Database(entities = [User::class, Report::class, Tip::class, Alert::class, WantedCriminal::class, AuditLog::class, HelpMessage::class], version = 17, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TraceNetDatabase : RoomDatabase() {
    abstract fun dao(): TraceNetDao
}
