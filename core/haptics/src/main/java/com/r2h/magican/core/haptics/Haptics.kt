package com.r2h.magican.core.haptics

enum class HapticPatternType {
    Selection,
    Success,
    Warning,
    Error,
    MysticPulse
}

interface Haptics {
    fun tick()
    fun confirm()
    fun pattern(type: HapticPatternType)
}

object NoOpHaptics : Haptics {
    override fun tick() = Unit
    override fun confirm() = Unit
    override fun pattern(type: HapticPatternType) = Unit
}
