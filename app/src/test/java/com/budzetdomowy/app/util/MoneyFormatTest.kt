package com.budzetdomowy.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatTest {
    @Test
    fun parsePlainZloty() {
        assertEquals(1200L, MoneyFormat.parseToCents("12"))
    }

    @Test
    fun parseComma() {
        assertEquals(1234L, MoneyFormat.parseToCents("12,34"))
    }

    @Test
    fun parseDot() {
        assertEquals(50L, MoneyFormat.parseToCents("0.50"))
    }

    @Test
    fun rejectEmptyAndNegative() {
        assertNull(MoneyFormat.parseToCents(""))
        assertNull(MoneyFormat.parseToCents("abc"))
        assertNull(MoneyFormat.parseToCents("-3"))
    }
}
