package com.vsmelov.liveclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proofs are a promise to the user, not decoration. These tests hold that promise
 * the same way ordinary invariants are held: no action without a source, no
 * source without a caveat.
 */
class EvidenceTest {

    @Test
    fun `every action carries evidence with every field filled in`() {
        EventType.entries.forEach { type ->
            val evidence = type.evidence
            assertTrue("no exposure on $type", evidence.exposureRes != 0)
            assertTrue("no basis on $type", evidence.basisRes != 0)
            assertTrue("no caveat on $type", evidence.caveatRes != 0)
            assertTrue("no source title on $type", evidence.sourceTitle.isNotBlank())
            assertTrue("no source link on $type", evidence.sourceUrl.isNotBlank())
        }
    }

    @Test
    fun `every source link is https`() {
        EventType.entries.forEach { type ->
            assertTrue(
                "link on $type is not https: ${type.evidence.sourceUrl}",
                type.evidence.sourceUrl.startsWith("https://"),
            )
        }
    }

    @Test
    fun `values chosen by hand are marked as chosen, not as research`() {
        val chosen = EventType.entries.filter { it.evidence.confidence == Confidence.CHOSEN }
        assertEquals(
            setOf(EventType.DRINK, EventType.REST, EventType.WORKOUT),
            chosen.toSet(),
        )
    }

    @Test
    fun `an action marked as having no effect really has a zero delta`() {
        EventType.entries
            .filter { it.evidence.confidence == Confidence.NONE }
            .forEach { type ->
                assertEquals("$type is marked «no effect» but moves the estimate", 0, type.deltaMinutes)
            }
    }

    @Test
    fun `estimates are marked as estimates rather than hidden behind a link`() {
        val estimates = EventType.entries.filter { it.evidence.confidence == Confidence.ESTIMATE }
        assertTrue("estimates should exist and be labelled", estimates.isNotEmpty())
        // They must not claim a named study: the source title says what it is.
        estimates.forEach { type ->
            assertTrue(
                "$type presents an estimate as a named study",
                type.evidence.sourceTitle.contains("mine", ignoreCase = true),
            )
        }
    }

    @Test
    fun `every confidence level has a label resource`() {
        Confidence.entries.forEach { level ->
            assertTrue("$level has no label", level.labelRes != 0)
        }
    }
}

/** The hazard-ratio to minutes conversion. */
class MicrolivesTest {

    @Test
    fun `a neutral hazard ratio moves nothing`() {
        assertEquals(0.0, Microlives.perDay(1.0), 1e-9)
        assertEquals(0, Microlives.minutesPerDay(1.0))
    }

    @Test
    fun `harmful ratios cost life and protective ratios add it`() {
        assertTrue(Microlives.minutesPerDay(1.13) < 0)
        assertTrue(Microlives.minutesPerDay(0.81) > 0)
    }

    @Test
    fun `the formula reproduces the published table rows`() {
        // The author rounds to whole microlives, hence the tolerance.
        assertEquals(-1.33, Microlives.perDay(1.13), 0.01) // red meat, table says -1
        assertEquals(4.53, Microlives.perDay(0.66), 0.01) // vegetables, table says +4
        assertEquals(2.30, Microlives.perDay(0.81), 0.01) // exercise, table says +2
        assertEquals(1.15, Microlives.perDay(0.90), 0.01) // coffee, table says +1
        assertEquals(-0.84, Microlives.perDay(1.08), 0.01) // television, table says -1
    }

    @Test
    fun `values used in the app match their hazard ratios`() {
        assertEquals(Coefficients.LATE_NIGHT, Microlives.minutesPerDay(1.10))
        assertEquals(Coefficients.SLEEP_SHORT, Microlives.minutesPerDay(1.14))
        assertEquals(Coefficients.NATURE, Microlives.minutesPerDay(0.96))
        assertEquals(Coefficients.SOCIAL, Microlives.minutesPerDay(1.0 / 1.29))
        assertEquals(Coefficients.SAUNA, Microlives.minutesPerDay(0.60))
        assertEquals(Coefficients.NUTS, Microlives.minutesPerDay(0.80))
        assertEquals(Coefficients.RUNNING, Microlives.minutesPerDay(0.70))
    }

    @Test
    fun `the trusted range matches what the authors promise`() {
        assertTrue(Microlives.isTrusted(1.13))
        assertTrue(Microlives.isTrusted(0.81))
        assertTrue("2.17 is outside the promised range", !Microlives.isTrusted(2.17))
        assertTrue("0.60 is outside the promised range", !Microlives.isTrusted(0.60))
    }

    @Test
    fun `a nonpositive hazard ratio is rejected instead of producing nonsense`() {
        listOf(0.0, -1.0).forEach { bad ->
            val error = runCatching { Microlives.perDay(bad) }.exceptionOrNull()
            assertTrue("$bad went through without an error", error is IllegalArgumentException)
        }
    }
}
