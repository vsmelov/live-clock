package com.vsmelov.liveclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `ids are unique and stable`() {
        val ids = AppLanguage.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        // Stored as ids rather than ordinals so adding a language never
        // reshuffles somebody's saved preference.
        assertEquals("system", AppLanguage.SYSTEM.id)
        assertEquals("en", AppLanguage.ENGLISH.id)
        assertEquals("ru", AppLanguage.RUSSIAN.id)
    }

    @Test
    fun `parsing returns the original language`() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromId(language.id))
        }
    }

    @Test
    fun `an unknown or missing id falls back to following the system`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromId(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromId("klingon"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromId(""))
    }

    @Test
    fun `only system has no locale tag`() {
        assertNull(AppLanguage.SYSTEM.tag)
        AppLanguage.entries.filter { it != AppLanguage.SYSTEM }.forEach { language ->
            assertTrue("$language has no locale tag", !language.tag.isNullOrBlank())
        }
    }

    @Test
    fun `every language has a label resource`() {
        AppLanguage.entries.forEach { language ->
            assertTrue("$language has no label", language.labelRes != 0)
        }
    }
}
