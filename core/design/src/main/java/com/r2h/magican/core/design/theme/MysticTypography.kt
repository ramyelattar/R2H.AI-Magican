package com.r2h.magican.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import java.util.Locale

enum class MysticLanguage {
    Auto,
    English,
    Arabic
}

@Immutable
data class MysticTypographyTokens(
    val language: MysticLanguage,
    val material: Typography
)

@Composable
fun rememberMysticTypography(language: MysticLanguage = MysticLanguage.Auto): MysticTypographyTokens {
    val resolved = resolveLanguage(language)
    return remember(resolved) {
        MysticTypographyTokens(
            language = resolved,
            material = when (resolved) {
                MysticLanguage.Arabic -> arabicTypography()
                MysticLanguage.English -> englishTypography()
                MysticLanguage.Auto -> englishTypography()
            }
        )
    }
}

private fun resolveLanguage(language: MysticLanguage): MysticLanguage {
    if (language != MysticLanguage.Auto) return language
    val current = Locale.getDefault().language.lowercase(Locale.ROOT)
    return if (current.startsWith("ar")) MysticLanguage.Arabic else MysticLanguage.English
}

private fun englishTypography(): Typography {
    val heading = TextStyle(
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.1.sp,
        lineHeight = 34.sp
    )
    val body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp
    )
    return Typography(
        displayLarge = heading.copy(fontSize = 48.sp, lineHeight = 54.sp),
        headlineLarge = heading.copy(fontSize = 32.sp),
        titleLarge = heading.copy(fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = heading.copy(fontSize = 18.sp, lineHeight = 24.sp),
        bodyLarge = body.copy(fontSize = 16.sp),
        bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = body.copy(fontSize = 14.sp, lineHeight = 18.sp)
    )
}

private fun arabicTypography(): Typography {
    val heading = TextStyle(
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.sp,
        lineHeight = 40.sp,
        textDirection = TextDirection.ContentOrRtl
    )
    val body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.sp,
        lineHeight = 28.sp,
        textDirection = TextDirection.ContentOrRtl
    )
    return Typography(
        displayLarge = heading.copy(fontSize = 50.sp, lineHeight = 58.sp),
        headlineLarge = heading.copy(fontSize = 34.sp),
        titleLarge = heading.copy(fontSize = 24.sp, lineHeight = 32.sp),
        titleMedium = heading.copy(fontSize = 20.sp, lineHeight = 28.sp),
        bodyLarge = body.copy(fontSize = 18.sp),
        bodyMedium = body.copy(fontSize = 16.sp, lineHeight = 24.sp),
        labelLarge = body.copy(fontSize = 15.sp, lineHeight = 20.sp)
    )
}
