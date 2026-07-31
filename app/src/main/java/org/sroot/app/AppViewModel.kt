package org.sroot.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppState(
    val snapshot: DeviceSnapshot = DeviceSnapshot.current(),
    val matchedProfile: SupportProfile? = null,
    val manifestError: String? = null,
    val log: String = "",
    val running: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val manifest = runCatching { SupportManifest.load(application) }
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun refreshSnapshot() {
        val snapshot = DeviceSnapshot.current()
        _state.update {
            it.copy(
                snapshot = snapshot,
                matchedProfile = manifest.getOrNull()?.match(snapshot),
            )
        }
    }

    fun runDiagnostics() {
        if (_state.value.running) {
            return
        }

        _state.update { it.copy(running = true, log = "") }
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = DeviceSnapshot.current()
            val matchedProfile = manifest.getOrNull()?.match(snapshot)
            val result = runCatching { NativeProbe.run() }
            val log = buildString {
                appendLine("Sroot APP diagnostic")
                appendLine("model=${snapshot.model}")
                appendLine("fingerprint=${snapshot.fingerprint}")
                appendLine("manifest_profile=${matchedProfile?.profileId ?: "none"}")
                appendLine()
                append(result.getOrElse { "native_probe_error=${it.message}" })
            }
            _state.update {
                it.copy(
                    snapshot = snapshot,
                    matchedProfile = matchedProfile,
                    log = log,
                    running = false,
                )
            }
        }
    }

    private fun initialState(): AppState {
        val snapshot = DeviceSnapshot.current()
        return AppState(
            snapshot = snapshot,
            matchedProfile = manifest.getOrNull()?.match(snapshot),
            manifestError = manifest.exceptionOrNull()?.message,
        )
    }
}
