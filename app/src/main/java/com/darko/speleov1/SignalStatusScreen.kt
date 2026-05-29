package com.darko.speleov1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private data class VisibleNetwork(
    val operator: String,
    val technology: String,
    val dbm: Int,
    val registered: Boolean,
    val cellId: String = "—",
    val areaCode: String = "—",
    val pci: String = "—",
    val channel: String = "—"
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SignalStatusScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val telephony = remember { context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    var hasPhonePermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
    }
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var operatorName by remember { mutableStateOf(telephony.networkOperatorName.orEmpty().ifBlank { "—" }) }
    var networkType by remember { mutableStateOf("—") }
    var signalDbm by remember { mutableIntStateOf(Int.MIN_VALUE) }
    var visibleNetworks by remember { mutableStateOf<List<VisibleNetwork>>(emptyList()) }
    var visibleNetworksMessage by remember { mutableStateOf<String?>(null) }
    val phonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasPhonePermission = granted }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasLocationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPhonePermission) phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        if (!hasLocationPermission) locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    DisposableEffect(hasPhonePermission) {
        operatorName = telephony.networkOperatorName.orEmpty().ifBlank { "—" }
        networkType = readNetworkTypeSafe(telephony, hasPhonePermission)
        if (!hasPhonePermission) return@DisposableEffect onDispose { }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    signalDbm = extractSignalDbm(signalStrength)
                    operatorName = telephony.networkOperatorName.orEmpty().ifBlank { "—" }
                    networkType = readNetworkTypeSafe(telephony, true)
                }
            }
            telephony.registerTelephonyCallback(ContextCompat.getMainExecutor(context), callback)
            onDispose { telephony.unregisterTelephonyCallback(callback) }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Android 12")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    signalDbm = extractSignalDbm(signalStrength)
                    operatorName = telephony.networkOperatorName.orEmpty().ifBlank { "—" }
                    networkType = readNetworkTypeSafe(telephony, true)
                }
            }
            @Suppress("DEPRECATION")
            telephony.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            onDispose {
                @Suppress("DEPRECATION")
                fun stopListening() {
                    telephony.listen(listener, PhoneStateListener.LISTEN_NONE)
                }
                stopListening()
            }
        }
    }

    fun refreshVisibleNetworks() {
        if (!hasLocationPermission) {
            visibleNetworks = emptyList()
            visibleNetworksMessage = "Nema dozvole za lokaciju"
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        val scanned = readVisibleNetworks(context, telephony)
        visibleNetworks = scanned
        visibleNetworksMessage = if (scanned.isEmpty()) {
            "Android nije vratio dodatne mreže na ovoj lokaciji"
        } else {
            null
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) refreshVisibleNetworks()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Signal i pokrivenost") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Natrag") } }) }
    ) { inner ->
        CaveScreenBackground {
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(inner).padding(20.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(Color(0xFF42A5F5).copy(alpha = 0.16f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color(0xFF42A5F5), modifier = Modifier.size(31.dp))
                    }
                    Column {
                        Text("Signal i pokrivenost", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Status mreže i vidljive ćelije", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("Status mreže", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (!hasPhonePermission) {
                    StatusCard("Dozvola", "Nema dozvole za čitanje mreže", "Dopusti READ_PHONE_STATE za mrežni tip i signal.", Color(0xFFFF7043))
                    TextButton(onClick = { phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }) { Text("Dopusti mrežu") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusCard("Operator", operatorName, "trenutna mreža", Color(0xFF42A5F5), Modifier.weight(1f))
                        StatusCard("Tip", networkType, "2G / 3G / 4G / 5G", Color(0xFF80CBC4), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusCard("Signal", if (signalDbm == Int.MIN_VALUE) "—" else "$signalDbm dBm", "jačina signala", signalColor(signalDbm), Modifier.weight(1f))
                        SignalBarsCard(signalBars(signalDbm), signalColor(signalDbm), Modifier.weight(1f))
                    }
                }

                Text("Dostupne mreže u okolici", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Android može prikazati ćelije/bazne stanice koje mobitel vidi u okolici. App može očitati Cell ID, TAC/LAC, PCI/PSC i kanal kad uređaj to vrati, ali ne zna stvarnu GPS lokaciju tornja bez vanjske baze baznih stanica.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { refreshVisibleNetworks() }, enabled = hasLocationPermission) { Text("Osvježi ćelije") }
                        if (!hasLocationPermission) {
                            Text("Nema dozvole za lokaciju", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        } else if (visibleNetworks.isEmpty()) {
                            Text(visibleNetworksMessage ?: "Nema dodatnih mreža za prikaz", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        } else {
                            visibleNetworks.forEach { network ->
                                AvailableNetworkRow(network)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    label: String,
    value: String,
    helper: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label.uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(helper, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SignalBarsCard(bars: Int, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("BARS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                (1..5).forEach { idx ->
                    Box(Modifier.weight(1f).height((12 + idx * 7).dp).background(if (idx <= bars) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(5.dp)))
                }
            }
            Text("$bars/5", color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AvailableNetworkRow(network: VisibleNetwork) {
    val dbmText = if (network.dbm == Int.MIN_VALUE) "jačina nije dostupna" else "${network.dbm} dBm"
    val tint = signalColor(network.dbm)
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(16.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SignalMiniBars(signalBars(network.dbm), tint, Modifier.weight(0.25f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(network.operator.ifBlank { "Nepoznata mreža" }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("${network.technology} • ${if (network.registered) "spojeno" else "vidljivo"} • $dbmText", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("Cell ID: ${network.cellId} • ${network.areaCode} • ${network.pci} • ${network.channel}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SignalMiniBars(bars: Int, tint: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier.height(28.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        (1..5).forEach { idx ->
            Box(Modifier.weight(1f).height((5 + idx * 4).dp).background(if (idx <= bars) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(3.dp)))
        }
    }
}

@SuppressLint("MissingPermission")
private fun readVisibleNetworks(context: Context, telephony: TelephonyManager): List<VisibleNetwork> {
    val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation) return emptyList()
    return runCatching {
        val cells = telephony.allCellInfo.orEmpty()
        val networks = cells.mapNotNull { cell ->
            val operator = cellOperatorLabel(cell).ifBlank { "Nepoznata mreža" }
            val technology = cellTechnologyLabel(cell)
            val dbm = cellSignalDbm(cell)
            val details = cellTowerDetails(cell)
            VisibleNetwork(
                operator = operator,
                technology = technology,
                dbm = dbm,
                registered = cell.isRegistered,
                cellId = details.cellId,
                areaCode = details.areaCode,
                pci = details.pci,
                channel = details.channel
            )
        }
        networks
            .groupBy { it.operator + "|" + it.technology }
            .map { (_, items) ->
                items.sortedWith(
                    compareByDescending<VisibleNetwork> { it.registered }
                        .thenByDescending { if (it.dbm == Int.MIN_VALUE) -999 else it.dbm }
                ).first()
            }
            .sortedWith(
                compareByDescending<VisibleNetwork> { it.registered }
                    .thenByDescending { if (it.dbm == Int.MIN_VALUE) -999 else it.dbm }
            )
            .take(12)
    }.getOrDefault(emptyList())
}

private data class CellTowerDetails(
    val cellId: String = "—",
    val areaCode: String = "—",
    val pci: String = "—",
    val channel: String = "—"
)

private fun cellOperatorLabel(cell: CellInfo): String = runCatching {
    when (cell) {
        is CellInfoLte -> cell.cellIdentity.operatorAlphaLong?.toString().orEmpty().ifBlank { "${cell.cellIdentity.mccString.orEmpty()}-${cell.cellIdentity.mncString.orEmpty()}" }
        is CellInfoGsm -> cell.cellIdentity.operatorAlphaLong?.toString().orEmpty().ifBlank { "${cell.cellIdentity.mccString.orEmpty()}-${cell.cellIdentity.mncString.orEmpty()}" }
        is CellInfoWcdma -> cell.cellIdentity.operatorAlphaLong?.toString().orEmpty().ifBlank { "${cell.cellIdentity.mccString.orEmpty()}-${cell.cellIdentity.mncString.orEmpty()}" }
        is CellInfoCdma -> cell.cellIdentity.operatorAlphaLong?.toString().orEmpty()
        is CellInfoNr -> cell.cellIdentity.operatorAlphaLong?.toString().orEmpty()
        else -> ""
    }.trim('-')
}.getOrDefault("")


private fun cellTowerDetails(cell: CellInfo): CellTowerDetails = runCatching {
    when (cell) {
        is CellInfoLte -> {
            val id = cell.cellIdentity
            CellTowerDetails(
                cellId = id.ci.takeIf { it != Int.MAX_VALUE }?.toString() ?: "—",
                areaCode = id.tac.takeIf { it != Int.MAX_VALUE }?.let { "TAC $it" } ?: "—",
                pci = id.pci.takeIf { it != Int.MAX_VALUE }?.let { "PCI $it" } ?: "—",
                channel = id.earfcn.takeIf { it != Int.MAX_VALUE }?.let { "EARFCN $it" } ?: "—"
            )
        }
        is CellInfoNr -> {
            val id = cell.cellIdentity as? android.telephony.CellIdentityNr
            CellTowerDetails(
                cellId = id?.nci?.takeIf { it != Long.MAX_VALUE }?.toString() ?: "—",
                areaCode = id?.tac?.takeIf { it != Int.MAX_VALUE }?.let { "TAC $it" } ?: "—",
                pci = id?.pci?.takeIf { it != Int.MAX_VALUE }?.let { "PCI $it" } ?: "—",
                channel = id?.nrarfcn?.takeIf { it != Int.MAX_VALUE }?.let { "NRARFCN $it" } ?: "—"
            )
        }
        is CellInfoWcdma -> {
            val id = cell.cellIdentity
            CellTowerDetails(
                cellId = id.cid.takeIf { it != Int.MAX_VALUE }?.toString() ?: "—",
                areaCode = id.lac.takeIf { it != Int.MAX_VALUE }?.let { "LAC $it" } ?: "—",
                pci = id.psc.takeIf { it != Int.MAX_VALUE }?.let { "PSC $it" } ?: "—",
                channel = runCatching { id.uarfcn.takeIf { it != Int.MAX_VALUE }?.let { "UARFCN $it" } }.getOrNull() ?: "—"
            )
        }
        is CellInfoGsm -> {
            val id = cell.cellIdentity
            CellTowerDetails(
                cellId = id.cid.takeIf { it != Int.MAX_VALUE }?.toString() ?: "—",
                areaCode = id.lac.takeIf { it != Int.MAX_VALUE }?.let { "LAC $it" } ?: "—",
                pci = runCatching { id.bsic.takeIf { it != Int.MAX_VALUE }?.let { "BSIC $it" } }.getOrNull() ?: "—",
                channel = runCatching { id.arfcn.takeIf { it != Int.MAX_VALUE }?.let { "ARFCN $it" } }.getOrNull() ?: "—"
            )
        }
        is CellInfoCdma -> {
            val id = cell.cellIdentity
            CellTowerDetails(
                cellId = id.basestationId.takeIf { it != Int.MAX_VALUE }?.toString() ?: "—",
                areaCode = id.networkId.takeIf { it != Int.MAX_VALUE }?.let { "NID $it" } ?: "—",
                pci = id.systemId.takeIf { it != Int.MAX_VALUE }?.let { "SID $it" } ?: "—",
                channel = "CDMA"
            )
        }
        else -> CellTowerDetails()
    }
}.getOrDefault(CellTowerDetails())

private fun cellTechnologyLabel(cell: CellInfo): String = when (cell) {
    is CellInfoLte -> "4G"
    is CellInfoNr -> "5G"
    is CellInfoWcdma -> "3G"
    is CellInfoGsm -> "2G"
    is CellInfoCdma -> "CDMA"
    else -> "nepoznato"
}

private fun cellSignalDbm(cell: CellInfo): Int = runCatching {
    when (cell) {
        is CellInfoLte -> cell.cellSignalStrength.dbm
        is CellInfoGsm -> cell.cellSignalStrength.dbm
        is CellInfoWcdma -> cell.cellSignalStrength.dbm
        is CellInfoCdma -> cell.cellSignalStrength.dbm
        is CellInfoNr -> cell.cellSignalStrength.dbm
        else -> Int.MIN_VALUE
    }
}.getOrDefault(Int.MIN_VALUE)

private fun extractSignalDbm(signalStrength: SignalStrength): Int = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        signalStrength.cellSignalStrengths.firstOrNull()?.dbm ?: Int.MIN_VALUE
    } else {
        Int.MIN_VALUE
    }
}.getOrDefault(Int.MIN_VALUE)

private fun readNetworkTypeSafe(telephony: TelephonyManager, hasPermission: Boolean): String {
    if (!hasPermission) return "—"
    return runCatching { networkTypeLabel(telephony.dataNetworkType) }.getOrDefault("—")
}

private fun networkTypeLabel(type: Int): String = when (type) {
    TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_GSM -> "2G"
    TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"
    TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN -> "4G"
    TelephonyManager.NETWORK_TYPE_NR -> "5G"
    else -> "nepoznato"
}

private fun signalBars(dbm: Int): Int = when {
    dbm == Int.MIN_VALUE -> 0
    dbm > -75 -> 5
    dbm > -85 -> 4
    dbm > -95 -> 3
    dbm > -105 -> 2
    else -> 1
}

private fun signalColor(dbm: Int): Color = when {
    dbm == Int.MIN_VALUE -> Color(0xFF9E9E9E)
    dbm > -85 -> Color(0xFF4CAF50)
    dbm > -100 -> Color(0xFFFFD54F)
    else -> Color(0xFFFF5252)
}
