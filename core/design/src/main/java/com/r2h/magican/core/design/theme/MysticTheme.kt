package com.r2h.magican.core.design.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val MysticDarkColors = darkColorScheme(
    primary = Color(0xFF00F5D4),
    secondary = Color(0xFF43B0FF),
    tertiary = Color(0xFFFF4D9E),
    background = Color(0xFF070B16),
    surface = Color(0xFF101A2C),
    onPrimary = Color(0xFF03131E),
    onSecondary = Color(0xFF03131E),
    onTertiary = Color(0xFF2A001A),
    onBackground = Color(0xFFEAF4FF),
    onSurface = Color(0xFFEAF4FF)
)

private val MysticLightColors = lightColorScheme(
    primary = Color(0xFF00C3A7),
    secondary = Color(0xFF007DE0),
    tertiary = Color(0xFFD52780),
    background = Color(0xFFEAF4FF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0A1730),
    onSurface = Color(0xFF0A1730)
)

@Composable
fun MysticTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    language: MysticLanguage = MysticLanguage.Auto,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val typographyTokens = rememberMysticTypography(language)
    val neon = neonTokens(darkTheme)
    val glass = glassTokens(darkTheme)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> MysticDarkColors
        else -> MysticLightColors
    }

    CompositionLocalProvider(
        LocalNeonTokens provides neon,
        LocalGlassTokens provides glass
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typographyTokens.material,
            content = content
        )
    }
}
