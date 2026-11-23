package com.shoptimemaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoptimemaster.app.data.local.entity.TimeSessionEntity
import com.shoptimemaster.app.domain.util.DhakaTimeUtil
import com.shoptimemaster.app.ui.viewmodel.DashboardUiState

@Composable
fun HistoryAndJournalScreen(
    state: DashboardUiState,
    onDeleteSession: (TimeSessionEntity) -> Unit,
    onSaveJournal: (String, String, String, String, String) -> Unit
) {
    val isBn = state.prefs.language == "bn"
    val bnDigits = state.prefs.useBengaliNumerals
    var subTab by remember { mutableIntStateOf(0) }

    val todayStr = DhakaTimeUtil.localDateFromEpoch(state.nowMs).toString()
    val existingJ = state.journals.firstOrNull { it.date == todayStr }
    var completed by remember(existingJ) { mutableStateOf(existingJ?.completedTasks ?: "") }
    var pending by remember(existingJ) { mutableStateOf(existingJ?.pendingTasks ?: "") }
    var problems by remember(existingJ) { mutableStateOf(existingJ?.businessProblems ?: "") }
    var tomorrow by remember(existingJ) { mutableStateOf(existingJ?.tomorrowPlan ?: "") }
    var savedMsg by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isBn) "ইতিহাস, ডায়েরি ও অডিট লগ" else "History, Journal & Audit",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text(if (isBn) "সেশন" else "Sessions") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text(if (isBn) "ডায়েরি" else "Journal") })
            Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text(if (isBn) "অডিট" else "Audit") })
        }

        when (subTab) {
            0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.sessions.isEmpty()) {
                    item {
                        Text(if (isBn) "কোনো সেশন রেকর্ড নেই।" else "No sessions recorded yet.", modifier = Modifier.padding(16.dp))
                    }
                }
                items(state.sessions) { s ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "${DhakaTimeUtil.localDateFromEpoch(s.checkIn)} · ${DhakaTimeUtil.formatTime(s.checkIn, bnDigits)} — ${
                                        s.checkOut?.let { DhakaTimeUtil.formatTime(it, bnDigits) } ?: "Active"
                                    }",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("${s.source} · ${s.status}", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onDeleteSession(s) }) {
                                Text(if (isBn) "মুছুন" else "Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(value = completed, onValueChange = { completed = it }, label = { Text(if (isBn) "সম্পন্ন কাজ" else "Completed Tasks") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = pending, onValueChange = { pending = it }, label = { Text(if (isBn) "অসম্পূর্ণ কাজ" else "Pending Tasks") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = problems, onValueChange = { problems = it }, label = { Text(if (isBn) "ব্যবসায়িক সমস্যা" else "Business Problems") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = tomorrow, onValueChange = { tomorrow = it }, label = { Text(if (isBn) "আগামীকালের পরিকল্পনা" else "Tomorrow's Plan") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onSaveJournal(todayStr, completed, pending, problems, tomorrow)
                            savedMsg = if (isBn) "ডায়েরি সংরক্ষিত হয়েছে।" else "Journal saved."
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (isBn) "সংরক্ষণ করুন" else "Save Journal")
                    }
                    if (savedMsg.isNotEmpty()) {
                        Text(savedMsg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.auditLogs) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${log.entityType} · ${log.reason}", fontWeight = FontWeight.SemiBold)
                            Text("${log.oldValue} -> ${log.newValue}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
