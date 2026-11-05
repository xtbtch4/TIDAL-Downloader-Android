package com.xtbtch4.tidaldownloader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthIntent(intent)
        setContent { TidalScreen() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.xtbtch4.tidaldownloader" && uri.host == "oauth") {
            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) { TidalApi.finishAuthorization(this@MainActivity, uri) }
                    OAuthUiState.message = result
                    OAuthUiState.connected = true
                } catch (e: Exception) {
                    OAuthUiState.message = e.message ?: "Ошибка авторизации"
                    OAuthUiState.connected = false
                }
            }
        }
    }
}

private object OAuthUiState {
    var message by mutableStateOf("Для работы с каталогом нужен TIDAL Developer Client ID.")
    var connected by mutableStateOf(false)
}

@Composable
private fun TidalScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var clientId by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    MaterialTheme {
        Scaffold { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("TIDAL Downloader", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Android • 0.2.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("1. Подключение TIDAL", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("TIDAL Developer Client ID") },
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (clientId.isBlank()) {
                                    OAuthUiState.message = "Введи Client ID из TIDAL Developer Portal."
                                } else {
                                    val url = TidalApi.createAuthorizationUrl(context, clientId)
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Войти через TIDAL") }
                        Text(
                            if (OAuthUiState.connected) "Подключено" else OAuthUiState.message,
                            color = if (OAuthUiState.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "В Developer Portal зарегистрируй Redirect URI: ${TidalApi.REDIRECT_URI}. Авторизация выполняется OAuth 2.1 + PKCE; пароль приложение не запрашивает.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("2. Проверка ссылки", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = link,
                            onValueChange = { link = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Ссылка на трек или альбом") },
                            placeholder = { Text("https://tidal.com/browse/track/...") },
                            singleLine = true
                        )
                        Button(
                            enabled = !busy && OAuthUiState.connected,
                            onClick = {
                                busy = true
                                result = ""
                                scope.launch {
                                    result = try {
                                        withContext(Dispatchers.IO) { TidalApi.inspectLink(link) }
                                    } catch (e: Exception) {
                                        e.message ?: "Ошибка запроса"
                                    }
                                    busy = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) "Запрос..." else "Получить данные из TIDAL") }
                    }
                }
                if (result.isNotBlank()) {
                    Card {
                        Text(
                            result,
                            Modifier.fillMaxWidth().padding(14.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    "Это первый рабочий этап интеграции: OAuth-вход и чтение метаданных через официальный TIDAL Web API. Загрузка защищённых аудиопотоков и обход DRM не реализованы.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
