package com.mousa.mizani.core

import com.mousa.mizani.core.time.DateUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class DateUtilsLocaleTest {

    private val originalLocale = Locale.getDefault()
    private val march10 = Date.from(LocalDateTime.of(2026, 3, 10, 14, 5).atZone(ZoneId.systemDefault()).toInstant())

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    @Test
    fun `month names follow the language after switching from Arabic to English`() {
        Locale.setDefault(Locale.forLanguageTag("ar"))
        val arabic = DateUtils.formatMonthYear(march10)

        Locale.setDefault(Locale.ENGLISH)

        assertEquals("March 2026", DateUtils.formatMonthYear(march10))
        assertEquals("Mar 10, 2026", DateUtils.formatDate(march10))
        assertEquals("Mar 10, 2026 14:05", DateUtils.formatDateTime(march10))
        assertEquals("14:05", DateUtils.formatTime(march10))
        assertEquals(false, arabic == DateUtils.formatMonthYear(march10))
    }

    @Test
    fun `month names follow the language after switching from English to Arabic`() {
        Locale.setDefault(Locale.ENGLISH)
        DateUtils.formatMonthYear(march10)

        Locale.setDefault(Locale.forLanguageTag("ar"))

        assertEquals(
            DateUtils.format(march10, DateUtils.PATTERN_MONTH_YEAR, Locale.forLanguageTag("ar")),
            DateUtils.formatMonthYear(march10)
        )
    }
}
