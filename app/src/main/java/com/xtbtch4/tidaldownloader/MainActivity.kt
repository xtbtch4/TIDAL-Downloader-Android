package com.xtbtch4.tidaldownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TidalDownloaderScreen() }
    }
}

@Composable
private fun TidalDownloaderScreen() {
    val links = remember { mutableStateListOf<String>() }
    var input by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf("Lossless") }
    var menuOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Добавь ссылку на альбом, трек или плейлист TIDAL.") }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("TIDAL Downloader", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Android • версия 0.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ссылка TIDAL", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://tidal.com/browse/album/...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Качество: ")
                            TextButton(onClick = { menuOpen = true }) { Text(quality) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                listOf("High", "Lossless", "Hi-Res").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { quality = option; menuOpen = false })
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Button(onClick = {
                                val candidate = input.trim()
                                if (candidate.startsWith("https://") &&
                                    (candidate.contains("tidal.com/") || candidate.contains("listen.tidal.com/"))) {
                                    if (!links.contains(candidate)) links.add(candidate)
                                    input = ""
                                    message = "Ссылка добавлена в список."
                                } else {
                                    message = "Вставь корректную HTTPS-ссылку TIDAL."
                                }
                            }) { Text("Добавить") }
                        }
                    }
                }
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Очередь", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("${links.size} ссылок")
                    TextButton(onClick = { links.clear(); message = "Очередь очищена." }) { Text("Очистить") }
                }
                if (links.isEmpty()) {
                    Text("Очередь пока пуста.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(links) { index, link ->
                            Card {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${index + 1}. $link", modifier = Modifier.weight(1f), maxLines = 2)
                                    TextButton(onClick = { links.remove(link) }) { Text("Удалить") }
                                }
                            }
                        }
                    }
                }
                Text(
                    "Начальная версия интерфейса: ссылки добавляются в очередь, но загрузка музыки пока не реализована. Подключение возможно только через разрешённые способы доступа к контенту TIDAL; обход DRM не поддерживается.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
