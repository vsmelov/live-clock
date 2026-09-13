package com.vsmelov.liveclock.sync

import com.vsmelov.liveclock.data.SyncSettings
import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Имена тестов латиницей намеренно: из backtick-имени собирается имя .class
 * для лямбд внутри теста, и кириллица в пути ломает сборку под не-UTF-8
 * локалью (POSIX на CI — падает даже clean). Комментарии и сообщения
 * ассертов при этом остаются русскими.
 */
class SyncClientTest {

    private val events = listOf(
        LifeEvent(EventType.SMOKE, Instant.ofEpochSecond(1_757_000_000), -15),
        LifeEvent(EventType.WORKOUT, Instant.ofEpochSecond(1_757_100_000), 30),
    )

    @Test
    fun `sync is off by default and Noop is selected`() {
        assertSame(NoopSyncClient, SyncClientProvider.forSettings(SyncSettings()))
    }

    @Test
    fun `without a url sync stays Noop even when enabled`() {
        val settings = SyncSettings(enabled = true, endpointUrl = "  ")
        assertSame(NoopSyncClient, SyncClientProvider.forSettings(settings))
    }

    @Test
    fun `with a url and the flag the http client is selected`() {
        val settings = SyncSettings(enabled = true, endpointUrl = "https://example.test/events")
        assertTrue(SyncClientProvider.forSettings(settings) is HttpSyncClient)
    }

    @Test
    fun `disabled sync is not selected even with a valid url`() {
        val settings = SyncSettings(enabled = false, endpointUrl = "https://example.test/events")
        assertSame(NoopSyncClient, SyncClientProvider.forSettings(settings))
    }

    @Test
    fun `Noop silently swallows any log`() = runTest {
        NoopSyncClient.push(events)
        NoopSyncClient.push(emptyList())
    }

    @Test
    fun `an empty log does not touch the network even with a broken url`() = runTest {
        // URL невалидный: если бы клиент его разбирал, тест упал бы исключением.
        HttpSyncClient("не-адрес-вообще", "token").push(emptyList())
    }

    @Test
    fun `cleartext http is rejected with a clear error`() = runTest {
        val error = runCatching {
            HttpSyncClient("http://example.test/events", "token").push(events)
        }.exceptionOrNull()

        assertTrue("ожидалась SyncException, получено $error", error is SyncException)
        assertTrue(error?.message.orEmpty(), error?.message.orEmpty().contains("https"))
    }

    @Test
    fun `the request body carries events in the stable format`() {
        val json = HttpSyncClient.encodePayload(events, Instant.ofEpochSecond(1_757_200_000))

        assertTrue(json, json.contains("\"type\":\"smoke\""))
        assertTrue(json, json.contains("\"type\":\"workout\""))
        assertTrue(json, json.contains("\"delta_minutes\":-15"))
        assertTrue(json, json.contains("\"sent_at_epoch_second\":1757200000"))
    }

    @Test
    fun `the request body does not contain the token`() {
        val json = HttpSyncClient.encodePayload(events, Instant.ofEpochSecond(1_757_200_000))
        assertFalse(json, json.contains("Bearer"))
        assertFalse(json, json.contains("token"))
    }
}
