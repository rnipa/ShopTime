package com.shoptimemaster.app.domain.engine

import com.shoptimemaster.app.data.local.entity.*
import com.shoptimemaster.app.domain.util.DhakaTimeUtil
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class DaySummary(
    val date: LocalDate,
    val presenceMinutes: Long,
    val businessOutsideMinutes: Long,
    val personalOutsideMinutes: Long,
    val totalBusinessMinutes: Long,
    val targetMinutes: Int,
    val completionPct: Int,
    val goalAchieved: Boolean,
    val entryCount: Int,
    val exitCount: Int,
    val productivityScore: Int?
)

data class HourlyIncomeMetrics(
    val totalRevenue: Double,
    val netProfit: Double,
    val physicalHours: Double,
    val businessHours: Double,
    val revenuePerPhysicalHour: Double,
    val profitPerPhysicalHour: Double,
    val revenuePerBusinessHour: Double,
    val profitPerBusinessHour: Double,
    val isConfirmed: Boolean
)

data class LeakageInsight(
    val titleBn: String,
    val titleEn: String,
    val detailBn: String,
    val detailEn: String,
    val recommendationBn: String,
    val recommendationEn: String
)

object AnalyticsEngine {
    fun computeHourlyIncome(
        income: IncomeRecordEntity?,
        physicalMinutes: Long,
        businessMinutes: Long
    ): HourlyIncomeMetrics {
        val physHrs = physicalMinutes / 60.0
        val busHrs = businessMinutes / 60.0
        val rev = income?.revenue ?: 0.0
        val net = income?.netProfit ?: 0.0
        return HourlyIncomeMetrics(
            totalRevenue = rev,
            netProfit = net,
            physicalHours = physHrs,
            businessHours = busHrs,
            revenuePerPhysicalHour = if (physHrs > 0.0) rev / physHrs else 0.0,
            profitPerPhysicalHour = if (physHrs > 0.0) net / physHrs else 0.0,
            revenuePerBusinessHour = if (busHrs > 0.0) rev / busHrs else 0.0,
            profitPerBusinessHour = if (busHrs > 0.0) net / busHrs else 0.0,
            isConfirmed = income?.isConfirmed ?: false
        )
    }

    fun calculatePresenceMinutesForDate(
        date: LocalDate,
        sessions: List<TimeSessionEntity>,
        nowMs: Long
    ): Long {
        var total = 0L
        for (s in sessions) {
            if (s.status == "NEEDS_REVIEW" && s.checkOut == null) continue
            val endMs = s.checkOut ?: nowMs
            val split = DhakaTimeUtil.splitIntervalByDhakaDate(s.checkIn, endMs)
            total += split[date] ?: 0L
        }
        return max(0L, total)
    }

    fun calculateOutsideSplitForDate(
        date: LocalDate,
        periods: List<OutsidePeriodEntity>,
        reasons: Map<String, OutsideReasonEntity>,
        settings: GoalSettingsEntity
    ): Pair<Long, Long> {
        var businessOut = 0L
        var personalOut = 0L
        for (p in periods) {
            val endMs = p.returnTime ?: continue
            val split = DhakaTimeUtil.splitIntervalByDhakaDate(p.exitTime, endMs)
            val mins = split[date] ?: 0L
            if (mins <= 0) continue
            val r = reasons[p.reasonId]
            val countsAsBus = when (p.reasonId) {
                "lunch" -> settings.countLunchAsWork
                "rest" -> settings.countRestAsWork
                else -> r?.categoryType == "BUSINESS" || r?.countsAsBusinessTime == true
            }
            if (countsAsBus) businessOut += mins else personalOut += mins
        }
        return businessOut to personalOut
    }

    fun summarizeDay(
        date: LocalDate,
        sessions: List<TimeSessionEntity>,
        periods: List<OutsidePeriodEntity>,
        reasons: Map<String, OutsideReasonEntity>,
        dailyGoals: Map<String, DailyGoalEntity>,
        settings: GoalSettingsEntity,
        nowMs: Long
    ): DaySummary {
        val presence = calculatePresenceMinutesForDate(date, sessions, nowMs)
        val (busOut, perOut) = calculateOutsideSplitForDate(date, periods, reasons, settings)
        val totalBus = presence + busOut
        val goalSnap = dailyGoals[date.toString()]
        val target = (goalSnap?.targetMinutes ?: settings.dailyMinutes).coerceAtLeast(1)
        val pct = min(100, ((totalBus.toDouble() / target) * 100.0).roundToInt())

        val daySessions = sessions.filter { DhakaTimeUtil.localDateFromEpoch(it.checkIn) == date }
        val entries = daySessions.size
        val exits = daySessions.count { it.checkOut != null }

        val score = if (entries > 0 || presence > 0) {
            val targetScore = min(60.0, (totalBus.toDouble() / target) * 60.0)
            val tracked = (totalBus + perOut).toDouble()
            val leakRatio = if (tracked > 0) perOut / tracked else 0.0
            val discipline = max(0.0, 40.0 * (1.0 - leakRatio * 1.4))
            min(100, max(0, (targetScore + discipline).roundToInt()))
        } else null

        return DaySummary(
            date = date,
            presenceMinutes = presence,
            businessOutsideMinutes = busOut,
            personalOutsideMinutes = perOut,
            totalBusinessMinutes = totalBus,
            targetMinutes = target,
            completionPct = pct,
            goalAchieved = totalBus >= target,
            entryCount = entries,
            exitCount = exits,
            productivityScore = score
        )
    }

    fun analyzeLeakageLast7Days(summaries: List<DaySummary>): List<LeakageInsight> {
        val active = summaries.filter { it.entryCount > 0 || it.presenceMinutes > 0 }
        if (active.size < 2) return emptyList()

        val insights = mutableListOf<LeakageInsight>()
        val personalTotal = active.sumOf { it.personalOutsideMinutes }
        if (personalTotal >= 120) {
            val hrs = personalTotal / 60
            val mins = personalTotal % 60
            insights.add(
                LeakageInsight(
                    titleBn = "ব্যক্তিগত কাজে অতিরিক্ত বাইরে সময়",
                    titleEn = "High Personal Time Outside Shop",
                    detailBn = "গত ৭ দিনে ব্যক্তিগত কারণে দোকানের বাইরে ${hrs} ঘণ্টা ${mins} মিনিট ব্যয় হয়েছে।",
                    detailEn = "You spent ${hrs}h ${mins}m outside for personal reasons in the last 7 days.",
                    recommendationBn = "দুপুরের বিরতি ও ব্যক্তিগত কাজের জন্য প্রতিদিন ৬০ মিনিট সীমা নির্ধারণ করুন।",
                    recommendationEn = "Set a 60-minute daily cap for lunch and personal errands."
                )
            )
        }
        val avgExits = active.sumOf { it.exitCount }.toDouble() / active.size
        if (avgExits >= 3.5) {
            insights.add(
                LeakageInsight(
                    titleBn = "ঘন ঘন দোকান থেকে বের হওয়া",
                    titleEn = "Frequent Daily Exits",
                    detailBn = "প্রতিদিন গড়ে %.1f বার দোকান থেকে বের হচ্ছেন।".format(avgExits),
                    detailEn = "You average %.1f exits per active day.".format(avgExits),
                    recommendationBn = "ব্যাংক ও ছোট কাজগুলো দিনের একটি নির্দিষ্ট সময়ে একসাথে করুন।",
                    recommendationEn = "Batch banking and errands into a single daily window."
                )
            )
        }
        return insights
    }
}
