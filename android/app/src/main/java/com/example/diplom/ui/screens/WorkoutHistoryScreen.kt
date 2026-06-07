package com.example.diplom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diplom.data.ApiException
import com.example.diplom.data.HistoryItemDto
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.apiCall

@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf<List<HistoryItemDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        val hi = apiCall { RemoteApi.api.history() }
        hi.onSuccess { history = it.items }.onFailure { error = (it as? ApiException)?.message ?: it.message }
        loading = false
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Назад") }
        Text("Все тренировки", style = MaterialTheme.typography.headlineSmall)
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            history.isEmpty() -> Text("Пока нет завершённых тренировок.", style = MaterialTheme.typography.bodyMedium)
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history, key = { it.id }) { row ->
                        WorkoutHistoryItemCard(row)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryItemCard(row: HistoryItemDto, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(row.program.title, style = MaterialTheme.typography.titleSmall)
            Text(
                "День ${row.programDay.dayIndex}: ${row.programDay.title ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "${formatDateTimeRuHistory(row.completedAt)} · ${formatDurationHistory(row.durationSeconds)}",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun parseRecordedAtMillisHistory(iso: String): Long {
    val trimmed = iso.trim()
    val parsers = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to true,
        "yyyy-MM-dd'T'HH:mm:ss'Z'" to true,
        "yyyy-MM-dd'T'HH:mm:ss.SSSX" to false,
    )
    for ((pattern, utc) in parsers) {
        try {
            val fmt = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
            if (utc) fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val p = fmt.parse(trimmed) ?: continue
            return p.time
        } catch (_: Exception) {
            continue
        }
    }
    return try {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(trimmed.take(10))?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
}

private fun formatDateTimeRuHistory(iso: String): String {
    val ms = parseRecordedAtMillisHistory(iso)
    if (ms <= 0L) return iso.take(16)
    return java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.forLanguageTag("ru-RU"))
        .format(java.util.Date(ms))
}

private fun formatDurationHistory(sec: Int): String {
    if (sec < 60) return "$sec с"
    val m = sec / 60
    val s = sec % 60
    return if (s == 0) "$m мин" else "$m мин $s с"
}
