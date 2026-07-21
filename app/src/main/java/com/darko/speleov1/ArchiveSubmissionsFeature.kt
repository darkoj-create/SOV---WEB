package com.darko.speleov1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darko.speleov1.util.ArchiveSubmission
import com.darko.speleov1.util.ArchiveSubmissionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
internal fun ArchiveSubmissionsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<ArchiveSubmission>>(emptyList()) }
    var selected by remember { mutableStateOf<ArchiveSubmission?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("submitted") }
    var query by rememberSaveable { mutableStateOf("") }

    fun refresh() {
        loading = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { ArchiveSubmissionsRepository.load(context, filter) } }
            result.onSuccess { list ->
                rows = list
                selected = selected?.let { old -> list.firstOrNull { it.id == old.id } }
                message = "Učitano ${list.size} predanih jama."
            }.onFailure { err ->
                message = "Greška: ${err.message.orEmpty().take(120)}"
            }
            loading = false
        }
    }

    LaunchedEffect(filter) { refresh() }

    val filtered = remember(rows, query) {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) rows else rows.filter { item ->
            listOf(item.objectName, item.objectType, item.nearestPlace, item.county, item.municipality, item.submitterEmail, item.team, item.technicalDescription).joinToString(" ").lowercase(Locale.getDefault()).contains(q)
        }
    }

    CaveScreenBackground {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
                    Column(Modifier.weight(1f)) {
                        Text("Predane jame", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Pregledaj nove predaje.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { refresh() }, enabled = !loading, shape = RoundedCornerShape(16.dp)) { Text(if (loading) "Sync..." else "Osvježi") }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Traži predaju") })
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("submitted" to "Čeka review", "needs_changes" to "Fali nešto", "approved" to "Odobreno", "rejected" to "Odbijeno", "all" to "Sve").forEach { (id, label) ->
                                FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(label) })
                            }
                        }
                        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Text("Predaje (${filtered.size})", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (filtered.isEmpty()) {
                item { SovEmptyState(Icons.Default.Description, "Nema predaja", "Kad članovi predaju nove jame ili nacrte, pojavit će se ovdje.") }
            } else {
                items(filtered, key = { it.id }) { item ->
                    ArchiveSubmissionRow(item = item, selected = selected?.id == item.id, onClick = { selected = item })
                }
            }
        }
    }

    // APK UX FIX v1.4.17: Detail kartica kao full-screen Dialog, ne inline u listi
    val detailItem = selected
    if (detailItem != null) {
        Dialog(
            onDismissRequest = { selected = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { selected = null }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Zatvori detalj",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    detailItem.objectName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    listOf(detailItem.objectType, detailItem.nearestPlace)
                                        .filter { it.isNotBlank() }.joinToString(" · "),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    item {
                        ArchiveSubmissionDetailCard(
                            item = detailItem,
                            onApprove = { note ->
                                loading = true
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { ArchiveSubmissionsRepository.approve(context, detailItem.id, note) }
                                    }
                                    message = result.getOrElse { it }.let {
                                        if (it is Throwable) "Greška: ${it.message.orEmpty().take(120)}" else it.toString()
                                    }
                                    loading = false
                                    selected = null
                                    refresh()
                                }
                            },
                            onNeeds = { missing, note ->
                                loading = true
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { ArchiveSubmissionsRepository.markNeedsChanges(context, detailItem.id, missing, note) }
                                    }
                                    message = result.getOrElse { it }.let {
                                        if (it is Throwable) "Greška: ${it.message.orEmpty().take(120)}" else it.toString()
                                    }
                                    loading = false
                                    selected = null
                                    refresh()
                                }
                            },
                            onReject = { note ->
                                loading = true
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { ArchiveSubmissionsRepository.reject(context, detailItem.id, note) }
                                    }
                                    message = result.getOrElse { it }.let {
                                        if (it is Throwable) "Greška: ${it.message.orEmpty().take(120)}" else it.toString()
                                    }
                                    loading = false
                                    selected = null
                                    refresh()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveSubmissionEmpty() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)), shape = RoundedCornerShape(24.dp)) {
        Text("Nema predanih jama za ovaj filter.", modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArchiveSubmissionRow(item: ArchiveSubmission, selected: Boolean, onClick: () -> Unit) {
    val tint = when (item.status) {
        "approved" -> Color(0xFF26A69A)
        "needs_changes" -> Color(0xFFFFA726)
        "rejected" -> Color(0xFFEF5350)
        else -> Color(0xFF42A5F5)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = if (selected) 0.45f else 0.16f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(42.dp).background(tint.copy(alpha = 0.16f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Uvoz datoteke", tint = tint)
                }
                Column(Modifier.weight(1f)) {
                    Text(item.objectName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOf(item.objectType, item.nearestPlace, item.submitterEmail).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { item.id }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(statusLabel(item.status), color = tint, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ArchiveSubmissionChip("Privitci ${item.files.size}", Color(0xFF42A5F5))
                if (item.missingCategories.isNotEmpty()) ArchiveSubmissionChip("Fali: ${item.missingCategories.joinToString(", ")}", Color(0xFFFFA726))
                if (item.approvedObjectId.isNotBlank()) ArchiveSubmissionChip("Baza: ${item.approvedObjectId}", Color(0xFF26A69A))
            }
        }
    }
}

@Composable
private fun ArchiveSubmissionDetailCard(
    item: ArchiveSubmission,
    onApprove: (String) -> Unit,
    onNeeds: (List<String>, String) -> Unit,
    onReject: (String) -> Unit
) {
    var note by remember(item.id) { mutableStateOf(item.archivistNote) }
    var missing by remember(item.id) { mutableStateOf(item.missingCategories.toSet()) }
    var showApproveConfirm by remember(item.id) { mutableStateOf(false) }
    val missingOptions = listOf("koordinate", "pločica", "fotka", "nacrt", "zapisnik", "KML točka", "GPX trag", "TopoDroid ZIP", "opis", "ekipa/autori")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(item.objectName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(listOf(item.objectType, item.nearestPlace, item.submitterEmail).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { item.id }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ArchiveSubmissionChip(statusLabel(item.status), Color(0xFF42A5F5))
            }
            ArchiveSubmissionKv("Lokacija", listOf(item.county, item.municipality, item.nearestPlace).filter { it.isNotBlank() }.joinToString(" · "))
            ArchiveSubmissionKv("Koordinate", listOf(item.lat, item.lon).filter { it.isNotBlank() }.joinToString(", "))
            ArchiveSubmissionKv("Dimenzije", listOf(item.depthM.takeIf { it.isNotBlank() }?.let { "dubina $it m" }, item.lengthM.takeIf { it.isNotBlank() }?.let { "duljina $it m" }).filterNotNull().joinToString(" · "))
            ArchiveSubmissionKv("Ekipa/datum", listOf(item.team, item.surveyDate).filter { it.isNotBlank() }.joinToString(" · "))
            ArchiveSubmissionText("Pristup", item.accessDescription)
            ArchiveSubmissionText("Opis / tehnički opis", item.technicalDescription)
            ArchiveSubmissionText("Istraživanje / povijest", item.researchHistory)
            ArchiveSubmissionText("Napomene", item.notes)
            Text("Privitci", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            if (item.files.isEmpty()) Text("Nema privitaka.", color = MaterialTheme.colorScheme.onSurfaceVariant) else item.files.forEach { file ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Download, contentDescription = "Preuzmi", tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(file.fileName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${file.fileType} · ${file.sizeBytes / 1024} KB", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Označi što fali", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                missingOptions.forEach { opt ->
                    FilterChip(selected = missing.contains(opt), onClick = { missing = if (missing.contains(opt)) missing - opt else missing + opt }, label = { Text(opt) })
                }
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("Arhivarska napomena") })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showApproveConfirm = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Potvrđeno")
                    Spacer(Modifier.width(6.dp))
                    Text("Odobri")
                }
                FilledTonalButton(onClick = { onNeeds(missing.toList(), note) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text("Fali nešto") }
            }
            OutlinedButton(
                onClick = { onReject(note) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Zatvori")
                Spacer(Modifier.width(6.dp))
                Text("Odbij predaju")
            }

            // APK UX FIX v1.4.17: Potvrda prije odobravanja
            if (showApproveConfirm) {
                AlertDialog(
                    onDismissRequest = { showApproveConfirm = false },
                    title = { Text("Potvrdi odobrenje", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "Odobriti ${item.objectName}?"
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showApproveConfirm = false
                                onApprove(note)
                            }
                        ) { Text("Odobri") }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showApproveConfirm = false }) {
                            Text("Odustani")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ArchiveSubmissionKv(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ArchiveSubmissionText(label: String, value: String) {
    if (value.isBlank()) return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ArchiveSubmissionChip(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.25f))) {
        Text(label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

private fun statusLabel(status: String): String = when (status) {
    "approved" -> "Odobreno"
    "needs_changes" -> "Fali nešto"
    "rejected" -> "Odbijeno"
    else -> "Čeka review"
}
