package com.example.nt_05_thienthach

import android.content.res.Configuration
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nt_05_thienthach.data.AppLanguages
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LanguageResourcesTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun localized(tag: String) = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
        setLocale(Locale.forLanguageTag(tag))
    })
    @Test fun allTwentyLanguagesLoadTheirOwnMenuResources() {
        val expected = listOf("Ngôn ngữ", "Language", "语言", "言語", "언어", "Idioma", "Idioma", "भाषा", "Bahasa", "ภาษา", "Язык", "Sprache", "Langue", "Dil", "اللغة", "Lingua", "Język", "Wika", "Bahasa", "زبان")
        AppLanguages.supported.forEachIndexed { index, language ->
            val ctx = localized(language.tag)
            assertEquals(language.tag, expected[index], ctx.getString(R.string.language))
            assertTrue(ctx.getString(R.string.help_controls_body).isNotBlank())
            assertTrue(ctx.getString(R.string.capacitor_body).isNotBlank())
        }
    }
    @Test fun formattedResourcesAreSafeInEveryLanguage() {
        val formats = listOf(R.string.armor_value, R.string.level, R.string.combo_status, R.string.mission_survive,
            R.string.notice_near, R.string.goal_remaining, R.string.time_format, R.string.slow_effect)
        AppLanguages.supported.forEach { language ->
            val ctx = localized(language.tag)
            formats.forEach { id ->
                val output = ctx.getString(id, 1, 2, 3)
                assertFalse("Unexpanded format in ${language.tag}: $output", output.contains("%"))
            }
        }
    }
    @Test fun arabicAndUrduUseRtlWhileEnglishAndVietnameseUseLtr() {
        for (tag in listOf("ar", "ur")) assertEquals(View.LAYOUT_DIRECTION_RTL, localized(tag).resources.configuration.layoutDirection)
        for (tag in listOf("en", "vi")) assertEquals(View.LAYOUT_DIRECTION_LTR, localized(tag).resources.configuration.layoutDirection)
    }
    @Test fun unknownLanguageFallsBackToEnglish() {
        assertEquals("Language", localized("nl").getString(R.string.language))
        assertEquals("Meteor Dash", localized("nl").getString(R.string.app_name))
    }
}
