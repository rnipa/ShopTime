package com.shoptimemaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoptimemaster.app.data.local.entity.LocationEventEntity
import com.shoptimemaster.app.data.local.entity.TimeSessionEntity
import com.shoptimemaster.app.domain.util.DhakaTimeUtil
import com.shoptimemaster.app.ui.viewmodel.DashboardUiState

private fun fmtDigits(text: String, bn: Boolean): String =
    if (bn) DhakaTimeUtil.toBengaliDigits(text) else text

private fun fmtDuration(minutes: Long, isBn: Boolean, bnDigits: Boolean): String {
    val h = minutes / 60
    val m = minutes % 60
    val raw = if (isBn) "${h} ঘ. ${m} মি." else "${h}h ${m}m"
    return fmtDigits(raw, bnDigits)
}

private fun fmtElapsed(ms: Long, bnDigits: Boolean): String {
    val sec = (ms.coerceAtLeast(0L)) / 1000L
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    val raw = "%02d:%02d:%02d".format(h, m, s)
    return fmtDigits(raw, bnDigits)
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onCheckIn: () -> Unit,
    onOpenExitSheet: () -> Unit,
    onOpenManualDialog: () -> Unit,
    onNavigateTracker: () -> Unit
) {
    val isBn = state.prefs.language == "bn"
    val bnDigits = state.prefs.useBengaliNumerals
    val today = state.todaySummary
    val presenceMin = today?.presenceMinutes ?: 0L
    val busOutMin = today?.businessOutsideMinutes ?: 0L
    val perOutMin = today?.personalOutsideMinutes ?: 0L
    val targetMin = (today?.targetMinutes ?: state.goalSettings.dailyMinutes).coerceAtLeast(1)
    val totalBusMin = presenceMin + busOutMin
    val pct = ((totalBusMin.toFloat() / targetMin) * 100f).coerceIn(0f, 100f).toInt()

    val elapsedMs = when {
        state.activeSession != null -> state.nowMs - state.activeSession.checkIn
        state.activeOutside != null -> state.nowMs - state.activeOutside.exitTime
        else -> 0L
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = state.shop?.name ?: "SHOP TIME MASTER",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            val statusStr = when {
                state.activeSession != null -> if (isBn) "দোকানে আছেন · সক্রিয়" else "INSIDE SHOP · Active"
                state.activeOutside != null -> if (isBn) "বাইরে আছেন" else "OUTSIDE SHOP"
                else -> if (isBn) "ট্র্যাকিং বন্ধ" else "NOT TRACKING"
            }
            Text(statusStr, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.pendingEvents.isNotEmpty() || state.recoverySessions.isNotEmpty()) {
            item {
                OutlinedCard(onClick = onNavigateTracker, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = if (isBn) "জিপিএস / সেশন রিকভারি অপেক্ষমাণ" else "Pending GPS / Recovery Events",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (isBn) "নিশ্চিত করতে ট্র্যাকার ট্যাবে যান" else "Tap to review in Tracker",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(if (isBn) "আজকের দোকানে উপস্থিতি" else "Today's Shop Presence", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = fmtDuration(presenceMin, isBn, bnDigits),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(if (isBn) "চলমান সময়" else "Live Timer", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = fmtElapsed(elapsedMs, bnDigits),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${if (isBn) "দৈনিক লক্ষ্য:" else "Daily Goal:"} ${fmtDigits("$pct%", bnDigits)} (${fmtDuration(totalBusMin, isBn, bnDigits)} / ${fmtDuration(targetMin.toLong(), isBn, bnDigits)})",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (state.activeSession == null) {
                            Button(onClick = onCheckIn, modifier = Modifier.weight(1f).height(52.dp)) {
                                Text(if (isBn) "দোকানে প্রবেশ" else "SHOP CHECK IN")
                            }
                        } else {
                            Button(
                                onClick = onOpenExitSheet,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).height(52.dp)
                            ) {
                                Text(if (isBn) "দোকান থেকে বের হন" else "SHOP CHECK OUT")
                            }
                        }
                        OutlinedButton(onClick = onOpenManualDialog, modifier = Modifier.height(52.dp)) {
                            Text(if (isBn) "ম্যানুয়াল" else "Manual")
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (isBn) "ব্যবসায়িক বাইরে" else "Business Outside", style = MaterialTheme.typography.labelSmall)
                        Text(fmtDuration(busOutMin, isBn, bnDigits), fontWeight = FontWeight.Bold)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (isBn) "ব্যক্তিগত বাইরে" else "Personal Outside", style = MaterialTheme.typography.labelSmall)
                        Text(fmtDuration(perOutMin, isBn, bnDigits), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (isBn) "মাসিক ঘণ্টাপ্রতি লাভ" else "Monthly Profit/Hr", style = MaterialTheme.typography.labelSmall)
                        val pHr = state.monthIncomeMetrics?.profitPerBusinessHour ?: 0.0
                        Text(DhakaTimeUtil.formatBdt(pHr, bnDigits), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (isBn) "টানা লক্ষ্য অর্জন" else "Current Streak", style = MaterialTheme.typography.labelSmall)
                        Text(
                            fmtDigits("${state.currentStreak} ${if (isBn) "দিন" else "days"}", bnDigits),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackerScreen(
    state: DashboardUiState,
    onCheckIn: () -> Unit,
    onOpenExitSheet: () -> Unit,
    onTogglePauseGps: () -> Unit,
    onConfirmEvent: (LocationEventEntity) -> Unit,
    onDismissEvent: (LocationEventEntity) -> Unit,
    onResolveRecovery: (TimeSessionEntity, Long) -> Unit
) {
    val isBn = state.prefs.language == "bn"
    val bnDigits = state.prefs.useBengaliNumerals

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (isBn) "লাইভ ট্র্যাকার ও জিপিএস রিভিউ" else "Live Tracker & GPS Review",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isBn) "ম্যানুয়াল এবং জিপিএস নিয়ন্ত্রণ" else "Manual & GPS Controls", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (state.activeSession == null) {
                            Button(onClick = onCheckIn, modifier = Modifier.weight(1f)) {
                                Text(if (isBn) "Check In" else "Check In")
                            }
                        } else {
                            Button(
                                onClick = onOpenExitSheet,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isBn) "Check Out" else "Check Out")
                            }
                        }
                        OutlinedButton(onClick = onTogglePauseGps, modifier = Modifier.weight(1f)) {
                            Text(if (state.prefs.gpsPaused) (if (isBn) "জিপিএস চালু" else "Resume GPS") else (if (isBn) "জিপিএস থামান" else "Pause GPS"))
                        }
                    }
                }
            }
        }

        if (state.recoverySessions.isNotEmpty()) {
            item {
                Text(if (isBn) "অসম্পূর্ণ সেশন রিকভারি" else "Session Recovery", fontWeight = FontWeight.SemiBold)
            }
            items(state.recoverySessions) { rs ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Check-in: ${DhakaTimeUtil.formatTime(rs.checkIn, bnDigits)}")
                        Button(onClick = { onResolveRecovery(rs, rs.checkIn + 8 * 3_600_000L) }) {
                            Text(if (isBn) "৮ ঘণ্টা ধরে সম্পন্ন" else "Close (+8h)")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "${if (isBn) "জিপিএস ইভেন্ট রিভিউ" else "GPS Event Review"} (${state.pendingEvents.size})",
                fontWeight = FontWeight.SemiBold
            )
        }
        items(state.pendingEvents) { ev ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(ev.eventType, fontWeight = FontWeight.Bold)
                        Text(DhakaTimeUtil.formatTime(ev.timestamp, bnDigits), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onConfirmEvent(ev) }) { Text(if (isBn) "নিশ্চিত" else "Confirm") }
                        OutlinedButton(onClick = { onDismissEvent(ev) }) { Text(if (isBn) "বাতিল" else "Dismiss") }
                    }
                }
            }
        }
    }
}

