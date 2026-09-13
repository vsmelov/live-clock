package com.vsmelov.liveclock.sync

import com.vsmelov.liveclock.data.LifeClockJson
import com.vsmelov.liveclock.data.SyncPayload
import com.vsmelov.liveclock.data.toDto
import com.vsmelov.liveclock.domain.LifeEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.time.Instant

/**
 * POSTs the event log to an arbitrary endpoint with a Bearer token.
 *
 * Deliberately on the JDK's bare [HttpURLConnection]: the job is one POST
 * endpoint, and pulling in a third-party HTTP client for that is not worth it.
 *
 * Called only from the worker, never from an ActionCallback.
 */
class HttpSyncClient(
    private val endpointUrl: String,
    private val bearerToken: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Instant = Instant::now,
) : EventSyncClient {

    override suspend fun push(events: List<LifeEvent>) {
        if (events.isEmpty()) return

        val url = parseEndpoint(endpointUrl)
        val body = encodePayload(events, now()).toByteArray(Charsets.UTF_8)

        withContext(ioDispatcher) {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (bearerToken.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                }
                setFixedLengthStreamingMode(body.size)
            }

            try {
                connection.outputStream.use { stream -> stream.write(body) }

                val code = connection.responseCode
                if (code !in SUCCESS_CODES) {
                    // The error body is drained but never put into the exception:
                    // it could contain anything, including an echo of the headers
                    // with the token in them.
                    connection.errorStream?.use { it.readBytes() }
                    throw SyncException("Sync server answered $code")
                }
                connection.inputStream.use { it.readBytes() }
            } catch (error: IOException) {
                throw if (error is SyncException) error else SyncException("Sync failed", error)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseEndpoint(raw: String): URL {
        val uri = try {
            URI(raw.trim())
        } catch (error: IllegalArgumentException) {
            throw SyncException("Malformed sync URL", error)
        }

        // https only. A Bearer token over plain http would go out in the clear,
        // and targetSdk 36 blocks cleartext by default anyway — so an explicit
        // error is more useful than an opaque refusal from the platform.
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            throw SyncException("The sync URL must be https, not ${uri.scheme}")
        }

        return try {
            uri.toURL()
        } catch (error: MalformedURLException) {
            throw SyncException("Malformed sync URL", error)
        } catch (error: IllegalArgumentException) {
            throw SyncException("Malformed sync URL", error)
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 15_000
        private val SUCCESS_CODES = 200..299

        /** The request body. Kept separate so it can be checked without a network. */
        fun encodePayload(events: List<LifeEvent>, sentAt: Instant): String =
            LifeClockJson.encodeToString(
                SyncPayload(
                    events = events.map { it.toDto() },
                    sentAtEpochSecond = sentAt.epochSecond,
                ),
            )
    }
}
