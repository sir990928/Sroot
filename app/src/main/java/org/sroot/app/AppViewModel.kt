package org.sroot.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkflowStage(val index: Int) {
    IDLE(-1),
    IDENTIFY(0),
    DOWNLOAD(1),
    START(2),
    COMPLETE(3),
    FAILED(-1),
}

enum class WorkflowNote {
    WAITING,
    IDENTIFYING,
    PREPARING,
    STARTING,
    RUNNING,
    DONE,
    FAILED,
}

data class AppState(
    val snapshot: DeviceSnapshot = DeviceSnapshot.current(),
    val matchedProfile: SupportProfile? = null,
    val manifestError: String? = null,
    val log: String = "",
    val running: Boolean = false,
    val stage: WorkflowStage = WorkflowStage.IDLE,
    val failedAt: WorkflowStage? = null,
    val stageNote: WorkflowNote = WorkflowNote.WAITING,
    val stageError: String? = null,
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
                log = "",
                running = false,
                stage = WorkflowStage.IDLE,
                failedAt = null,
                stageNote = WorkflowNote.WAITING,
                stageError = null,
            )
        }
    }

    fun runDiagnostics() {
        if (_state.value.running) {
            return
        }

        _state.update {
            it.copy(
                running = true,
                log = "",
                stage = WorkflowStage.IDENTIFY,
                failedAt = null,
                stageNote = WorkflowNote.IDENTIFYING,
                stageError = null,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = DeviceSnapshot.current()
            val matchedProfile = manifest.getOrNull()?.match(snapshot)
            _state.update {
                it.copy(
                    snapshot = snapshot,
                    matchedProfile = matchedProfile,
                )
            }
            delay(180)

            if (manifest.isFailure || matchedProfile == null) {
                val error = manifest.exceptionOrNull()?.message
                    ?: "当前设备没有匹配的公开诊断配置"
                failWorkflow(
                    stage = WorkflowStage.IDENTIFY,
                    error = error,
                    log = buildLog(snapshot, matchedProfile, "识别失败：$error"),
                )
                return@launch
            }

            setStage(WorkflowStage.DOWNLOAD, WorkflowNote.PREPARING)
            delay(180)
            setStage(WorkflowStage.START, WorkflowNote.STARTING)
            delay(180)
            setStage(WorkflowStage.START, WorkflowNote.RUNNING)

            val result = runCatching { NativeProbe.run() }
            val log = buildString {
                append(buildLog(snapshot, matchedProfile, ""))
                append(result.getOrElse { "native_probe_error=${it.message}" })
            }
            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        log = log,
                        running = false,
                        stage = WorkflowStage.COMPLETE,
                        failedAt = null,
                        stageNote = WorkflowNote.DONE,
                        stageError = null,
                    )
                }
            } else {
                failWorkflow(
                    stage = WorkflowStage.START,
                    error = result.exceptionOrNull()?.message ?: "Native 诊断失败",
                    log = log,
                )
            }
        }
    }

    private fun setStage(stage: WorkflowStage, note: WorkflowNote) {
        _state.update {
            it.copy(
                stage = stage,
                failedAt = null,
                stageNote = note,
                stageError = null,
            )
        }
    }

    private fun failWorkflow(stage: WorkflowStage, error: String, log: String) {
        _state.update {
            it.copy(
                log = log,
                running = false,
                stage = WorkflowStage.FAILED,
                failedAt = stage,
                stageNote = WorkflowNote.FAILED,
                stageError = error,
            )
        }
    }

    private fun buildLog(
        snapshot: DeviceSnapshot,
        matchedProfile: SupportProfile?,
        status: String,
    ): String = buildString {
        appendLine("Sroot APP 诊断")
        appendLine("model=${snapshot.model}")
        appendLine("fingerprint=${snapshot.fingerprint}")
        appendLine("manifest_profiles=${manifest.getOrNull()?.profiles?.size ?: 0}")
        appendLine("manifest_profile=${matchedProfile?.profileId ?: "none"}")
        if (status.isNotBlank()) {
            appendLine("status=$status")
        }
        appendLine()
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
