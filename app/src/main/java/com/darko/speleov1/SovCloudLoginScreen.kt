package com.darko.speleov1

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darko.speleov1.util.SovAppPermissions
import com.darko.speleov1.util.SovAuthSession
import com.darko.speleov1.util.SovPermissionsStore
import com.darko.speleov1.util.SovRoleSyncManager
import com.darko.speleov1.util.SovSupabaseRoleClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SovCloudLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit
) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf(SovPermissionsStore.loadSession(context)) }
    var permissions by remember { mutableStateOf(SovPermissionsStore.loadPermissions(context)) }
    var email by rememberSaveable { mutableStateOf(session.email.ifBlank { permissions.email }) }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun signIn() {
        if (email.isBlank() || password.isBlank()) {
            message = language.pick("Upiši email i lozinku.", "Enter email and password.")
            return
        }
        scope.launch {
            busy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val signedSession = SovSupabaseRoleClient.signInWithPassword(email.trim(), password)
                    val fetchedPermissions = SovSupabaseRoleClient.fetchCurrentPermissions(signedSession.accessToken)
                    signedSession to fetchedPermissions
                }
            }.onSuccess { (signedSession, fetchedPermissions) ->
                password = ""
                val savedPermissions = fetchedPermissions.copy(
                    expiresAtMillis = signedSession.expiresAtMillis,
                    lastSyncOk = true,
                    lastSyncError = ""
                )
                SovPermissionsStore.saveSession(context, signedSession)
                SovPermissionsStore.savePermissions(context, savedPermissions)
                session = signedSession
                permissions = savedPermissions
                message = language.pick("Prijavljen si kao ${savedPermissions.roleLabel}.", "Signed in as ${savedPermissions.roleLabel}.")
                Toast.makeText(context, language.pick("SOV Cloud prijava spremljena", "SOV Cloud sign-in saved"), Toast.LENGTH_SHORT).show()
                onLoggedIn()
            }.onFailure { throwable ->
                message = throwable.message ?: language.pick("Prijava nije uspjela.", "Sign-in failed.")
            }
            busy = false
        }
    }

    fun signOut() {
        SovPermissionsStore.clear(context)
        session = SovAuthSession()
        permissions = SovAppPermissions()
        password = ""
        message = language.pick("Odjavljen si. App će koristiti offline cache dok se opet ne prijaviš.", "Signed out. The app will use offline cache until you sign in again.")
    }

    LaunchedEffect(Unit) {
        if (session.isLoggedIn) {
            runCatching { withContext(Dispatchers.IO) { SovRoleSyncManager.syncNow(context, forceNetwork = false) } }
                .onSuccess { state ->
                    session = state.session
                    permissions = state.permissions
                    if (state.usedCachedPermissions && state.message.isNotBlank()) message = state.message
                }
        }
    }

    CaveScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(46.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = language.pick("Natrag", "Back"), tint = Color.White)
                    }
                }
                Text(
                    language.pick("SOV Cloud prijava", "SOV Cloud sign-in"),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(46.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF162033).copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .background(Color(0xFF1B5E20).copy(alpha = 0.16f), RoundedCornerShape(34.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = language.pick("Ikona prijave korisnika", "User sign-in icon"),
                            tint = Color(0xFF8BE9B5),
                            modifier = Modifier.size(82.dp)
                        )
                    }
                    Text(
                        text = if (session.isLoggedIn) language.pick("Prijava je spremljena", "Sign-in is saved") else language.pick("Prijava za SOV Cloud", "SOV Cloud sign-in"),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = language.pick(
                            if (session.isLoggedIn) "Pritisni Nastavi i otvaram Cloud." else "Prijavi se jednom. Nakon prijave automatski otvaram Cloud.",
                            if (session.isLoggedIn) "Tap Continue and I will open Cloud." else "Sign in once. After sign-in I will open Cloud automatically."
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    if (session.isLoggedIn) {
                        Surface(
                            color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(28.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(permissions.roleLabel, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text(session.email.ifBlank { permissions.email.ifBlank { permissions.status } }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    Text(language.pick("Zadnji sync: ${SovPermissionsStore.lastSyncLabel(context)}", "Last sync: ${SovPermissionsStore.lastSyncLabel(context)}"), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Button(
                            onClick = onLoggedIn,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(language.pick("Nastavi u Cloud", "Continue to Cloud"))
                        }
                        OutlinedButton(
                            onClick = { signOut() },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(language.pick("Odjavi ovaj uređaj", "Sign out this device"), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                            label = { Text("Email") }
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            label = { Text(language.pick("Lozinka", "Password")) }
                        )
                        Button(
                            onClick = { signIn() },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 15.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(language.pick("Prijavi me i otvori Cloud", "Sign in and open Cloud"), fontWeight = FontWeight.Bold)
                        }
                    }

                    message?.let {
                        Surface(
                            color = Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Text(
                text = language.pick(
                    "Prijava se sprema lokalno na uređaj. Nema ručnog synca u Postavkama — aplikacija sama osvježava prava.",
                    "Sign-in is saved locally on this device. There is no manual sync in Settings — the app refreshes permissions automatically."
                ),
                color = Color.White.copy(alpha = 0.70f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}
