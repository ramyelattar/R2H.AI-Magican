package com.r2h.magican

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.r2h.magican.core.design.components.GlassCard
import com.r2h.magican.core.design.components.MysticScaffold
import com.r2h.magican.core.design.components.NeonButton
import com.r2h.magican.core.design.theme.MysticLanguage
import com.r2h.magican.core.design.theme.MysticTheme
import com.r2h.magican.features.birthchart.presentation.BirthChartScreen
import com.r2h.magican.features.compatibility.presentation.CompatibilityScreen
import com.r2h.magican.features.dreams.presentation.DreamsScreen
import com.r2h.magican.features.horoscope.presentation.HoroscopeScreen
import com.r2h.magican.features.library.presentation.LibraryScreen
import com.r2h.magican.features.palm.presentation.PalmScreen
import com.r2h.magican.features.tarot.presentation.TarotScreen
import com.r2h.magican.features.voiceaura.presentation.VoiceAuraScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appPreferences.themeModeValue
                .collectAsStateWithLifecycle(initialValue = AppPreferences.ThemeModeValue.System)
            val darkTheme = when (themeMode) {
                AppPreferences.ThemeModeValue.System -> isSystemInDarkTheme()
                AppPreferences.ThemeModeValue.Dark   -> true
                AppPreferences.ThemeModeValue.Light  -> false
            }

            MysticTheme(
                darkTheme = darkTheme,
                dynamicColor = true,
                language = MysticLanguage.Auto
            ) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        lifecycleScope.launch { appPreferences.setThemeMode(newMode) }
                    }
                )
            }
        }
    }
}

private data class FeatureRoute(
    val route: String,
    val title: String,
    val description: String
)

private val HomeRoutes = listOf(
    FeatureRoute("tarot", "Tarot", "Draw cards, flip reveals, and receive guided interpretation."),
    FeatureRoute("palm", "Palm", "Analyze hand observations and reveal practical insights."),
    FeatureRoute("voice", "Voice Aura", "Record voice energy and generate aura-based interpretation."),
    FeatureRoute("dreams", "Dreams", "Interpret dream narratives with mood-aware context."),
    FeatureRoute("horoscope", "Horoscope", "Generate daily cosmic guidance with focus areas."),
    FeatureRoute("compatibility", "Compatibility", "Compare two people and surface harmony actions."),
    FeatureRoute("birthchart", "Birth Chart", "Get chart-based readings from date, time, and location."),
    FeatureRoute("library", "Library", "Import PDFs, bookmark pages, summarize, and search quickly.")
)

@Composable
private fun AppNavHost(
    navController: NavHostController,
    themeMode: AppPreferences.ThemeModeValue,
    onThemeModeChange: (AppPreferences.ThemeModeValue) -> Unit
) {
    var lastRoute by rememberSaveable { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                lastRouteTitle = HomeRoutes.firstOrNull { it.route == lastRoute }?.title,
                onOpenLast = {
                    val route = lastRoute ?: return@HomeScreen
                    navController.navigate(route)
                },
                onOpen = { route ->
                    lastRoute = route
                    navController.navigate(route)
                }
            )
        }
        composable("tarot") { FeatureContainer("Tarot", onBack = navController::navigateUp) { TarotScreen() } }
        composable("palm") { FeatureContainer("Palm", onBack = navController::navigateUp) { PalmScreen() } }
        composable("voice") { FeatureContainer("Voice Aura", onBack = navController::navigateUp) { VoiceAuraScreen() } }
        composable("dreams") { FeatureContainer("Dreams", onBack = navController::navigateUp) { DreamsScreen() } }
        composable("horoscope") { FeatureContainer("Horoscope", onBack = navController::navigateUp) { HoroscopeScreen() } }
        composable("compatibility") { FeatureContainer("Compatibility", onBack = navController::navigateUp) { CompatibilityScreen() } }
        composable("birthchart") { FeatureContainer("Birth Chart", onBack = navController::navigateUp) { BirthChartScreen() } }
        composable("library") { FeatureContainer("Library", onBack = navController::navigateUp) { LibraryScreen() } }
    }
}

@Composable
private fun HomeScreen(
    themeMode: AppPreferences.ThemeModeValue,
    onThemeModeChange: (AppPreferences.ThemeModeValue) -> Unit,
    lastRouteTitle: String?,
    onOpenLast: () -> Unit,
    onOpen: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredRoutes = remember(query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) HomeRoutes
        else HomeRoutes.filter {
            it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    MysticScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Magican",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Choose your ritual. Search quickly, continue where you left off, and personalize theme mode.",
                style = MaterialTheme.typography.bodyMedium
            )

            GlassCard {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonButton(
                        text = "Theme: System",
                        onClick = { onThemeModeChange(AppPreferences.ThemeModeValue.System) },
                        enabled = themeMode != AppPreferences.ThemeModeValue.System,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NeonButton(
                        text = "Theme: Dark",
                        onClick = { onThemeModeChange(AppPreferences.ThemeModeValue.Dark) },
                        enabled = themeMode != AppPreferences.ThemeModeValue.Dark,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NeonButton(
                        text = "Theme: Light",
                        onClick = { onThemeModeChange(AppPreferences.ThemeModeValue.Light) },
                        enabled = themeMode != AppPreferences.ThemeModeValue.Light,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (lastRouteTitle != null) {
                GlassCard {
                    Text("Continue", style = MaterialTheme.typography.titleMedium)
                    Text("Resume $lastRouteTitle", style = MaterialTheme.typography.bodyMedium)
                    NeonButton(
                        text = "Open Last Feature",
                        onClick = onOpenLast,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search features") },
                singleLine = true
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRoutes, key = { it.route }) { item ->
                    GlassCard {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.description, style = MaterialTheme.typography.bodyMedium)
                        NeonButton(
                            text = "Open ${item.title}",
                            onClick = { onOpen(item.route) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureContainer(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                NeonButton(
                    text = "Back to Home",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
