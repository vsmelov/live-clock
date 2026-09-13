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
