package com.darko.speleov1

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darko.speleov1.util.OfflineZapisnikDraft
import com.darko.speleov1.util.OfflineZapisnikTarget

private val objectTypeOptions = listOf(
    "špilja",
    "jama",
    "špilja s jamskim ulazom",
    "jama sa špiljskim ulazom",
    "kompleksni speleološki objekt",
    "špiljski sustav",
    "jamski sustav",
    "sustav",
    "kaverna"
)

private val hydrologyOptions = listOf(
    "suh",
    "nakapnica/prokapnica",
    "povremena stajaća voda",
    "stalna stajaća voda",
    "povremeni tok",
    "stalni tok",
    "povremeno potopljen",
    "stalno potopljen"
)

private val hydrogeologyOptions = listOf(
    "nema",
    "povremeni izvor",
    "stalni izvor",
    "povremeni ponor",
    "stalni ponor",
    "estavela",
    "vrulja",
    "anhijalini objekt",
    "protočan objekt",
    "nepoznato"
)

private val purposeOptions = listOf(
    "Speleološka istraživanja",
    "Praćenje stanja",
    "Edukacija",
    "Other"
)

private val threatsOptions = listOf(
    "nema",
    "nepoznato",
    "eksploatacija mineralnih sirovina",
    "građevinski radovi u objektu",
    "građevinski radovi iznad objekta",
    "iskop sedimenta/sonda",
    "crpljenje vode/bunar",
    "ekološki incident",
    "kanalizacija/septička",
    "komunalni otpad",
    "kemijski otpad",
    "krupni/građevinski otpad",
    "strvine",
    "sakupljanje fosila i minerala (uključujući sige)",
    "nekontrolirano posjećivanje"
)

private val yesNoUnknownOptions = listOf("da", "ne", "nepoznato")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineZapisnikSheet(
    target: OfflineZapisnikTarget,
    onSaveDraft: (OfflineZapisnikDraft) -> Unit,
    onShareDraft: (OfflineZapisnikDraft) -> Unit,
    onOpenOnlineForm: () -> Unit
) {
    val context = LocalContext.current
    var draft by remember(target.storageKey) { mutableStateOf(target.draft) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Offline zapisnik", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Radi offline za ${target.sourceHint.lowercase()}. Spremi draft lokalno pa kasnije shareaj ili prenesi u Google formu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionIconButton(text = "Spremi", icon = Icons.Default.Save, onClick = {
                        onSaveDraft(draft)
                        Toast.makeText(context, "Draft spremljen offline", Toast.LENGTH_SHORT).show()
                    })
                    ActionIconButton(text = "Share", icon = Icons.Default.Share, onClick = { onShareDraft(draft) })
                    ActionIconButton(text = "Google forma", icon = Icons.Default.OpenInNew, onClick = onOpenOnlineForm)
                }
            }
        }

        OfflineZapisnikSectionCard("Osnovno") {
            OutlinedTextField(value = draft.email, onValueChange = { draft = draft.copy(email = it) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = draft.objectName, onValueChange = { draft = draft.copy(objectName = it) }, label = { Text("Ime speleološkog objekta") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = draft.plateNumber, onValueChange = { draft = draft.copy(plateNumber = it) }, label = { Text("Broj pločice na objektu") }, modifier = Modifier.fillMaxWidth())
            SingleChoiceChips(label = "Vrsta objekta", selected = draft.objectType, options = objectTypeOptions, onSelected = { draft = draft.copy(objectType = it) })
            MultiChoiceChips(label = "Hidrološke karakteristike", selected = draft.hydrology, options = hydrologyOptions, onSelected = { draft = draft.copy(hydrology = it) })
            MultiChoiceChips(label = "Hidrogeološke pojave", selected = draft.hydrogeology, options = hydrogeologyOptions, onSelected = { draft = draft.copy(hydrogeology = it) })
        }

        OfflineZapisnikSectionCard("Lokacija i koordinate") {
            OutlinedTextField(value = draft.nearestPlace, onValueChange = { draft = draft.copy(nearestPlace = it) }, label = { Text("Najbliže mjesto") }, modifier = Modifier.fillMaxWidth())
            SingleChoiceChips(label = "Koordinatni sustav", selected = draft.coordinateSystem, options = listOf("HTRS96/TM", "WGS84"), onSelected = { draft = draft.copy(coordinateSystem = it) })
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = draft.xCoordinate, onValueChange = { draft = draft.copy(xCoordinate = it) }, label = { Text("X koordinata") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = draft.yCoordinate, onValueChange = { draft = draft.copy(yCoordinate = it) }, label = { Text("Y koordinata") }, modifier = Modifier.fillMaxWidth())
            }
        }

        OfflineZapisnikSectionCard("Aktivnost") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = draft.startDate, onValueChange = { draft = draft.copy(startDate = it) }, label = { Text("Datum početka") }, placeholder = { Text("DD/MM/GGGG") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = draft.endDate, onValueChange = { draft = draft.copy(endDate = it) }, label = { Text("Datum završetka") }, placeholder = { Text("DD/MM/GGGG") }, modifier = Modifier.fillMaxWidth())
            }
            SingleChoiceChips(label = "Svrha aktivnosti", selected = draft.activityPurpose, options = purposeOptions, onSelected = { draft = draft.copy(activityPurpose = it) })
            if (draft.activityPurpose == "Other") {
                OutlinedTextField(value = draft.activityPurposeOther, onValueChange = { draft = draft.copy(activityPurposeOther = it) }, label = { Text("Druga svrha") }, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(value = draft.activityDescription, onValueChange = { draft = draft.copy(activityDescription = it) }, label = { Text("Opis aktivnosti") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(value = draft.executionMethod, onValueChange = { draft = draft.copy(executionMethod = it) }, label = { Text("Način izvođenja aktivnosti") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            MultiChoiceChips(label = "Primjećene ugroze objekta", selected = draft.threats, options = threatsOptions, onSelected = { draft = draft.copy(threats = it) })
        }

        OfflineZapisnikSectionCard("Fauna i nalazi") {
            SingleChoiceChips(label = "Kolonija šišmiša", selected = draft.batColony, options = yesNoUnknownOptions, onSelected = { draft = draft.copy(batColony = it) })
            SingleChoiceChips(label = "Špiljski školjkaš", selected = draft.caveBivalve, options = yesNoUnknownOptions, onSelected = { draft = draft.copy(caveBivalve = it) })
            SingleChoiceChips(label = "Špiljska spužva", selected = draft.caveSponge, options = yesNoUnknownOptions, onSelected = { draft = draft.copy(caveSponge = it) })
            SingleChoiceChips(label = "Čovječja ribica", selected = draft.olm, options = yesNoUnknownOptions, onSelected = { draft = draft.copy(olm = it) })
            SingleChoiceChips(label = "Fosilni nalazi", selected = draft.fossilFinds, options = yesNoUnknownOptions, onSelected = { draft = draft.copy(fossilFinds = it) })
        }

        OfflineZapisnikSectionCard("Ljudi") {
            OutlinedTextField(value = draft.teamMembers, onValueChange = { draft = draft.copy(teamMembers = it) }, label = { Text("Članovi istraživanja") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceChips(
    label: String,
    selected: List<String>,
    options: List<String>,
    onSelected: (List<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected.contains(option),
                    onClick = {
                        onSelected(if (selected.contains(option)) selected - option else selected + option)
                    },
                    label = { Text(option) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleChoiceChips(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}


@Composable
private fun OfflineZapisnikSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title)
            content()
        }
    }
}
