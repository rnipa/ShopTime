package com.shoptimemaster.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.shoptimemaster.app.domain.util.DhakaTimeUtil
import com.shoptimemaster.app.export.ReportExporter
import com.shoptimemaster.app.ui.viewmodel.DashboardUiState

@Composable
fun AnalyticsScreen(
    state: DashboardUiState,
    onSaveIncome: (String, Double, Double, Double, Double, Boolean) -> Unit,
    onSaveGoals: (Int, Int, Int, String, String, Boolean, Boolean) -> Unit
) {
    val isBn = state.prefs.language == "bn"
    val bnDigits = state.prefs.useBengaliNumerals
    val context = LocalContext.current
    var subTab by remember { mutableIntStateOf(0) }

    val today = DhakaTimeUtil.localDateFromEpoch(state.nowMs)
    val monthStr = "${today.year}-${today.monthValue.toString().padStart(2, '0')}"
    val curInc = state.incomeRecords.firstOrNull { it.month == monthStr }
    var revText by remember(curInc) { mutableStateOf(curInc?.revenue?.toString() ?: "") }
    var cogsText by remember(curInc) { mutableStateOf(curInc?.costOfGoods?.toString() ?: "") }
    var expText by remember(curInc) { mutableStateOf(curInc?.expenses?.toString() ?: "") }
    var otherText by remember(curInc) { mutableStateOf(curInc?.otherCosts?.toString() ?: "") }
    var confirmed by remember(curInc) { mutableStateOf(curInc?.isConfirmed ?: false) }

    var dailyHrs by remember(state.goalSettings) { mutableStateOf((state.goalSettings.dailyMinutes / 60).toString()) }
    var weeklyHrs by remember(state.goalSettings) { mutableStateOf((state.goalSettings.weeklyMinutes / 60).toString()) }
    var monthlyHrs by remember(state.goalSettings) { mutableStateOf((state.goalSettings.monthlyMinutes / 60).toString()) }
    var lunchWork by remember(state.goalSettings) { mutableStateOf(state.goalSettings.countLunchAsWork) }
    var restWork by remember(state.goalSettings) { mutableStateOf(state.goalSettings.countRestAsWork) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isBn) "স্মার্ট বিশ্লেষণ ও আয়" else "Analytics & Income",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text(if (isBn) "চার্ট" else "Charts") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text(if (isBn) "আয় (৳)" else "Income") })
            Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text(if (isBn) "লক্ষ্য" else "Goals") })
            Tab(selected = subTab == 3, onClick = { subTab = 3 }, text = { Text(if (isBn) "কোচ" else "Coach") })
        }

        when (subTab) {
            0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (isBn) "গত ৭ দিনের কর্মঘণ্টা" else "Last 7 Days Business Hours", fontWeight = FontWeight.SemiBold)
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val maxMins = (state.last7Days.maxOfOrNull { it.totalBusinessMinutes } ?: 60L).coerceAtLeast(60L)
                            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                val count = state.last7Days.size.coerceAtLeast(1)
                                val barW = size.width / (count * 2f)
                                state.last7Days.forEachIndexed { idx, day ->
                                    val hRatio = (day.totalBusinessMinutes.toFloat() / maxMins).coerceIn(0f, 1f)
                                    val barH = size.height * hRatio
                                    drawRect(
                                        color = primaryColor,
                                        topLeft = Offset(idx * (size.width / count) + barW * 0.5f, size.height - barH),
                                        size = Size(barW, barH)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val rows = listOf(listOf("Date", "Presence(m)", "BusOutside(m)", "PersonalOutside(m)", "Completion(%)")) +
                                    state.last7Days.map {
                                        listOf(it.date.toString(), it.presenceMinutes.toString(), it.businessOutsideMinutes.toString(), it.personalOutsideMinutes.toString(), it.completionPct.toString())
                                    }
                                val file = ReportExporter.exportCsv(context, "shop_time_report.csv", rows, excelBom = true)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Export CSV"))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Excel CSV") }

                        OutlinedButton(
                            onClick = {
                                val lines = state.last7Days.map {
                                    "${it.date}: ${it.presenceMinutes}m shop, ${it.businessOutsideMinutes}m bus-out, ${it.completionPct}%"
                                }
                                val file = ReportExporter.exportPdfReport(context, "shop_time_report.pdf", "SHOP TIME MASTER Report", lines)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Export PDF"))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("PDF") }
                    }
                }
            }

            1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(value = revText, onValueChange = { revText = it }, label = { Text(if (isBn) "মাসিক মোট বিক্রি (৳)" else "Monthly Revenue (৳)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cogsText, onValueChange = { cogsText = it }, label = { Text(if (isBn) "পণ্য ক্রয় খরচ / COGS (৳)" else "Cost of Goods (৳)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = expText, onValueChange = { expText = it }, label = { Text(if (isBn) "পরিচালন ব্যয় (৳)" else "Expenses (৳)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = otherText, onValueChange = { otherText = it }, label = { Text(if (isBn) "অন্যান্য খরচ (৳)" else "Other Costs (৳)") }, modifier = Modifier.fillMaxWidth())
                    Row {
                        Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                        Text(if (isBn) "নিশ্চিত নিট লাভ" else "Confirmed Net Profit", modifier = Modifier.padding(top = 12.dp))
                    }
                    Button(
                        onClick = {
                            onSaveIncome(
                                monthStr,
                                revText.toDoubleOrNull() ?: 0.0,
                                cogsText.toDoubleOrNull() ?: 0.0,
                                expText.toDoubleOrNull() ?: 0.0,
                                otherText.toDoubleOrNull() ?: 0.0,
                                confirmed
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isBn) "সংরক্ষণ করুন" else "Save Income") }

                    val inc = state.monthIncomeMetrics
                    if (inc != null) {
                        Spacer(Modifier.height(10.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${if (isBn) "নিট লাভ:" else "Net Profit:"} ${DhakaTimeUtil.formatBdt(inc.netProfit, bnDigits)}", fontWeight = FontWeight.Bold)
                                Text("${if (isBn) "ঘণ্টাপ্রতি আয় (ব্যবসায়িক):" else "Revenue/Business Hr:"} ${DhakaTimeUtil.formatBdt(inc.revenuePerBusinessHour, bnDigits)}")
                                Text("${if (isBn) "ঘণ্টাপ্রতি লাভ (ব্যবসায়িক):" else "Profit/Business Hr:"} ${DhakaTimeUtil.formatBdt(inc.profitPerBusinessHour, bnDigits)}")
                            }
                        }
                    }
                }
            }

            2 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(value = dailyHrs, onValueChange = { dailyHrs = it }, label = { Text(if (isBn) "দৈনিক লক্ষ্য (ঘণ্টা)" else "Daily Target (hrs)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = weeklyHrs, onValueChange = { weeklyHrs = it }, label = { Text(if (isBn) "সাপ্তাহিক লক্ষ্য (ঘণ্টা)" else "Weekly Target (hrs)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = monthlyHrs, onValueChange = { monthlyHrs = it }, label = { Text(if (isBn) "মাসিক লক্ষ্য (ঘণ্টা)" else "Monthly Target (hrs)") }, modifier = Modifier.fillMaxWidth())
                    Row {
                        Checkbox(checked = lunchWork, onCheckedChange = { lunchWork = it })
                        Text(if (isBn) "দুপুরের খাবার কর্মঘণ্টায় ধরুন" else "Count lunch as work", modifier = Modifier.padding(top = 12.dp))
                    }
                    Row {
                        Checkbox(checked = restWork, onCheckedChange = { restWork = it })
                        Text(if (isBn) "বিশ্রাম কর্মঘণ্টায় ধরুন" else "Count rest as work", modifier = Modifier.padding(top = 12.dp))
                    }
                    Button(
                        onClick = {
                            onSaveGoals(
                                (dailyHrs.toIntOrNull() ?: 10) * 60,
                                (weeklyHrs.toIntOrNull() ?: 60) * 60,
                                (monthlyHrs.toIntOrNull() ?: 260) * 60,
                                state.goalSettings.preferredStart,
                                state.goalSettings.preferredEnd,
                                lunchWork,
                                restWork
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isBn) "লক্ষ্য সংরক্ষণ করুন" else "Save Goals") }
                }
            }

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.leakageInsights.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(if (isBn) "অফলাইন স্মার্ট কোচ" else "Offline Smart Coach", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (isBn) "পর্যাপ্ত বিশ্লেষণের জন্য অন্তত ২ দিনের সেশন রেকর্ড করুন।"
                                    else "Record at least 2 active days to unlock personalized time leakage insights."
                                )
                            }
                        }
                    }
                } else {
                    state.leakageInsights.forEach { ins ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (isBn) ins.titleBn else ins.titleEn, fontWeight = FontWeight.Bold)
                                    Text(if (isBn) ins.detailBn else ins.detailEn)
                                    Text(if (isBn) ins.recommendationBn else ins.recommendationEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: DashboardUiState,
    onToggleLanguage: (String) -> Unit,
    onToggleNumerals: (Boolean) -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onWipeAll: () -> Unit
) {
    val isBn = state.prefs.language == "bn"
    var confirmWipe by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (isBn) "সেটিংস" else "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (isBn) "ভাষা ও সংখ্যা" else "Language & Numerals", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isBn, onClick = { onToggleLanguage("bn") }, label = { Text("বাংলা (bn-BD)") })
                        FilterChip(selected = !isBn, onClick = { onToggleLanguage("en") }, label = { Text("English") })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBn) "বাংলা সংখ্যা (০১২৩)" else "Bengali Numerals (০১২৩)")
                        Switch(checked = state.prefs.useBengaliNumerals, onCheckedChange = onToggleNumerals)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBn) "ডার্ক মোড" else "Dark Mode")
                        Switch(checked = state.prefs.darkTheme, onCheckedChange = onToggleTheme)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isBn) "ডেটা ব্যবস্থাপনা" else "Data Management", fontWeight = FontWeight.SemiBold)
                    if (!confirmWipe) {
                        OutlinedButton(onClick = { confirmWipe = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (isBn) "সব ডেটা মুছে ফেলুন" else "Delete All Data", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Button(
                            onClick = {
                                onWipeAll()
                                confirmWipe = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isBn) "হ্যাঁ, স্থায়ীভাবে মুছুন" else "Confirm Permanent Wipe")
                        }
                    }
                }
            }
        }
    }
}
