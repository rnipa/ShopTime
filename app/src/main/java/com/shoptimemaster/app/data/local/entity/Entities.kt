package com.shoptimemaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val geofenceRadiusMeters: Float = 100f,
    val createdAt: Long
)

@Entity(
    tableName = "time_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("shopId"), Index("checkIn"), Index("status")]
)
data class TimeSessionEntity(
    @PrimaryKey val id: String,
    val checkIn: Long,
    val checkOut: Long?,
    val source: String, // MANUAL or GPS
    val status: String, // ACTIVE, COMPLETED, NEEDS_REVIEW
    val shopId: String,
    val notes: String,
    val confidence: Float
)

@Entity(tableName = "outside_reasons")
data class OutsideReasonEntity(
    @PrimaryKey val id: String,
    val nameBn: String,
    val nameEn: String,
    val categoryType: String, // BUSINESS, PERSONAL, BREAK
    val countsAsBusinessTime: Boolean,
    val isCustom: Boolean = false
)

@Entity(
    tableName = "outside_periods",
    foreignKeys = [
        ForeignKey(
            entity = OutsideReasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["reasonId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("reasonId"), Index("exitTime")]
)
data class OutsidePeriodEntity(
    @PrimaryKey val id: String,
    val sessionId: String?,
    val exitTime: Long,
    val returnTime: Long?,
    val reasonId: String,
    val notes: String,
    val source: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD in Asia/Dhaka
    val targetMinutes: Int,
    val preferredStart: String,
    val preferredEnd: String,
    val isRestDay: Boolean
)

@Entity(tableName = "goal_settings")
data class GoalSettingsEntity(
    @PrimaryKey val id: String = "default",
    val dailyMinutes: Int = 600,
    val weeklyMinutes: Int = 3600,
    val monthlyMinutes: Int = 15600,
    val preferredStart: String = "09:00",
    val preferredEnd: String = "21:00",
    val breakMinutes: Int = 60,
    val weeklyRestDaysCsv: String = "5", // Friday
    val countLunchAsWork: Boolean = false,
    val countRestAsWork: Boolean = false
)

@Entity(tableName = "income_records", indices = [Index(value = ["month"], unique = true)])
data class IncomeRecordEntity(
    @PrimaryKey val id: String,
    val month: String, // YYYY-MM
    val revenue: Double,
    val expenses: Double,
    val costOfGoods: Double,
    val otherCosts: Double,
    val netProfit: Double,
    val isConfirmed: Boolean,
    val notes: String,
    val updatedAt: Long
)

@Entity(tableName = "daily_journals", indices = [Index(value = ["date"], unique = true)])
data class DailyJournalEntity(
    @PrimaryKey val id: String,
    val date: String,
    val completedTasks: String,
    val pendingTasks: String,
    val importantTasks: String,
    val businessProblems: String,
    val personalObservations: String,
    val tomorrowPlan: String,
    val updatedAt: Long
)

@Entity(tableName = "location_events", indices = [Index("timestamp"), Index("status")])
data class LocationEventEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val eventType: String, // ENTER or EXIT
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val source: String,
    val status: String, // PENDING_REVIEW, CONFIRMED, DISMISSED, AUTO_APPLIED
    val distanceMeters: Float
)

@Entity(tableName = "audit_logs", indices = [Index("modifiedAt")])
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val oldValue: String,
    val newValue: String,
    val reason: String,
    val modifiedAt: Long
)

@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey val notificationType: String,
    val enabled: Boolean,
    val scheduledTime: String
)
