package com.vsmelov.liveclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты на расширяемость: они не перечисляют типы руками, а проходят по
 * [EventType.entries], поэтому продолжат защищать инвариант после того,
 * как в enum добавятся новые записи.
 */
/**
 * Имена тестов латиницей намеренно: из backtick-имени собирается имя .class
 * для лямбд внутри теста, и кириллица в пути ломает сборку под не-UTF-8
 * локалью (POSIX на CI — падает даже clean). Комментарии и сообщения
 * ассертов при этом остаются русскими.
 */
class EventTypeTest {

    @Test
    fun `ids are unique`() {
        val ids = EventType.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `ids are non blank and trimmed`() {
        EventType.entries.forEach { type ->
            assertTrue("пустой id у $type", type.id.isNotBlank())
            assertEquals("id с пробелами у $type", type.id.trim(), type.id)
        }
    }

    @Test
    fun `every type has a label and an emoji`() {
        EventType.entries.forEach { type ->
            assertTrue("нет подписи у $type", type.label.isNotBlank())
            assertTrue("нет эмодзи у $type", type.emoji.isNotBlank())
        }
    }

    @Test
    fun `parsing an id returns the original type`() {
        EventType.entries.forEach { type ->
            assertEquals(type, EventType.fromId(type.id))
        }
    }

    @Test
    fun `an unknown id does not break parsing`() {
        assertNull(EventType.fromId("такого-типа-нет"))
        assertNull(EventType.fromId(""))
    }

    @Test
    fun `no type has a zero delta`() {
        EventType.entries.forEach { type ->
            assertTrue("нулевой коэффициент у $type", type.deltaMinutes != 0)
        }
    }

    @Test
    fun `deltas come from Coefficients`() {
        assertEquals(Coefficients.SMOKE, EventType.SMOKE.deltaMinutes)
        assertEquals(Coefficients.DRINK, EventType.DRINK.deltaMinutes)
        assertEquals(Coefficients.REST, EventType.REST.deltaMinutes)
        assertEquals(Coefficients.WORKOUT, EventType.WORKOUT.deltaMinutes)
    }

    @Test
    fun `delta sign agrees with isGain`() {
        EventType.entries.forEach { type ->
            assertEquals("$type", type.deltaMinutes >= 0, type.isGain)
        }
    }

    @Test
    fun `widget button types exist`() {
        // Виджет жёстко показывает эти два типа, они не должны пропасть из enum.
        assertNotNull(EventType.fromId("smoke"))
        assertNotNull(EventType.fromId("rest"))
    }
}

/** Поиск по действиям — то, что вводится в строку над списком. */
class EventTypeSearchTest {

    @Test
    fun `empty query matches everything`() {
        EventType.entries.forEach { type ->
            assertTrue("$type", type.matches(""))
            assertTrue("$type", type.matches("   "))
        }
    }

    @Test
    fun `search is case insensitive`() {
        assertTrue(EventType.SMOKE.matches("покурил"))
        assertTrue(EventType.SMOKE.matches("ПОКУРИЛ"))
        assertTrue(EventType.SMOKE.matches("ПоКуРиЛ"))
    }

    @Test
    fun `search finds a type by how it is called in the head`() {
        assertTrue(EventType.SMOKE.matches("сижка"))
        assertTrue(EventType.SMOKE.matches("сигарета"))
        assertTrue(EventType.DRINK.matches("бухло"))
        assertTrue(EventType.DRINK.matches("пиво"))
        assertTrue(EventType.WORKOUT.matches("зал"))
        assertTrue(EventType.WORKOUT.matches("качалка"))
    }

    @Test
    fun `search works in english too`() {
        assertTrue(EventType.SMOKE.matches("smoke"))
        assertTrue(EventType.COFFEE.matches("coffee"))
        assertTrue(EventType.WORKOUT.matches("gym"))
    }

    @Test
    fun `search finds a type by its stable id`() {
        EventType.entries.forEach { type ->
            assertTrue("$type не находится по своему id", type.matches(type.id))
        }
    }

    @Test
    fun `nonsense query matches nothing`() {
        val query = "квазимодо"
        assertTrue(EventType.entries.none { it.matches(query) })
    }

    @Test
    fun `surrounding spaces do not break the search`() {
        assertTrue(EventType.COFFEE.matches("  кофе  "))
    }

    @Test
    fun `every type is reachable by typing its own label`() {
        EventType.entries.forEach { type ->
            val hits = EventType.entries.filter { it.matches(type.label) }
            assertTrue("$type не находится по своей подписи", type in hits)
        }
    }

    @Test
    fun `default pinned types exist and are distinct`() {
        assertTrue(EventType.DEFAULT_PINNED.isNotEmpty())
        assertEquals(EventType.DEFAULT_PINNED.size, EventType.DEFAULT_PINNED.toSet().size)
    }
}
