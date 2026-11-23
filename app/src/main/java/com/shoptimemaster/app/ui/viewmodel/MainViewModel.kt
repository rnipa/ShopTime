package com.shoptimemaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoptimemaster.app.data.local.dao.*
import com.shoptimemaster.app.data.local.entity.*
import com.shoptimemaster.app.data.local.preferences.UserPreferencesDataStore
import com.shoptimemaster.app.data.local.preferences.UserPrefs
import com.shoptimemaster.app.domain.engine.*
import com.shoptimemaster.app.domain.util.DhakaTimeUtil
import com.shoptimemaster.app.location.GeofenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class DashboardUiState(
    val nowMs: Long = System.currentTimeMillis(),
    val shop: ShopEntity? = null,
    val activeSession: TimeSessionEntity? = null,
    val activeOutside: OutsidePeriodEntity? = null,
    val todaySummary: DaySummary? = null,
    val last7Days: List<DaySummary> = emptyList(),
    val monthPresenceMinutes: Long = 0L,
    val monthBusinessMinutes: Long = 0L,
    val currentStreak: Int = 0,
    val goalSettings: GoalSettingsEntity = GoalSettingsEntity(),
    val incomeRecords: List<IncomeRecordEntity> = emptyList(),
    val monthIncomeMetrics: HourlyIncomeMetrics? = null,
    val leakageInsights: List<LeakageInsight> = emptyList(),
    val prefs: UserPrefs = UserPrefs(),
    val reasons: List<OutsideReasonEntity> = emptyList(),
    val sessions: List<TimeSessionEntity> = emptyList(),
    val outsidePeriods: List<OutsidePeriodEntity> = emptyList(),
    val pendingEvents: List<LocationEventEntity> = emptyList(),
    val recoverySessions: List<TimeSessionEntity> = emptyList(),
    val journals: List<DailyJournalEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val shopDao: ShopDao,
    private val sessionDao: TimeSessionDao,
    private val outsideDao: OutsideDao,
    private val goalDao: GoalDao,
    private val businessDao: BusinessDataDao,
    private val prefsStore: UserPreferencesDataStore,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    private data class DbBundle(
        val shop: ShopEntity?,
        val sessions: List<TimeSessionEntity>,
        val outsides: List<OutsidePeriodEntity>,
        val reasons: List<OutsideReasonEntity>,
        val goalSettings: GoalSettingsEntity,
        val dailyGoals: List<DailyGoalEntity>,
        val incomes: List<IncomeRecordEntity>,
        val locEvents: List<LocationEventEntity>,
        val journals: List<DailyJournalEntity>,
        val auditLogs: List<AuditLogEntity>,
        val prefs: UserPrefs
    )

    private val dbFlow = combine(
        shopDao.observePrimaryShop(),
        sessionDao.observeAll(),
        outsideDao.observeAllPeriods(),
        outsideDao.observeReasons(),
        goalDao.observeSettings()
    ) { shop, sessions, outsides, reasons, settings ->
        arrayOf(shop, sessions, outsides, reasons, settings ?: GoalSettingsEntity())
    }.combine(
        combine(
            goalDao.observeDailyGoals(),
            businessDao.observeIncomeRecords(),
            businessDao.observeLocationEvents(),
            businessDao.observeJournals(),
            businessDao.observeAuditLogs()
        ) { dailyGoals, incomes, events, journals, audits ->
            arrayOf(dailyGoals, incomes, events, journals, audits)
        }
    ) { a, b ->
        Pair(a, b)
    }.combine(prefsStore.prefsFlow) { (a, b), prefs ->
        @Suppress("UNCHECKED_CAST")
        DbBundle(
            shop = a[0] as ShopEntity?,
            sessions = a[1] as List<TimeSessionEntity>,
            outsides = a[2] as List<OutsidePeriodEntity>,
            reasons = a[3] as List<OutsideReasonEntity>,
            goalSettings = a[4] as GoalSettingsEntity,
            dailyGoals = b[0] as List<DailyGoalEntity>,
            incomes = b[1] as List<IncomeRecordEntity>,
            locEvents = b[2] as List<LocationEventEntity>,
            journals = b[3] as List<DailyJournalEntity>,
            auditLogs = b[4] as List<AuditLogEntity>,
            prefs = prefs
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(dbFlow, tickerFlow) { bundle, nowMs ->
        val today = DhakaTimeUtil.localDateFromEpoch(nowMs)
        val monthPrefix = "${today.year}-${today.monthValue.toString().padStart(2, '0')}"
        val reasonMap = bundle.reasons.associateBy { it.id }
        val dailyGoalMap = bundle.dailyGoals.associateBy { it.date }

        val last7 = (6 downTo 0).map { offset ->
            AnalyticsEngine.summarizeDay(
                date = today.minusDays(offset.toLong()),
                sessions = bundle.sessions,
                periods = bundle.outsides,
                reasons = reasonMap,
                dailyGoals = dailyGoalMap,
                settings = bundle.goalSettings,
                nowMs = nowMs
            )
        }
        val todaySummary = last7.last()

        var monthPhys = 0L
        var monthBus = 0L
        val daysInMonth = today.lengthOfMonth()
        for (d in 1..daysInMonth) {
            val dt = today.withDayOfMonth(d)
            if (dt.isAfter(today)) break
            val sum = AnalyticsEngine.summarizeDay(
                date = dt,
                sessions = bundle.sessions,
                periods = bundle.outsides,
                reasons = reasonMap,
                dailyGoals = dailyGoalMap,
                settings = bundle.goalSettings,
                nowMs = nowMs
            )
            monthPhys += sum.presenceMinutes
            monthBus += sum.totalBusinessMinutes
        }

        val monthIncome = bundle.incomes.firstOrNull { it.month == monthPrefix }
        val incomeMetrics = AnalyticsEngine.computeHourlyIncome(monthIncome, monthPhys, monthBus)

        var streak = 0
        var cursor = if (todaySummary.goalAchieved) today else today.minusDays(1)
        for (i in 0 until 60) {
            val s = AnalyticsEngine.summarizeDay(
                date = cursor,
                sessions = bundle.sessions,
                periods = bundle.outsides,
                reasons = reasonMap,
                dailyGoals = dailyGoalMap,
                settings = bundle.goalSettings,
                nowMs = nowMs
            )
            if (s.goalAchieved) {
                streak++
                cursor = cursor.minusDays(1)
            } else break
        }

        val activeSess = bundle.sessions.firstOrNull { it.status == "ACTIVE" && it.checkOut == null }
        val activeOut = if (activeSess == null) {
            bundle.outsides.firstOrNull {
                it.returnTime == null && DhakaTimeUtil.localDateFromEpoch(it.exitTime) == today
            }
        } else null

        DashboardUiState(
            nowMs = nowMs,
            shop = bundle.shop,
            activeSession = activeSess,
            activeOutside = activeOut,
            todaySummary = todaySummary,
            last7Days = last7,
            monthPresenceMinutes = monthPhys,
            monthBusinessMinutes = monthBus,
            currentStreak = streak,
            goalSettings = bundle.goalSettings,
            incomeRecords = bundle.incomes,
            monthIncomeMetrics = incomeMetrics,
            leakageInsights = AnalyticsEngine.analyzeLeakageLast7Days(last7),
            prefs = bundle.prefs,
            reasons = bundle.reasons,
            sessions = bundle.sessions,
            outsidePeriods = bundle.outsides,
            pendingEvents = bundle.locEvents.filter { it.status == "PENDING_REVIEW" },
            recoverySessions = bundle.sessions.filter { it.status == "NEEDS_REVIEW" && it.checkOut == null },
            journals = bundle.journals,
            auditLogs = bundle.auditLogs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun checkIn(source: String = "MANUAL") {
        viewModelScope.launch {
            var shop = shopDao.getPrimaryShop()
            if (shop == null) {
                shop = ShopEntity("default_shop", "My Shop", "", null, null, 100f, System.currentTimeMillis())
                shopDao.upsert(shop)
            }
            val now = System.currentTimeMillis()
            outsideDao.closeOpenOutsidePeriods(now)
            val inserted = sessionDao.checkInSafely(
                TimeSessionEntity(
                    id = "sess_${UUID.randomUUID()}",
                    checkIn = now,
                    checkOut = null,
                    source = source,
                    status = "ACTIVE",
                    shopId = shop.id,
                    notes = "",
                    confidence = if (source == "MANUAL") 1.0f else 0.85f
                )
            )
            if (inserted) {
                val today = DhakaTimeUtil.localDateFromEpoch(now)
                val gs = uiState.value.goalSettings
                goalDao.insertDailySnapshot(
                    DailyGoalEntity(today.toString(), gs.dailyMinutes, gs.preferredStart, gs.preferredEnd, false)
                )
            }
        }
    }

    fun checkOut(reasonId: String, notes: String = "", source: String = "MANUAL") {
        viewModelScope.launch {
            val active = sessionDao.getActiveSession() ?: return@launch
            val now = System.currentTimeMillis()
            sessionDao.update(active.copy(checkOut = now, status = "COMPLETED", notes = notes))
            outsideDao.upsertPeriod(
                OutsidePeriodEntity(
                    id = "out_${UUID.randomUUID()}",
                    sessionId = active.id,
                    exitTime = now,
                    returnTime = null,
                    reasonId = reasonId,
                    notes = notes,
                    source = source
                )
            )
        }
    }

    fun addCustomReason(name: String, countsAsBusiness: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            outsideDao.upsertReason(
                OutsideReasonEntity(
                    id = "reason_${UUID.randomUUID()}",
                    nameBn = name.trim(),
                    nameEn = name.trim(),
                    categoryType = if (countsAsBusiness) "BUSINESS" else "PERSONAL",
                    countsAsBusinessTime = countsAsBusiness,
                    isCustom = true
                )
            )
        }
    }

    fun addManualSession(checkInMs: Long, checkOutMs: Long, notes: String) {
        viewModelScope.launch {
            if (checkOutMs <= checkInMs) return@launch
            if (sessionDao.countOverlapping(checkInMs, checkOutMs) > 0) return@launch
            var shop = shopDao.getPrimaryShop()
            if (shop == null) {
                shop = ShopEntity("default_shop", "My Shop", "", null, null, 100f, System.currentTimeMillis())
                shopDao.upsert(shop)
            }
            val id = "sess_${UUID.randomUUID()}"
            sessionDao.insert(
                TimeSessionEntity(id, checkInMs, checkOutMs, "MANUAL", "COMPLETED", shop.id, notes, 1.0f)
            )
            businessDao.insertAuditLog(
                AuditLogEntity("audit_${UUID.randomUUID()}", "SESSION", id, "NONE", "$checkInMs->$checkOutMs", "Manual entry", System.currentTimeMillis())
            )
        }
    }

    fun deleteSession(session: TimeSessionEntity) {
        viewModelScope.launch {
            sessionDao.deleteById(session.id)
            businessDao.insertAuditLog(
                AuditLogEntity("audit_${UUID.randomUUID()}", "SESSION", session.id, "${session.checkIn}->${session.checkOut}", "DELETED", "Deleted session", System.currentTimeMillis())
            )
        }
    }

    fun resolveRecoverySession(session: TimeSessionEntity, checkOutMs: Long) {
        viewModelScope.launch {
            if (checkOutMs <= session.checkIn) return@launch
            sessionDao.update(session.copy(checkOut = checkOutMs, status = "COMPLETED"))
        }
    }

    fun confirmLocationEvent(ev: LocationEventEntity) {
        viewModelScope.launch {
            if (ev.eventType == "ENTER") checkIn("GPS") else checkOut("other", "GPS exit", "GPS")
            businessDao.upsertLocationEvent(ev.copy(status = "CONFIRMED"))
        }
    }

    fun dismissLocationEvent(ev: LocationEventEntity) {
        viewModelScope.launch {
            businessDao.upsertLocationEvent(ev.copy(status = "DISMISSED"))
        }
    }

    fun saveShop(name: String, address: String, lat: Double?, lng: Double?, radius: Float, enableGps: Boolean) {
        viewModelScope.launch {
            val entity = ShopEntity(
                id = uiState.value.shop?.id ?: "shop_${UUID.randomUUID()}",
                name = name.ifBlank { "My Shop" },
                address = address,
                latitude = lat,
                longitude = lng,
                geofenceRadiusMeters = radius,
                createdAt = uiState.value.shop?.createdAt ?: System.currentTimeMillis()
            )
            shopDao.upsert(entity)
            prefsStore.setOnboardingDone(true)
            prefsStore.setLocationPromptSeen(true)
            prefsStore.setGpsEnabled(enableGps)
            if (enableGps && lat != null && lng != null) {
                geofenceManager.registerShopGeofence(entity.id, lat, lng, radius)
            } else {
                geofenceManager.unregisterAll()
            }
        }
    }

    fun saveIncome(month: String, revenue: Double, cogs: Double, expenses: Double, other: Double, confirmed: Boolean) {
        viewModelScope.launch {
            val net = revenue - (cogs + expenses + other)
            businessDao.upsertIncome(
                IncomeRecordEntity(month, month, revenue, expenses, cogs, other, net, confirmed, "", System.currentTimeMillis())
            )
        }
    }

    fun saveGoals(dailyMin: Int, weeklyMin: Int, monthlyMin: Int, start: String, end: String, countLunch: Boolean, countRest: Boolean) {
        viewModelScope.launch {
            goalDao.upsertSettings(
                uiState.value.goalSettings.copy(
                    dailyMinutes = dailyMin,
                    weeklyMinutes = weeklyMin,
                    monthlyMinutes = monthlyMin,
                    preferredStart = start,
                    preferredEnd = end,
                    countLunchAsWork = countLunch,
                    countRestAsWork = countRest
                )
            )
        }
    }

    fun saveJournal(date: String, completed: String, pending: String, problems: String, tomorrow: String) {
        viewModelScope.launch {
            businessDao.upsertJournal(
                DailyJournalEntity(date, date, completed, pending, "", problems, "", tomorrow, System.currentTimeMillis())
            )
        }
    }

    fun toggleTheme(dark: Boolean) = viewModelScope.launch { prefsStore.setDarkTheme(dark) }
    fun toggleLanguage(lang: String) = viewModelScope.launch { prefsStore.setLanguage(lang) }
    fun toggleBengaliNumerals(bn: Boolean) = viewModelScope.launch { prefsStore.setBengaliNumerals(bn) }
    fun togglePauseGps() = viewModelScope.launch { prefsStore.setGpsPaused(!uiState.value.prefs.gpsPaused) }

    fun wipeAllData() {
        viewModelScope.launch {
            sessionDao.deleteAll()
            outsideDao.deleteAllPeriods()
            goalDao.deleteAllDailyGoals()
            businessDao.deleteAllIncome()
            businessDao.deleteAllJournals()
            businessDao.deleteAllLocationEvents()
            businessDao.deleteAllAuditLogs()
        }
    }
}
