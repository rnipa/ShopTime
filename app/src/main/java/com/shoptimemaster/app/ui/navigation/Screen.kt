package com.shoptimemaster.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object ShopSetup : Screen("shop_setup")
    data object LocationPermission : Screen("location_permission")
    data object Home : Screen("home")
    data object Tracker : Screen("tracker")
    data object History : Screen("history")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
    data object IncomeManager : Screen("income_manager")
    data object GoalManager : Screen("goal_manager")
    data object SmartCoach : Screen("smart_coach")
    data object DailyJournal : Screen("daily_journal")
    data object DataExport : Screen("data_export")
}
