package com.r2h.magican.features.tarot.domain

data class TarotCard(
    val id: Int,
    val name: String,
    val upright: String,
    val reversed: String
)

object TarotDeckFactory {
    fun majorArcana(): List<TarotCard> = listOf(
        TarotCard(0, "The Fool", "New beginnings and bold trust.", "Recklessness and delayed starts."),
        TarotCard(1, "The Magician", "Focused will and manifestation.", "Scattered power and illusion."),
        TarotCard(2, "The High Priestess", "Intuition and inner knowing.", "Hidden signals and confusion."),
        TarotCard(3, "The Empress", "Abundance and nurturing creation.", "Overgiving and stagnation."),
        TarotCard(4, "The Emperor", "Structure and stable leadership.", "Rigidity and control issues."),
        TarotCard(5, "The Hierophant", "Tradition and shared wisdom.", "Dogma and resistance."),
        TarotCard(6, "The Lovers", "Alignment and heart-led choices.", "Mismatched values."),
        TarotCard(7, "The Chariot", "Momentum and disciplined victory.", "Direction loss and friction."),
        TarotCard(8, "Strength", "Calm courage and compassion.", "Self-doubt and reactivity."),
        TarotCard(9, "The Hermit", "Reflection and inner guidance.", "Isolation and avoidance."),
        TarotCard(10, "Wheel of Fortune", "Cycles turning in your favor.", "Disruption and unpredictability."),
        TarotCard(11, "Justice", "Balance, truth, accountability.", "Bias and unclear outcomes."),
        TarotCard(12, "The Hanged Man", "New perspective through pause.", "Stalling and indecision."),
        TarotCard(13, "Death", "Transformation and release.", "Clinging to old patterns."),
        TarotCard(14, "Temperance", "Harmony and wise pacing.", "Excess and imbalance."),
        TarotCard(15, "The Devil", "Attachment and temptation exposed.", "Breaking unhealthy bonds."),
        TarotCard(16, "The Tower", "Sudden clarity and reset.", "Avoided truth prolonging stress."),
        TarotCard(17, "The Star", "Hope, healing, renewed faith.", "Discouragement and doubt."),
        TarotCard(18, "The Moon", "Dreamwork and subtle insight.", "Fear projections."),
        TarotCard(19, "The Sun", "Joy, vitality, visibility.", "Burnout or overexposure."),
        TarotCard(20, "Judgement", "Awakening and decisive calling.", "Self-criticism and delay."),
        TarotCard(21, "The World", "Completion and integration.", "Unfinished cycles.")
    )
}
