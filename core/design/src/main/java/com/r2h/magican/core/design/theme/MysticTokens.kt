package com.r2h.magican.core.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class NeonTokens(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val glow: Color,
    val onAccent: Color,
    val backdropTop: Color,
    val backdropBottom: Color
)

@Immutable
data class GlassTokens(
    val container: Color,
    val border: Color,
    val highlight: Color,
    val fogTint: Color,
    val shadow: Color
)

internal fun neonTokens(darkTheme: Boolean): NeonTokens {
    return if (darkTheme) {
        NeonTokens(
            primary = Color(0xFF00F5D4),
            secondary = Color(0xFF43B0FF),
            tertiary = Color(0xFFFF4D9E),
            glow = Color(0xFF73F7FF),
            onAccent = Color(0xFF03131E),
            backdropTop = Color(0xFF070B16),
            backdropBottom = Color(0xFF101A32)
        )
    } else {
        NeonTokens(
            primary = Color(0xFF00C3A7),
            secondary = Color(0xFF007DE0),
            tertiary = Color(0xFFD52780),
            glow = Color(0xFF29D7EC),
            onAccent = Color(0xFFFFFFFF),
            backdropTop = Color(0xFFEAF4FF),
            backdropBottom = Color(0xFFD9E9FF)
        )
    }
}

internal fun glassTokens(darkTheme: Boolean): GlassTokens {
    return if (darkTheme) {
        GlassTokens(
            container = Color(0x26FFFFFF),
            border = Color(0x55FFFFFF),
            highlight = Color(0x2FFFFFFF),
            fogTint = Color(0x80CFE9FF),
            shadow = Color(0x8A59D3FF)
        )
    } else {
        GlassTokens(
            container = Color(0xB8FFFFFF),
            border = Color(0x85FFFFFF),
            highlight = Color(0x99FFFFFF),
            fogTint = Color(0x66A8CBF7),
            shadow = Color(0x662385FF)
        )
    }
}

val LocalNeonTokens = staticCompositionLocalOf { neonTokens(darkTheme = true) }
val LocalGlassTokens = staticCompositionLocalOf { glassTokens(darkTheme = true) }

@Composable
fun mysticNeonTokens(): NeonTokens = LocalNeonTokens.current

@Composable
fun mysticGlassTokens(): GlassTokens = LocalGlassTokens.current
