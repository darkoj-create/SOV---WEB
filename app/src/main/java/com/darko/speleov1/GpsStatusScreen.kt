package com.darko.speleov1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GpsStatusScreen(onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Status") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { inner ->
        CaveScreenBackground {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(inner)
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingsGpsFieldStatusContent(language = language, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
