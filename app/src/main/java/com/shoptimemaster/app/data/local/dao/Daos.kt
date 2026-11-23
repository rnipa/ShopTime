package com.shoptimemaster.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shoptimemaster.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops LIMIT 1")
    fun observePrimaryShop(): Flow<ShopEntity?>

    @Query("SELECT * FROM shops LIMIT 1")
    suspend fun getPrimaryShop(): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shop: ShopEntity)

    @Query("DELETE FROM shops")
    suspend fun deleteAll()
}

@Dao
interface TimeSessionDao {
    @Query("SELECT * FROM time_sessions ORDER BY checkIn DESC")
    fun observeAll(): Flow<List<TimeSessionEntity>>

    @Query("SELECT * FROM time_sessions WHERE status = 'ACTIVE' AND checkOut IS NULL LIMIT 1")
    suspend fun getActiveSession(): TimeSessionEntity?

    @Query("SELECT * FROM time_sessions WHERE status = 'NEEDS_REVIEW' AND checkOut IS NULL")
    suspend fun getRecoverySessions(): List<TimeSessionEntity>

    @Query("SELECT COUNT(*) FROM time_sessions WHERE id != :ignoreId AND checkIn < :endMs AND (checkOut IS NULL OR checkOut > :startMs)")
    suspend fun countOverlapping(startMs: Long, endMs: Long, ignoreId: String = ""): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TimeSessionEntity)

    @Update
    suspend fun update(session: TimeSessionEntity)

    @Query("DELETE FROM time_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM time_sessions")
    suspend fun deleteAll()

    @Transaction
    suspend fun checkInSafely(session: TimeSessionEntity): Boolean {
        if (getActiveSession() != null) return false
        if (countOverlapping(session.checkIn, Long.MAX_VALUE) > 0) return false
        insert(session)
        return true
    }
}

@Dao
interface OutsideDao {
    @Query("SELECT * FROM outside_periods ORDER BY exitTime DESC")
    fun observeAllPeriods(): Flow<List<OutsidePeriodEntity>>

    @Query("SELECT * FROM outside_reasons")
    fun observeReasons(): Flow<List<OutsideReasonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeriod(period: OutsidePeriodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReason(reason: OutsideReasonEntity)

    @Query("UPDATE outside_periods SET returnTime = :returnMs WHERE returnTime IS NULL")
    suspend fun closeOpenOutsidePeriods(returnMs: Long)

    @Query("DELETE FROM outside_periods WHERE id = :id")
    suspend fun deletePeriod(id: String)

    @Query("DELETE FROM outside_periods")
    suspend fun deleteAllPeriods()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal_settings LIMIT 1")
    fun observeSettings(): Flow<GoalSettingsEntity?>

    @Query("SELECT * FROM daily_goals")
    fun observeDailyGoals(): Flow<List<DailyGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: GoalSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailySnapshot(goal: DailyGoalEntity)

    @Query("DELETE FROM daily_goals")
    suspend fun deleteAllDailyGoals()
}

@Dao
interface BusinessDataDao {
    @Query("SELECT * FROM income_records ORDER BY month DESC")
    fun observeIncomeRecords(): Flow<List<IncomeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncome(record: IncomeRecordEntity)

    @Query("DELETE FROM income_records")
    suspend fun deleteAllIncome()

    @Query("SELECT * FROM daily_journals ORDER BY date DESC")
    fun observeJournals(): Flow<List<DailyJournalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournal(journal: DailyJournalEntity)

    @Query("DELETE FROM daily_journals")
    suspend fun deleteAllJournals()

    @Query("SELECT * FROM location_events ORDER BY timestamp DESC")
    fun observeLocationEvents(): Flow<List<LocationEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocationEvent(event: LocationEventEntity)

    @Query("DELETE FROM location_events")
    suspend fun deleteAllLocationEvents()

    @Query("SELECT * FROM audit_logs ORDER BY modifiedAt DESC")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAllAuditLogs()

    @Query("SELECT * FROM notification_settings")
    fun observeNotificationSettings(): Flow<List<NotificationSettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotificationSetting(setting: NotificationSettingsEntity)
}
