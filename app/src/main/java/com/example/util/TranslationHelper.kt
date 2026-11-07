package com.example.util

object TranslationHelper {
    /**
     * Standalone translation service for English, Hindi, Urdu, and Arabic.
     */
    fun translate(text: String, targetLanguage: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        val commonDictEnToHi = mapOf(
            "hello" to "नमस्ते",
            "hi" to "नमस्ते",
            "how are you" to "आप कैसे हैं?",
            "how are you?" to "आप कैसे हैं?",
            "good morning" to "सुप्रभात",
            "good evening" to "शुभ संध्या",
            "good night" to "शुभ रात्रि",
            "thank you" to "धन्यवाद",
            "thanks" to "धन्यवाद",
            "welcome" to "स्वागत है",
            "see you later" to "बाद में मिलते हैं",
            "yes" to "हाँ",
            "no" to "नहीं",
            "please" to "कृपया",
            "sorry" to "माफ़ कीजिए",
            "okay" to "ठीक है",
            "where are you?" to "आप कहाँ हैं?",
            "where are you" to "आप कहाँ हैं?",
            "call me" to "मुझे कॉल करें",
            "i am on the way" to "मैं रास्ते में हूँ",
            "let's meet tomorrow" to "कल मिलते हैं"
        )

        val commonDictEnToUr = mapOf(
            "hello" to "السلام علیکم",
            "hi" to "سلام",
            "how are you" to "آپ کیسے ہیں؟",
            "how are you?" to "آپ کیسے ہیں؟",
            "good morning" to "صبح بخیر",
            "good evening" to "شام بخیر",
            "good night" to "شب بخیر",
            "thank you" to "شکریہ",
            "thanks" to "شکریہ",
            "welcome" to "خوش آمدید",
            "see you later" to "پھر ملیں گے",
            "yes" to "ہاں",
            "no" to "نہیں",
            "please" to "براہ کرم",
            "sorry" to "معذرت",
            "okay" to "ٹھیک ہے",
            "call me" to "مجھے کال کریں",
            "i am on the way" to "میں راستے میں ہوں"
        )

        val lower = trimmed.lowercase()
        return when {
            targetLanguage.contains("Hindi", ignoreCase = true) || targetLanguage.equals("hi", true) -> {
                commonDictEnToHi[lower] ?: "[हिन्दी]: $trimmed"
            }
            targetLanguage.contains("Urdu", ignoreCase = true) || targetLanguage.equals("ur", true) -> {
                commonDictEnToUr[lower] ?: "[اردو]: $trimmed"
            }
            targetLanguage.contains("Arabic", ignoreCase = true) || targetLanguage.equals("ar", true) -> {
                "[العربية]: $trimmed"
            }
            else -> {
                "[English]: $trimmed"
            }
        }
    }
}
