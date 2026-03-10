package com.r2h.magican.features.palm.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.r2h.magican.core.design.components.GlassCard
import com.r2h.magican.core.design.components.MysticScaffold
import com.r2h.magican.core.design.components.NeonButton

@Composable
fun PalmScreen(viewModel: PalmViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MysticScaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard {
                Text(
                    "Palm Reading",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    state.status,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonButton(
                        text = "Left",
                        onClick = { viewModel.onHandChanged("left") },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "Right",
                        onClick = { viewModel.onHandChanged("right") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = state.observations,
                    onValueChange = viewModel::onObservationsChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Observed lines, mounts, shape") },
                    minLines = 4
                )
                NeonButton(
                    text = "Analyze Palm",
                    onClick = viewModel::analyze,
                    isLoading = state.isLoading,
                    loadingText = "Analyzing…",
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                state.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }

            if (state.summary.isNotBlank()) {
                GlassCard {
                    Text(state.summary, style = MaterialTheme.typography.titleMedium)
                    state.insights.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
                    state.actions.forEach { Text("Action: $it", style = MaterialTheme.typography.bodyMedium) }
                    Text(state.disclaimer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
