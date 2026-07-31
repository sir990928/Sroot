package org.sroot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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
        topBar = { TopAppBar(title = { Text("Sroot APP") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                TargetCard(state.snapshot, state.matchedProfile, state.manifestError)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::refreshSnapshot,
                    ) {
                        Text("Refresh")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !state.running,
                        onClick = viewModel::runDiagnostics,
                    ) {
                        if (state.running) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            Text("Run diagnostics")
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Native probe", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.log.ifBlank { "No diagnostic run yet." },
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TargetCard(
    snapshot: DeviceSnapshot,
    matchedProfile: SupportProfile?,
    manifestError: String?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Device context", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            DeviceLine("Model", snapshot.model)
            DeviceLine("Device", snapshot.device)
            DeviceLine("Android", "${snapshot.androidRelease} (SDK ${snapshot.sdk})")
            DeviceLine("Kernel", snapshot.kernelRelease)
            DeviceLine("ABI", snapshot.abi)
            DeviceLine("Page size", snapshot.pageSize.toString())
            DeviceLine("Support profile", matchedProfile?.displayName ?: "Unmatched")
            DeviceLine("Profile mode", matchedProfile?.mode ?: "none")
            if (manifestError != null) {
                DeviceLine("Manifest", "error: $manifestError")
            }
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
