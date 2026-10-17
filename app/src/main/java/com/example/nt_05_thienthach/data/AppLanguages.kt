package com.example.nt_05_thienthach.data

import java.util.Locale

data class AppLanguage(val tag: String, val nativeName: String, val market: String)
object AppLanguages {
    val supported = listOf(
        AppLanguage("vi", "Tiếng Việt", "Việt Nam"),
        AppLanguage("en", "English", "United States"),
        AppLanguage("zh", "简体中文", "中国"),
        AppLanguage("ja", "日本語", "日本"),
        AppLanguage("ko", "한국어", "대한민국"),
        AppLanguage("pt", "Português", "Brasil"),
        AppLanguage("es", "Español", "México"),
        AppLanguage("hi", "हिन्दी", "भारत"),
        AppLanguage("id", "Bahasa Indonesia", "Indonesia"),
        AppLanguage("th", "ไทย", "ไทย"),
        AppLanguage("ru", "Русский", "Россия"),
        AppLanguage("de", "Deutsch", "Deutschland"),
        AppLanguage("fr", "Français", "France"),
        AppLanguage("tr", "Türkçe", "Türkiye"),
        AppLanguage("ar", "العربية", "السعودية"),
        AppLanguage("it", "Italiano", "Italia"),
        AppLanguage("pl", "Polski", "Polska"),
        AppLanguage("fil", "Filipino", "Pilipinas"),
        AppLanguage("ms", "Bahasa Melayu", "Malaysia"),
        AppLanguage("ur", "اردو", "پاکستان")
    )
    fun resolve(tag: String): AppLanguage? {
        val code = Locale.forLanguageTag(tag).language.let { if (it == "in") "id" else if (it == "tl") "fil" else it }
        return supported.firstOrNull { it.tag == code }
    }
}
