@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.darko.speleov1.nacrt

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darko.speleov1.LocalAppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NacrtScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current

    var survey by remember { mutableStateOf<NacrtZipParser.NacrtSurvey?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var renderTrigger by remember { mutableStateOf(0) }

    // Editable fields
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var team by remember { mutableStateOf("") }
    var club by remember { mutableStateOf("SO Velebit") }
    var cadastreNum by remember { mutableStateOf("") }

    // Process selected ZIP
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingUri = uri
    }

    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        loading = true; error = null
        try {
            val result = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)!!.use { NacrtZipParser.parse(it) }
            }
            survey = result
            title = result.name; date = result.date; team = result.team
            // Auto-render
            val bmp = withContext(Dispatchers.IO) {
                NacrtRenderer.render(result, NacrtRenderer.RenderOptions(
                    title = result.name, date = result.date, team = result.team, club = club
                ))
            }
            bitmap = bmp
        } catch (e: Exception) {
            error = e.message ?: "Greška pri parsiranju"
        }
        loading = false
    }

    // Re-render when fields change via button
    LaunchedEffect(renderTrigger) {
        if (renderTrigger == 0) return@LaunchedEffect
        val sv = survey ?: return@LaunchedEffect
        loading = true
        val bmp = withContext(Dispatchers.IO) {
            NacrtRenderer.render(sv, NacrtRenderer.RenderOptions(
                title = title, date = date, team = team, club = club, cadastreNum = cadastreNum
            ))
        }
        bitmap = bmp
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Architecture, contentDescription = null, tint = Color(0xFFE6C36A))
                Text(language.pick("Nacrt Generator", "Survey Drawing Generator"))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // File picker button
                OutlinedButton(
                    onClick = { zipPicker.launch("application/zip") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(language.pick("Odaberi TopoDroid ZIP", "Select TopoDroid ZIP"))
                }

                // Loading
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(language.pick("Parsiram ZIP…", "Parsing ZIP…"))
                    }
                }

                // Error
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Survey loaded — show stats & form
                survey?.let { s ->
                    // Stats
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatChip(language.pick("Duljina", "Length"), "${s.sql.stats.duljina.toString().replace('.', ',')} m")
                            StatChip(language.pick("Horiz.", "Horiz."), "${s.sql.stats.horizontalna.toString().replace('.', ',')} m")
                            StatChip(language.pick("Dubina", "Depth"), "${s.sql.stats.dubina.toString().replace('.', ',')} m")
                        }
                    }

                    // Editable fields
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text(language.pick("Naziv", "Title")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = team, onValueChange = { team = it },
                        label = { Text(language.pick("Mjerili", "Team")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = club, onValueChange = { club = it },
                        label = { Text(language.pick("Klub", "Club")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )

                    // Re-render button
                    Button(
                        onClick = { renderTrigger++ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(language.pick("Osvježi nacrt", "Refresh drawing"))
                    }
                }

                // Preview
                bitmap?.let { bmp ->
                    Text(
                        language.pick("Pregled nacrta", "Drawing preview"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Nacrt",
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        )
                    }

                    // Export buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            savePng(context, bmp, survey?.name ?: "nacrt", language)
                        }) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PNG")
                        }
                        OutlinedButton(onClick = {
                            sharePng(context, bmp, survey?.name ?: "nacrt")
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(language.pick("Podijeli", "Share"))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("Zatvori", "Close"))
            }
        }
    )
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

private fun savePng(context: Context, bitmap: Bitmap, name: String, language: Any?) {
    runCatching {
        val safeName = name.replace(Regex("[^a-zA-Z0-9_čćžšđČĆŽŠĐ -]"), "_")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$safeName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SOV Nacrti")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) } }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SOV Nacrti").apply { mkdirs() }
            val file = File(dir, "$safeName.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        Toast.makeText(context, "Nacrt spremljen u Slike/SOV Nacrti", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Greška: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun sharePng(context: Context, bitmap: Bitmap, name: String) {
    runCatching {
        val safeName = name.replace(Regex("[^a-zA-Z0-9_čćžšđČĆŽŠĐ -]"), "_")
        val file = File(context.cacheDir, "$safeName.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Nacrt"))
    }.onFailure {
        Toast.makeText(context, "Greška: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}
