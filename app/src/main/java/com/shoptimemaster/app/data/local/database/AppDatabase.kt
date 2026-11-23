package com.shoptimemaster.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shoptimemaster.app.data.local.dao.*
import com.shoptimemaster.app.data.local.entity.*

@Database(
    entities = [
        ShopEntity::class,
        TimeSessionEntity::class,
        OutsidePeriodEntity::class,
        OutsideReasonEntity::class,
        DailyGoalEntity::class,
        GoalSettingsEntity::class,
        IncomeRecordEntity::class,
        DailyJournalEntity::class,
        LocationEventEntity::class,
        AuditLogEntity::class,
        NotificationSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao
    abstract fun timeSessionDao(): TimeSessionDao
    abstract fun outsideDao(): OutsideDao
    abstract fun goalDao(): GoalDao
    abstract fun businessDataDao(): BusinessDataDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE income_records ADD COLUMN isConfirmed INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "shop_time_master.db"
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val defaults = listOf(
                            Triple("lunch", "খাবার", "Lunch") to Pair("BREAK", 0),
                            Triple("home", "বাসার কাজ", "Home") to Pair("PERSONAL", 0),
                            Triple("personal", "ব্যক্তিগত কাজ", "Personal") to Pair("PERSONAL", 0),
                            Triple("business", "ব্যবসায়িক কাজ", "Business") to Pair("BUSINESS", 1),
                            Triple("stock", "মালামাল সংগ্রহ", "Stock Collection") to Pair("BUSINESS", 1),
                            Triple("banking", "ব্যাংক", "Banking") to Pair("BUSINESS", 1),
                            Triple("rest", "বিশ্রাম", "Rest") to Pair("BREAK", 0),
                            Triple("social", "বন্ধু / আড্ডা", "Social") to Pair("PERSONAL", 0),
                            Triple("emergency", "জরুরি কাজ", "Emergency") to Pair("PERSONAL", 0),
                            Triple("other", "অন্যান্য", "Other") to Pair("PERSONAL", 0)
                        )
                        defaults.forEach { (names, meta) ->
                            db.execSQL(
                                "INSERT OR IGNORE INTO outside_reasons (id, nameBn, nameEn, categoryType, countsAsBusinessTime, isCustom) VALUES (?, ?, ?, ?, ?, 0)",
                                arrayOf(names.first, names.second, names.third, meta.first, meta.second)
                            )
                        }
                        db.execSQL("INSERT OR IGNORE INTO goal_settings (id) VALUES ('default')")
                    }
                })
                .build()
        }
    }
}
