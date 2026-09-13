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
 * POST лога событий на произвольный endpoint с Bearer-токеном.
 *
 * Сознательно на голом [HttpURLConnection] из JDK: задача — одна POST-ручка,
 * тянуть ради неё сторонний HTTP-клиент незачем.
 *
 * Вызывается только из воркера, никогда из ActionCallback.
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
                    // Тело ошибки читаем, но в исключение не кладём: там может
                    // оказаться что угодно, включая эхо заголовков с токеном.
                    connection.errorStream?.use { it.readBytes() }
                    throw SyncException("Сервер синка ответил $code")
                }
                connection.inputStream.use { it.readBytes() }
            } catch (error: IOException) {
                throw if (error is SyncException) error else SyncException("Синк не удался", error)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseEndpoint(raw: String): URL {
        val uri = try {
            URI(raw.trim())
        } catch (error: IllegalArgumentException) {
            throw SyncException("Некорректный URL синка", error)
        }

        // Только https. Bearer-токен по открытому http ушёл бы в эфир открытым
        // текстом, и targetSdk 36 всё равно режет cleartext по умолчанию —
        // так что явная ошибка полезнее невнятного отказа платформы.
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            throw SyncException("URL синка должен быть https, а не ${uri.scheme}")
        }

        return try {
            uri.toURL()
        } catch (error: MalformedURLException) {
            throw SyncException("Некорректный URL синка", error)
        } catch (error: IllegalArgumentException) {
            throw SyncException("Некорректный URL синка", error)
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 15_000
        private val SUCCESS_CODES = 200..299

        /** Тело запроса. Вынесено отдельно, чтобы проверялось без сети. */
        fun encodePayload(events: List<LifeEvent>, sentAt: Instant): String =
            LifeClockJson.encodeToString(
                SyncPayload(
                    events = events.map { it.toDto() },
                    sentAtEpochSecond = sentAt.epochSecond,
                ),
            )
    }
}
