package com.xtbtch4.tidaldownloader

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

object TidalApi {
    const val REDIRECT_URI = "com.xtbtch4.tidaldownloader://oauth/callback"
    private const val PREFS = "tidal_oauth"
    private const val AUTH_URL = "https://login.tidal.com/authorize"
    private const val TOKEN_URL = "https://auth.tidal.com/v1/oauth2/token"
    private const val API_BASE = "https://openapi.tidal.com/v2"

    @Volatile var accessToken: String? = null
        private set

    private fun b64(bytes: ByteArray) =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    fun createAuthorizationUrl(context: Context, clientId: String): String {
        val random = SecureRandom()
        val verifierBytes = ByteArray(32).also(random::nextBytes)
        val verifier = b64(verifierBytes)
        val stateBytes = ByteArray(24).also(random::nextBytes)
        val state = b64(stateBytes)
        val challenge = b64(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("verifier", verifier).putString("state", state).putString("client_id", clientId.trim()).apply()

        return Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId.trim())
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("state", state)
            .build().toString()
    }

    fun finishAuthorization(context: Context, callback: Uri): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expectedState = prefs.getString("state", null)
        val returnedState = callback.getQueryParameter("state")
        if (expectedState.isNullOrBlank() || returnedState != expectedState) {
            throw IllegalStateException("OAuth state не совпал. Запусти вход заново.")
        }
        callback.getQueryParameter("error_description")?.let {
            throw IllegalStateException("TIDAL отказал в авторизации: $it")
        }
        val code = callback.getQueryParameter("code")
            ?: throw IllegalStateException("В ответе TIDAL нет кода авторизации.")
        val verifier = prefs.getString("verifier", null)
            ?: throw IllegalStateException("PKCE verifier не найден. Запусти вход заново.")
        val clientId = prefs.getString("client_id", null)
            ?: throw IllegalStateException("Client ID не найден.")

        val form = listOf(
            "grant_type" to "authorization_code",
            "client_id" to clientId,
            "code" to code,
            "redirect_uri" to REDIRECT_URI,
            "code_verifier" to verifier
        ).joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }

        val response = request(TOKEN_URL, "POST", form, null, true)
        val json = JSONObject(response)
        accessToken = json.optString("access_token").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("TIDAL не вернул access_token: $response")
        prefs.edit().remove("verifier").remove("state").apply()
        return "Вход выполнен. Токен получен."
    }

    fun inspectLink(link: String): String {
        val token = accessToken ?: throw IllegalStateException("Сначала войди через TIDAL.")
        val uri = Uri.parse(link.trim())
        val parts = uri.pathSegments
        val typeIndex = parts.indexOfFirst { it == "track" || it == "album" }
        if (typeIndex < 0 || typeIndex + 1 >= parts.size) {
            throw IllegalArgumentException("Поддерживаются ссылки TIDAL на track и album.")
        }
        val type = parts[typeIndex]
        val id = parts[typeIndex + 1].substringBefore("?")
        if (!id.matches(Regex("[A-Za-z0-9_-]+"))) {
            throw IllegalArgumentException("Не удалось определить ID трека или альбома.")
        }
        val endpoint = "$API_BASE/${if (type == "track") "tracks" else "albums"}/${enc(id)}?countryCode=UA"
        val body = request(endpoint, "GET", null, token, false)
        val root = JSONObject(body)
        val data = root.optJSONObject("data") ?: throw IllegalStateException("TIDAL вернул неожиданный ответ: $body")
        val attrs = data.optJSONObject("attributes") ?: JSONObject()
        val title = attrs.optString("title", "Без названия")
        val subtitle = when (type) {
            "track" -> attrs.optString("duration", "")
            else -> attrs.optString("numberOfItems", "")
        }
        return buildString {
            append(if (type == "track") "Трек: " else "Альбом: ")
            append(title)
            if (subtitle.isNotBlank()) append("\nДополнительно: $subtitle")
            append("\nID: $id")
            append("\n\nОтвет API:\n")
            append(data.toString(2))
        }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    private fun request(url: String, method: String, body: String?, token: String?, form: Boolean): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/vnd.api+json, application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    if (form) "application/x-www-form-urlencoded" else "application/json"
                )
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("TIDAL API HTTP $status: $response")
            }
            return response
        } finally {
            connection.disconnect()
        }
    }
}
