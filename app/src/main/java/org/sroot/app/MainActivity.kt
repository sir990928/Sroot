package org.sroot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector

private val workflowSteps = listOf(
    WorkflowStage.IDENTIFY,
    WorkflowStage.DOWNLOAD,
    WorkflowStage.START,
    WorkflowStage.COMPLETE,
)

class MainActivity : ComponentActivity() {
    private val activityViewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SrootApp(activityViewModel)
            }
        }
    }
}

@Composable
private fun SrootApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.34f)
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                    ) {
                        TargetCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            snapshot = state.snapshot,
                            matchedProfile = state.matchedProfile,
                            manifestError = state.manifestError,
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.running,
                            onClick = viewModel::refreshSnapshot,
                        ) {
                            Text(stringResource(R.string.refresh))
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        WorkflowCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            stage = state.stage,
                            failedAt = state.failedAt,
                            note = state.stageNote,
                            error = state.stageError,
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.running && state.matchedProfile != null,
                            onClick = viewModel::runDiagnostics,
                        ) {
                            if (state.running) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.run_diagnostics))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.66f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 0.dp,
                ),
            ) {
                item {
                    LogCard(state.log)
                }
            }
        }
    }
}

@Composable
private fun TargetCard(
    modifier: Modifier,
    snapshot: DeviceSnapshot,
    matchedProfile: SupportProfile?,
    manifestError: String?,
) {
    Card(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp),
        ) {
            Text(
                stringResource(R.string.device_context),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            DeviceLine(
                stringResource(R.string.model),
                "${snapshot.model} / ${snapshot.device}",
            )
            DeviceLine(
                stringResource(R.string.android),
                stringResource(
                    R.string.android_version,
                    snapshot.androidRelease,
                    snapshot.sdk,
                ),
            )
            DeviceLine(
                stringResource(R.string.support_profile),
                matchedProfile?.profileId
                    ?: stringResource(R.string.profile_unmatched),
            )
            DeviceLine(
                stringResource(R.string.selection_method),
                stringResource(R.string.profile_auto),
            )
            DeviceLine(
                stringResource(R.string.profile_mode),
                matchedProfile?.mode ?: stringResource(R.string.profile_none),
            )
            Text(
                text = matchedProfile?.displayName
                    ?: stringResource(R.string.profile_unmatched),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
            if (manifestError != null) {
                DeviceLine(
                    stringResource(R.string.manifest),
                    stringResource(R.string.manifest_error, manifestError),
                )
            }
        }
    }
}

@Composable
private fun WorkflowCard(
    modifier: Modifier,
    stage: WorkflowStage,
    failedAt: WorkflowStage?,
    note: WorkflowNote,
    error: String?,
) {
    val activeIndex = when {
        stage == WorkflowStage.COMPLETE -> workflowSteps.lastIndex
        stage == WorkflowStage.FAILED -> failedAt?.index ?: 0
        stage == WorkflowStage.IDLE -> -1
        else -> stage.index
    }
    val title = if (stage == WorkflowStage.FAILED) {
        stringResource(R.string.workflow_failed)
    } else if (stage == WorkflowStage.IDLE) {
        stringResource(R.string.workflow_waiting)
    } else {
        stringResource(stageLabel(stage))
    }
    val detail = if (stage == WorkflowStage.FAILED && error != null) {
        stringResource(R.string.workflow_failed_detail, error)
    } else {
        stringResource(noteLabel(note))
    }

    Card(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.workflow_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (stage == WorkflowStage.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                workflowSteps.forEach { step ->
                    StageStep(
                        modifier = Modifier.fillMaxWidth(),
                        step = step,
                        activeIndex = activeIndex,
                        failed = stage == WorkflowStage.FAILED && failedAt == step,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                detail,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun StageStep(
    modifier: Modifier,
    step: WorkflowStage,
    activeIndex: Int,
    failed: Boolean,
) {
    val completed = activeIndex >= step.index && !failed
    val active = activeIndex == step.index && !completed && !failed
    val color = when {
        failed -> MaterialTheme.colorScheme.error
        completed -> MaterialTheme.colorScheme.primary
        active -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    failed -> Icons.Filled.Close
                    completed -> Icons.Filled.Check
                    else -> stageIcon(step)
                },
                contentDescription = stringResource(stageLabel(step)),
                tint = if (completed || active || failed) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = stringResource(stageLabel(step)),
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun LogCard(log: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.native_probe),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                text = log.ifBlank {
                    stringResource(R.string.no_diagnostic)
                },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DeviceLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.weight(0.35f),
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            modifier = Modifier.weight(0.65f),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun stageLabel(stage: WorkflowStage): Int = when (stage) {
    WorkflowStage.IDENTIFY -> R.string.stage_identify
    WorkflowStage.DOWNLOAD -> R.string.stage_download
    WorkflowStage.START -> R.string.stage_start
    WorkflowStage.COMPLETE -> R.string.stage_complete
    else -> R.string.workflow_waiting
}

private fun stageIcon(stage: WorkflowStage): ImageVector = when (stage) {
    WorkflowStage.IDENTIFY -> Icons.Filled.Search
    WorkflowStage.DOWNLOAD -> Icons.Filled.FileDownload
    WorkflowStage.START -> Icons.Filled.PlayArrow
    WorkflowStage.COMPLETE -> Icons.Filled.Check
    else -> Icons.Filled.Search
}

private fun noteLabel(note: WorkflowNote): Int = when (note) {
    WorkflowNote.WAITING -> R.string.workflow_note_waiting
    WorkflowNote.IDENTIFYING -> R.string.workflow_note_identifying
    WorkflowNote.PREPARING -> R.string.workflow_note_preparing
    WorkflowNote.STARTING -> R.string.workflow_note_starting
    WorkflowNote.RUNNING -> R.string.workflow_note_running
    WorkflowNote.DONE -> R.string.workflow_note_done
    WorkflowNote.FAILED -> R.string.workflow_note_failed
}
