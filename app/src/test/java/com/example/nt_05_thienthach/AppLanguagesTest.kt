package com.example.nt_05_thienthach

import com.example.nt_05_thienthach.data.AppLanguages
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class AppLanguagesTest {
    @Test fun twentyUniqueLanguagesAndMarkets() {
        val all = AppLanguages.supported
        assertEquals(20, all.size)
        assertEquals(20, all.map { it.tag }.distinct().size)
        assertEquals(20, all.map { it.market }.distinct().size)
        assertTrue(all.all { it.nativeName.isNotBlank() && Locale.forLanguageTag(it.tag).language.isNotEmpty() })
    }
    @Test fun regionalLocalesResolveToTheirSupportedLanguage() {
        assertEquals("pt", AppLanguages.resolve("pt-BR")?.tag)
        assertEquals("es", AppLanguages.resolve("es-MX")?.tag)
        assertEquals("zh", AppLanguages.resolve("zh-Hans-CN")?.tag)
        assertEquals("ar", AppLanguages.resolve("ar-SA")?.tag)
        assertEquals("ur", AppLanguages.resolve("ur-PK")?.tag)
    }
    @Test fun legacyLanguageAliasesResolve() {
        assertEquals("id", AppLanguages.resolve("in-ID")?.tag)
        assertEquals("fil", AppLanguages.resolve("tl-PH")?.tag)
    }
    @Test fun systemAndUnsupportedLocalesAreNotMisreportedAsManualSelection() {
        assertNull(AppLanguages.resolve("")); assertNull(AppLanguages.resolve("nl-NL"))
    }
}
