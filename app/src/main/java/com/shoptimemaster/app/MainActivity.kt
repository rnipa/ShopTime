package com.shoptimemaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shoptimemaster.app.ui.navigation.Screen
import com.shoptimemaster.app.ui.screens.*
import com.shoptimemaster.app.ui.theme.ShopTimeMasterTheme
import com.shoptimemaster.app.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            var showExitSheet by remember { mutableStateOf(false) }
            var showManualDialog by remember { mutableStateOf(false) }
            val isBn = state.prefs.language == "bn"

            ShopTimeMasterTheme(darkTheme = state.prefs.darkTheme) {
                if (!state.prefs.onboardingDone) {
                    OnboardingSetupScreen(
                        state = state,
                        onToggleLanguage = { viewModel.toggleLanguage(it) },
                        onSaveShop = { name, addr, lat, lng, rad, gps ->
                            viewModel.saveShop(name, addr, lat, lng, rad, gps)
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val backStack by navController.currentBackStackEntryAsState()
                    val currentRoute = backStack?.destination?.route ?: Screen.Home.route

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Home.route,
                                    onClick = { navController.navigate(Screen.Home.route) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(if (isBn) "হোম" else "Home") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Tracker.route,
                                    onClick = { navController.navigate(Screen.Tracker.route) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                    label = { Text(if (isBn) "ট্র্যাকার" else "Tracker") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.History.route,
                                    onClick = { navController.navigate(Screen.History.route) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                                    label = { Text(if (isBn) "ইতিহাস" else "History") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Analytics.route,
                                    onClick = { navController.navigate(Screen.Analytics.route) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                    label = { Text(if (isBn) "বিশ্লেষণ" else "Analytics") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Settings.route,
                                    onClick = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text(if (isBn) "সেটিংস" else "Settings") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) {
                                DashboardScreen(
                                    state = state,
                                    onCheckIn = { viewModel.checkIn("MANUAL") },
                                    onOpenExitSheet = { showExitSheet = true },
                                    onOpenManualDialog = { showManualDialog = true },
                                    onNavigateTracker = { navController.navigate(Screen.Tracker.route) }
                                )
                            }
                            composable(Screen.Tracker.route) {
                                TrackerScreen(
                                    state = state,
                                    onCheckIn = { viewModel.checkIn("MANUAL") },
                                    onOpenExitSheet = { showExitSheet = true },
                                    onTogglePauseGps = { viewModel.togglePauseGps() },
                                    onConfirmEvent = { viewModel.confirmLocationEvent(it) },
                                    onDismissEvent = { viewModel.dismissLocationEvent(it) },
                                    onResolveRecovery = { rs, outMs -> viewModel.resolveRecoverySession(rs, outMs) }
                                )
                            }
                            composable(Screen.History.route) {
                                HistoryAndJournalScreen(
                                    state = state,
                                    onDeleteSession = { viewModel.deleteSession(it) },
                                    onSaveJournal = { d, c, p, pr, t -> viewModel.saveJournal(d, c, p, pr, t) }
                                )
                            }
                            composable(Screen.Analytics.route) {
                                AnalyticsScreen(
                                    state = state,
                                    onSaveIncome = { m, r, cg, ex, ot, cf -> viewModel.saveIncome(m, r, cg, ex, ot, cf) },
                                    onSaveGoals = { dy, wk, mo, s, e, ln, rs -> viewModel.saveGoals(dy, wk, mo, s, e, ln, rs) }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    state = state,
                                    onToggleLanguage = { viewModel.toggleLanguage(it) },
                                    onToggleNumerals = { viewModel.toggleBengaliNumerals(it) },
                                    onToggleTheme = { viewModel.toggleTheme(it) },
                                    onWipeAll = { viewModel.wipeAllData() }
                                )
                            }
                        }
                    }

                    if (showExitSheet) {
                        ExitReasonSheet(
                            state = state,
                            onDismiss = { showExitSheet = false },
                            onConfirm = { reasonId, note ->
                                viewModel.checkOut(reasonId, note)
                                showExitSheet = false
                            },
                            onAddCustomReason = { name, isBus -> viewModel.addCustomReason(name, isBus) }
                        )
                    }

                    if (showManualDialog) {
                        ManualSessionDialog(
                            isBn = isBn,
                            onDismiss = { showManualDialog = false },
                            onSave = { startMs, endMs, note -> viewModel.addManualSession(startMs, endMs, note) }
                        )
                    }
                }
            }
        }
    }
}
