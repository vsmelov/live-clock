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
class EventTypeTest {

    @Test
    fun `идентификаторы уникальны`() {
        val ids = EventType.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `идентификаторы непустые и без пробелов`() {
        EventType.entries.forEach { type ->
            assertTrue("пустой id у $type", type.id.isNotBlank())
            assertEquals("id с пробелами у $type", type.id.trim(), type.id)
        }
    }

    @Test
    fun `у каждого типа есть подпись и эмодзи`() {
        EventType.entries.forEach { type ->
            assertTrue("нет подписи у $type", type.label.isNotBlank())
            assertTrue("нет эмодзи у $type", type.emoji.isNotBlank())
        }
    }

    @Test
    fun `разбор по id возвращает исходный тип`() {
        EventType.entries.forEach { type ->
            assertEquals(type, EventType.fromId(type.id))
        }
    }

    @Test
    fun `неизвестный id не валит разбор`() {
        assertNull(EventType.fromId("такого-типа-нет"))
        assertNull(EventType.fromId(""))
    }

    @Test
    fun `ни один тип не нулевой — кнопка без эффекта бессмысленна`() {
        EventType.entries.forEach { type ->
            assertTrue("нулевой коэффициент у $type", type.deltaMinutes != 0)
        }
    }

    @Test
    fun `коэффициенты берутся из Coefficients`() {
        assertEquals(Coefficients.SMOKE, EventType.SMOKE.deltaMinutes)
        assertEquals(Coefficients.DRINK, EventType.DRINK.deltaMinutes)
        assertEquals(Coefficients.REST, EventType.REST.deltaMinutes)
        assertEquals(Coefficients.WORKOUT, EventType.WORKOUT.deltaMinutes)
    }

    @Test
    fun `знак коэффициента согласован с признаком isGain`() {
        EventType.entries.forEach { type ->
            assertEquals("$type", type.deltaMinutes >= 0, type.isGain)
        }
    }

    @Test
    fun `кнопки виджета существуют`() {
        // Виджет жёстко показывает эти два типа, они не должны пропасть из enum.
        assertNotNull(EventType.fromId("smoke"))
        assertNotNull(EventType.fromId("rest"))
    }
}
