package com.darko.speleov1

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Jedan ulaz za poruke koje moraju ostati čitljive na terenu.
 * Toast ostaje za sitne potvrde, a greške idu kroz dugi Snackbar.
 */
class SovMessenger(
    private val snackbarHostState: SnackbarHostState?,
    private val scope: CoroutineScope?
) {
    fun error(message: String, retryAction: (() -> Unit)? = null) {
        val clean = message.ifBlank { "Greška" }
        val host = snackbarHostState ?: return
        val launcher = scope ?: return
        launcher.launch {
            val result = host.showSnackbar(
                message = clean,
                actionLabel = if (retryAction != null) "Pokušaj ponovno" else null,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                retryAction?.invoke()
            }
        }
    }

    fun success(message: String) {
        val clean = message.ifBlank { "Gotovo" }
        val host = snackbarHostState ?: return
        val launcher = scope ?: return
        launcher.launch {
            host.showSnackbar(clean, duration = SnackbarDuration.Short)
        }
    }

    companion object {
        val Noop = SovMessenger(null, null)
    }
}

val LocalSovMessenger = staticCompositionLocalOf { SovMessenger.Noop }
