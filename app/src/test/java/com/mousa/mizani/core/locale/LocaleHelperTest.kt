package com.mousa.mizani.core.locale

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LocaleHelperTest {

    @Test
    fun `no language is chosen when nothing is saved`() {
        assertNull(LocaleHelper.chosenLanguage(null))
    }

    @Test
    fun `saved codes are the chosen language`() {
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.chosenLanguage("en"))
        assertEquals(AppLanguage.ARABIC, LocaleHelper.chosenLanguage("ar"))
    }

    @Test
    fun `an unsupported saved code counts as not chosen`() {
        assertNull(LocaleHelper.chosenLanguage("fr"))
        assertNull(LocaleHelper.chosenLanguage(""))
    }

    @Test
    fun `with nothing saved the shown language is Arabic only on an Arabic phone`() {
        assertEquals(AppLanguage.ARABIC, LocaleHelper.shownLanguage(null, "ar"))
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.shownLanguage(null, "en"))
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.shownLanguage(null, "fr"))
    }

    @Test
    fun `a saved language wins over the phone language`() {
        listOf("ar", "en", "fr").forEach { phone ->
            assertEquals(AppLanguage.ENGLISH, LocaleHelper.shownLanguage("en", phone))
            assertEquals(AppLanguage.ARABIC, LocaleHelper.shownLanguage("ar", phone))
        }
    }

    @Test
    fun `context is returned unchanged when the user never chose a language`() {
        val context = mock(Context::class.java)
        val preferences = mock(SharedPreferences::class.java)
        `when`(context.getSharedPreferences(anyString(), any(Int::class.java) ?: 0))
            .thenReturn(preferences)
        `when`(preferences.getString(anyString(), isNull())).thenReturn(null)

        assertSame(context, LocaleHelper.onAttach(context))
    }
}
