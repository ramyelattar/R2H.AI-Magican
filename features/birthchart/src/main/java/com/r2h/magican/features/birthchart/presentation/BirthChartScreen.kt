package com.r2h.magican.features.birthchart.presentation

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
fun BirthChartScreen(viewModel: BirthChartViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MysticScaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard {
                Text(
                    "Birth Chart",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    state.status,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                OutlinedTextField(
                    value = state.birthDateIso,
                    onValueChange = viewModel::onBirthDateChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Birth date (YYYY-MM-DD)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.birthTime24h,
                    onValueChange = viewModel::onBirthTimeChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Birth time (HH:MM)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.birthPlace,
                    onValueChange = viewModel::onBirthPlaceChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Birth place") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.focusArea,
                    onValueChange = viewModel::onFocusChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Focus area (optional)") },
                    singleLine = true
                )
                NeonButton(
                    text = "Generate Reading",
                    onClick = viewModel::generate,
                    isLoading = state.isLoading,
                    loadingText = "Generating…",
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
