package com.example.data.model

data class HamzaAiConfig(
    val tone: String = "Friendly & Warm", // "Friendly & Warm", "Professional & Formal", "Concise & Direct", "Creative & Witty", "Empathetic & Gentle"
    val responseLength: String = "Balanced", // "Concise", "Balanced", "Detailed"
    val emojiLevel: String = "Moderate", // "None", "Moderate", "Expressive"
    val defaultLanguage: String = "English", // "English", "हिन्दी (Hindi)", "اردو (Urdu)", "Auto-detect"
    val smartPolishingStyle: String = "Casual & Friendly", // "Casual & Friendly", "Executive & Sharp", "Persuasive", "Academic"
    val customInstructions: String = "",
    val grammarExplanation: Boolean = true
)