@Composable
fun ExitReasonSheet(
    state: DashboardUiState,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onAddCustomReason: (String, Boolean) -> Unit
) {
    val isBn = state.prefs.language == "bn"
    var note by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customBus by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBn) "দোকান থেকে বের হওয়ার কারণ" else "Select Exit Reason") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.reasons) { reason ->
                    OutlinedButton(
                        onClick = { onConfirm(reason.id, note) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isBn) reason.nameBn else reason.nameEn)
                    }
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (isBn) "ঐচ্ছিক নোট" else "Optional note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text(if (isBn) "নতুন কারণ যোগ করুন" else "Add custom reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row {
                            Checkbox(checked = customBus, onCheckedChange = { customBus = it })
                            Text(if (isBn) "ব্যবসায়িক" else "Business", modifier = Modifier.padding(top = 12.dp))
                        }
                        TextButton(onClick = {
                            if (customName.isNotBlank()) {
                                onAddCustomReason(customName, customBus)
                                customName = ""
                            }
                        }) {
                            Text(if (isBn) "যোগ করুন" else "Add")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isBn) "বাতিল" else "Cancel") }
        }
    )
}

@Composable
fun ManualSessionDialog(
    isBn: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long, Long, String) -> Unit
) {
    var hoursText by remember { mutableStateOf("8") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBn) "ম্যানুয়াল সেশন যোগ করুন" else "Add Manual Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text(if (isBn) "মোট ঘণ্টা (আজকের শেষ)" else "Duration (hours ending now)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isBn) "নোট" else "Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val hrs = hoursText.toDoubleOrNull()?.coerceIn(0.25, 20.0) ?: 1.0
                val now = System.currentTimeMillis()
                val start = now - (hrs * 3_600_000L).toLong()
                onSave(start, now, note)
                onDismiss()
            }) {
                Text(if (isBn) "সংরক্ষণ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isBn) "বাতিল" else "Cancel") }
        }
    )
}
