package com.r2h.magican.features.compatibility.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun CompatibilityScreen(viewModel: CompatibilityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MysticScaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard {
                Text(
                    "Compatibility",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    state.status,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                OutlinedTextField(
                    value = state.personA,
                    onValueChange = viewModel::onPersonAChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Person A") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.personB,
                    onValueChange = viewModel::onPersonBChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Person B") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.context,
                    onValueChange = viewModel::onContextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Context (friends, partners, colleagues)") },
                    minLines = 2
                )
                NeonButton(
                    text = "Analyze Match",
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
