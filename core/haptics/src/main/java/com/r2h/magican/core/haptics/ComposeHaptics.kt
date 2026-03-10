package com.r2h.magican.core.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

val LocalAppHaptics = staticCompositionLocalOf<Haptics> { NoOpHaptics }

@Composable
fun ProvideAppHaptics(
    haptics: Haptics,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppHaptics provides haptics, content = content)
}

@Composable
fun rememberHaptics(
    delegate: Haptics = LocalAppHaptics.current
): Haptics {
    val composeHaptics = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(delegate, composeHaptics, view) {
        ComposeAwareHaptics(delegate, composeHaptics, view)
    }
}

@Stable
private class ComposeAwareHaptics(
    private val delegate: Haptics,
    private val compose: HapticFeedback,
    private val view: View
) : Haptics {

    override fun tick() {
        if (delegate !== NoOpHaptics) {
            delegate.tick()
            return
        }
        compose.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    override fun confirm() {
        if (delegate !== NoOpHaptics) {
            delegate.confirm()
            return
        }
        compose.performHapticFeedback(HapticFeedbackType.LongPress)
        view.performHapticFeedback(confirmConstant())
    }

    override fun pattern(type: HapticPatternType) {
        if (delegate !== NoOpHaptics) {
            delegate.pattern(type)
            return
        }

        when (type) {
            HapticPatternType.Selection -> {
                compose.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }

            HapticPatternType.Success -> {
                compose.performHapticFeedback(HapticFeedbackType.LongPress)
                view.performHapticFeedback(confirmConstant())
            }

            HapticPatternType.Warning -> {
                compose.performHapticFeedback(HapticFeedbackType.LongPress)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }

            HapticPatternType.Error -> {
                compose.performHapticFeedback(HapticFeedbackType.LongPress)
                view.performHapticFeedback(rejectConstant())
            }

            HapticPatternType.MysticPulse -> {
                compose.performHapticFeedback(HapticFeedbackType.LongPress)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    private fun confirmConstant(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
    }

    private fun rejectConstant(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
    }
}
