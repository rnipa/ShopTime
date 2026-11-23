package com.shoptimemaster.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.shoptimemaster.app.ui.viewmodel.DashboardUiState

@SuppressLint("MissingPermission")
@Composable
fun OnboardingSetupScreen(
    state: DashboardUiState,
    onToggleLanguage: (String) -> Unit,
    onSaveShop: (String, String, Double?, Double?, Float, Boolean) -> Unit
) {
    val isBn = state.prefs.language == "bn"
    val context = LocalContext.current
    var shopName by remember { mutableStateOf(state.shop?.name ?: "") }
    var shopAddress by remember { mutableStateOf(state.shop?.address ?: "") }
    var latText by remember { mutableStateOf(state.shop?.latitude?.toString() ?: "") }
    var lngText by remember { mutableStateOf(state.shop?.longitude?.toString() ?: "") }
    var radius by remember { mutableFloatStateOf(state.shop?.geofenceRadiusMeters ?: 100f) }
    var gpsMsg by remember { mutableStateOf("") }

    val locPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    latText = "%.6f".format(loc.latitude)
                    lngText = "%.6f".format(loc.longitude)
                    gpsMsg = if (isBn) "অবস্থান সংরক্ষিত হয়েছে" else "Location captured"
                }
            }
        } else {
            gpsMsg = if (isBn) "লোকেশন অনুমতি ছাড়াও ম্যানুয়াল ট্র্যাকিং চলবে।" else "Manual tracking works without location."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SHOP TIME MASTER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = isBn, onClick = { onToggleLanguage("bn") }, label = { Text("বাংলা") })
                FilterChip(selected = !isBn, onClick = { onToggleLanguage("en") }, label = { Text("EN") })
            }
        }

        Text(
            text = if (isBn) "আপনার দোকানের তথ্য সেটআপ করুন" else "Set Up Your Shop",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isBn) {
                "সমস্ত সময়, আয় এবং ডায়েরির তথ্য সম্পূর্ণ অফলাইনে আপনার ফোনেই সুরক্ষিত থাকবে।"
            } else {
                "All time sessions, income, and journal records stay offline and private on your device."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = shopName,
            onValueChange = { shopName = it },
            label = { Text(if (isBn) "দোকানের নাম *" else "Shop Name *") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = shopAddress,
            onValueChange = { shopAddress = it },
            label = { Text(if (isBn) "দোকানের ঠিকানা" else "Shop Address") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = {
                locPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isBn) "বর্তমান জিপিএস অবস্থান নিন (ঐচ্ছিক)" else "Capture Current GPS Location (Optional)")
        }
        if (gpsMsg.isNotEmpty()) {
            Text(gpsMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = { lngText = it },
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "${if (isBn) "জিওফেন্স ব্যাসার্ধ:" else "Geofence Radius:"} ${radius.toInt()}m",
            style = MaterialTheme.typography.bodySmall
        )
        Slider(value = radius, onValueChange = { radius = it }, valueRange = 50f..500f)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    onSaveShop(
                        shopName.ifBlank { "আমার দোকান" },
                        shopAddress,
                        latText.toDoubleOrNull(),
                        lngText.toDoubleOrNull(),
                        radius,
                        false
                    )
                },
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text(if (isBn) "শুধু ম্যানুয়াল" else "Manual Only")
            }
            Button(
                onClick = {
                    onSaveShop(
                        shopName.ifBlank { "আমার দোকান" },
                        shopAddress,
                        latText.toDoubleOrNull(),
                        lngText.toDoubleOrNull(),
                        radius,
                        latText.toDoubleOrNull() != null && lngText.toDoubleOrNull() != null
                    )
                },
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text(if (isBn) "শুরু করুন" else "Start App")
            }
        }
    }
}
