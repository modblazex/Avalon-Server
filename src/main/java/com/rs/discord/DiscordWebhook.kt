package com.rs.discord

import com.rs.Settings
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object DiscordWebhook {
    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile var webhookUrl: String? = null

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(WebhookPayload::class.java)
    private val queue = LinkedBlockingQueue<WebhookPayload>()

    private const val JSON = "application/json; charset=utf-8"

    init {
        thread(name = "DiscordWebhookWorker", isDaemon = true) {
            val minInterval = 250L
            var lastSend = 0L

            while (true) {
                val payload = queue.take()

                val now = System.currentTimeMillis()
                val diff = now - lastSend
                if (diff < minInterval) {
                    Thread.sleep(minInterval - diff)
                }

                sendNowWithRetry(payload)
                lastSend = System.currentTimeMillis()
            }
        }
    }

    fun enqueue(payload: WebhookPayload) {
        if (webhookUrl.isNullOrBlank()) return
        if (!Settings.discordEnabled) {
            return;
        }
        println("DiscordWebhook: sending to $webhookUrl")
        queue.offer(payload)
    }

    @JvmStatic
    private fun sendNowWithRetry(payload: WebhookPayload) {
        val url = webhookUrl ?: return

        var attempts = 0
        var backoffMs = 500L

        while (attempts < 5) {
            attempts++

            // Rebuild request body each attempt (OkHttp request bodies are one-shot)
            val body = adapter.toJson(payload).toRequestBody(JSON.toMediaType())
            val req = Request.Builder().url(url).post(body).build()

            client.newCall(req).execute().use { resp ->
                when {
                    resp.isSuccessful || resp.code == 204 -> return // success
                    resp.code == 429 -> {
                        val retryAfterMs = resp.header("Retry-After")?.toLongOrNull()?.let { it * 1000 }
                            ?: 1000L
                        Thread.sleep(retryAfterMs)
                    }
                    resp.code in 500..599 -> {
                        Thread.sleep(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(8000)
                    }
                    else -> return // 4xx: unrecoverable
                }
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class WebhookPayload(
    val content: String? = null,
    val username: String? = null,
    val avatar_url: String? = null,
    val embeds: List<Embed>? = null
)

@JsonClass(generateAdapter = true)
data class Embed(
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val color: Int? = null,
    val timestamp: String? = null,
    val fields: List<EmbedField>? = null,
    val footer: EmbedFooter? = null,
    val thumbnail: EmbedImage? = null,
    val image: EmbedImage? = null
)

@JsonClass(generateAdapter = true) data class EmbedField(val name: String, val value: String, val inline: Boolean = false)
@JsonClass(generateAdapter = true) data class EmbedFooter(val text: String, val icon_url: String? = null)
@JsonClass(generateAdapter = true) data class EmbedImage(val url: String)
