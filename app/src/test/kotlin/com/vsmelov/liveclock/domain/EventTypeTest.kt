package com.vsmelov.liveclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Extensibility tests: they walk [EventType.entries] rather than listing types by
 * hand, so they keep guarding the invariants after new entries are added.
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
            assertTrue("blank id on $type", type.id.isNotBlank())
            assertEquals("id with whitespace on $type", type.id.trim(), type.id)
        }
    }

    @Test
    fun `every type has a label resource, keywords and an emoji`() {
        EventType.entries.forEach { type ->
            assertTrue("no label resource on $type", type.labelRes != 0)
            assertTrue("no keywords resource on $type", type.keywordsRes != 0)
            assertTrue("no emoji on $type", type.emoji.isNotBlank())
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
        assertNull(EventType.fromId("no-such-type"))
        assertNull(EventType.fromId(""))
    }

    @Test
    fun `a zero delta is allowed only where the effect was checked and not found`() {
        // Zero is the result of checking, not a forgotten value. A zero button
        // exists so that it is visible somebody looked and found nothing.
        EventType.entries.forEach { type ->
            if (type.deltaMinutes == 0) {
                assertEquals(
                    "zero delta on $type without a «no effect» marker",
                    Confidence.NONE,
                    type.evidence.confidence,
                )
            }
        }
    }

    @Test
    fun `isNeutral agrees with a zero delta`() {
        EventType.entries.forEach { type ->
            assertEquals("$type", type.deltaMinutes == 0, type.isNeutral)
        }
    }

    @Test
    fun `deltas come from Coefficients`() {
        assertEquals(Coefficients.SMOKE, EventType.SMOKE.deltaMinutes)
        assertEquals(Coefficients.DRINK, EventType.DRINK.deltaMinutes)
        assertEquals(Coefficients.REST, EventType.REST.deltaMinutes)
        assertEquals(Coefficients.WORKOUT, EventType.WORKOUT.deltaMinutes)
        assertEquals(Coefficients.SLEEP_SHORT, EventType.SLEEP_SHORT.deltaMinutes)
        assertEquals(Coefficients.SOCIAL, EventType.SOCIAL.deltaMinutes)
    }

    @Test
    fun `delta sign agrees with isGain`() {
        EventType.entries.forEach { type ->
            assertEquals("$type", type.deltaMinutes >= 0, type.isGain)
        }
    }

    @Test
    fun `widget button types exist`() {
        // The widget defaults hard-code these two; they must not vanish.
        assertNotNull(EventType.fromId("smoke"))
        assertNotNull(EventType.fromId("rest"))
        assertTrue(EventType.DEFAULT_PINNED.isNotEmpty())
        assertEquals(EventType.DEFAULT_PINNED.size, EventType.DEFAULT_PINNED.toSet().size)
    }
}

/** The action search box. Keywords are supplied explicitly so this stays pure. */
class ActionSearchTest {

    private fun matches(query: String, label: String = "Smoked", keywords: String = "") =
        ActionSearch.matches(query, id = "smoke", label = label, keywords = keywords)

    @Test
    fun `an empty query matches everything`() {
        assertTrue(matches(""))
        assertTrue(matches("   "))
    }

    @Test
    fun `search is case insensitive`() {
        assertTrue(matches("smoked"))
        assertTrue(matches("SMOKED"))
        assertTrue(matches("SmOkEd"))
    }

    @Test
    fun `search finds a type by its stable id`() {
        assertTrue(matches("smoke", label = "Anything"))
    }

    @Test
    fun `search runs across both languages at once`() {
        // Keywords deliberately carry both languages, so a query in either one
        // finds the action whatever the interface language is set to.
        val keywords = "cigarette tobacco sigareta"
        assertTrue(ActionSearch.matches("tobacco", "smoke", "Smoked", keywords))
        assertTrue(ActionSearch.matches("sigareta", "smoke", "Smoked", keywords))
    }

    @Test
    fun `a nonsense query matches nothing`() {
        assertTrue(!matches("quasimodo", keywords = "cigarette tobacco"))
    }

    @Test
    fun `surrounding spaces do not break the search`() {
        assertTrue(matches("  smoked  "))
    }
}
