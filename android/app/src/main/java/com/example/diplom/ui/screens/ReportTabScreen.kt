package com.example.diplom.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.diplom.data.ApiException
import com.example.diplom.data.FriendFeedItemDto
import com.example.diplom.data.FriendLeaderboardRowDto
import com.example.diplom.data.HistoryItemDto
import com.example.diplom.data.MeResponse
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.WeightLogItem
import com.example.diplom.data.apiAbsoluteUrl
import com.example.diplom.data.apiCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

@Composable
fun ReportTabScreen(modifier: Modifier = Modifier, rootNav: NavController) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var me by remember { mutableStateOf<MeResponse?>(null) }
    var history by remember { mutableStateOf<List<HistoryItemDto>>(emptyList()) }
    var weights by remember { mutableStateOf<List<WeightLogItem>>(emptyList()) }
    var leaderboard by remember { mutableStateOf<List<FriendLeaderboardRowDto>>(emptyList()) }
    var feed by remember { mutableStateOf<List<FriendFeedItemDto>>(emptyList()) }
    var lbByWorkouts by remember { mutableStateOf(true) }
    val navEntry by rootNav.currentBackStackEntryAsState()

    LaunchedEffect(Unit) {
        val m = apiCall { RemoteApi.api.me() }
        val h = apiCall { RemoteApi.api.weightHistory() }
        val hi = apiCall { RemoteApi.api.history() }
        m.onSuccess { me = it }.onFailure { error = (it as? ApiException)?.message ?: it.message }
        h.onSuccess { weights = it.items }.onFailure { if (error == null) error = (it as? ApiException)?.message }
        hi.onSuccess { history = it.items }
        loading = false
    }

    LaunchedEffect(navEntry?.destination?.route, lbByWorkouts) {
        val by = if (lbByWorkouts) "workouts" else "maxStreak"
        val lb = apiCall { RemoteApi.api.friendsLeaderboard(by) }
        lb.onSuccess { leaderboard = it.items }
        val fd = apiCall { RemoteApi.api.friendsFeed() }
        fd.onSuccess { feed = it.items }
    }

    val profile = me?.profile
    val heightM = (profile?.heightCm ?: 0) / 100.0
    val bmi = if (heightM > 0 && profile?.weightKg != null) profile.weightKg / (heightM * heightM) else null

    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Отчёт", style = MaterialTheme.typography.headlineSmall)
        }
        if (loading) {
            item { CircularProgressIndicator() }
            return@LazyColumn
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item {
            val bmiAccent = bmi?.let { bmiAccentColor(it) }
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        bmiAccent == null -> MaterialTheme.colorScheme.surface
                        else -> bmiAccent.copy(alpha = 0.12f)
                    },
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("ИМТ", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (bmi != null) String.format(Locale.US, "%.1f", bmi) else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = bmiAccent ?: MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        when {
                            bmi == null -> "Заполните рост и вес в профиле"
                            bmi < 18.5 -> "Ниже нормы"
                            bmi < 25 -> "Норма"
                            bmi < 30 -> "Избыточный вес"
                            else -> "Ожирение"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bmi == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            bmiAccentColor(bmi)
                        },
                    )
                }
            }
        }
        item {
            Text("Динамика веса", style = MaterialTheme.typography.titleMedium)
            Text("Ось Y — вес (кг), ось X — дата", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            WeightLineChart(weights = weights)
        }
        item {
            Text("Топ друзей", style = MaterialTheme.typography.titleMedium)
            Text(
                "Вы и ваши друзья",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = lbByWorkouts,
                    onClick = { lbByWorkouts = true },
                    label = { Text("По тренировкам") },
                )
                FilterChip(
                    selected = !lbByWorkouts,
                    onClick = { lbByWorkouts = false },
                    label = { Text("По серии") },
                )
            }
            Spacer(Modifier.height(8.dp))
            if (leaderboard.isEmpty()) {
                Text(
                    "Не удалось загрузить рейтинг.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    leaderboard.forEachIndexed { idx, row ->
                        val openFriend = !row.isMe
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (openFriend) {
                                        Modifier.clickable { rootNav.navigate("friendProfile/${row.user.id}") }
                                    } else {
                                        Modifier
                                    },
                                ),
                            colors = if (row.isMe) {
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                )
                            } else {
                                CardDefaults.cardColors()
                            },
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "${idx + 1}.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(28.dp),
                                )
                                val av = apiAbsoluteUrl(row.user.avatarUrl)
                                if (av != null) {
                                    AsyncImage(
                                        model = av,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (row.isMe) "${row.user.displayName} (вы)" else row.user.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        if (lbByWorkouts) {
                                            "Тренировок: ${row.workoutCount} · Серия: ${row.maxStreakDays} дн."
                                        } else {
                                            "Серия: ${row.maxStreakDays} дн. · Тренировок: ${row.workoutCount}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Активность друзей", style = MaterialTheme.typography.titleMedium)
            if (feed.isEmpty()) {
                Text(
                    "Пока нет записей о тренировках друзей.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    feed.forEach { ev ->
                        Text(
                            "${ev.friendDisplayName} закончил тренировку «${ev.workoutTitle}» ${formatRelativePastRu(ev.completedAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("История тренировок", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { rootNav.navigate("workoutHistory") }) { Text("Все") }
            }
        }
        if (history.isEmpty()) {
            item {
                Text("Пока нет завершённых тренировок.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(history.take(5), key = { it.id }) { row ->
            WorkoutHistoryItemCard(row)
        }
    }
}

/** Норма — зелёный; пограничные зоны — жёлтый; выраженный недостаток веса или ожирение — красный. */
private fun bmiAccentColor(bmi: Double): Color = when {
    bmi < 17.0 || bmi >= 30.0 -> Color(0xFFC62828)
    bmi < 18.5 || bmi >= 25.0 -> Color(0xFFF57F17)
    else -> Color(0xFF2E7D32)
}

private fun formatRelativePastRu(iso: String): String {
    val ms = parseRecordedAtMillis(iso)
    if (ms <= 0L) return iso
    val diff = System.currentTimeMillis() - ms
    if (diff < 60_000L) return "только что"
    val minutes = diff / 60_000L
    if (minutes < 60L) return "$minutes мин назад"
    val hours = minutes / 60L
    if (hours < 24L) return "$hours ч назад"
    val days = hours / 24L
    return "$days дн назад"
}

private fun parseRecordedAtMillis(iso: String): Long {
    val trimmed = iso.trim()
    val parsers = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to true,
        "yyyy-MM-dd'T'HH:mm:ss'Z'" to true,
        "yyyy-MM-dd'T'HH:mm:ss.SSSX" to false,
    )
    for ((pattern, utc) in parsers) {
        try {
            val fmt = SimpleDateFormat(pattern, Locale.US)
            if (utc) fmt.timeZone = TimeZone.getTimeZone("UTC")
            val p = fmt.parse(trimmed) ?: continue
            return p.time
        } catch (_: Exception) {
            continue
        }
    }
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(trimmed.take(10))?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
}

private fun formatDateShort(millis: Long): String {
    if (millis <= 0L) return "—"
    return SimpleDateFormat("dd.MM.yy", Locale.forLanguageTag("ru-RU")).format(Date(millis))
}

@Composable
private fun WeightLineChart(weights: List<WeightLogItem>) {
    val sorted = remember(weights) {
        weights.sortedBy { parseRecordedAtMillis(it.recordedAt) }
    }
    if (sorted.size < 2) {
        Text(
            if (sorted.isEmpty()) "Нет записей веса. Добавьте вес в профиле."
            else "Добавьте ещё хотя бы одну запись веса для графика.",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    val values = sorted.map { it.weightKg.toFloat() }
    var minW = values.minOrNull() ?: 0f
    var maxW = values.maxOrNull() ?: 1f
    val rawSpan = max(maxW - minW, 0.5f)
    minW -= rawSpan * 0.08f
    maxW += rawSpan * 0.08f
    val span = max(maxW - minW, 0.1f)
    val yTop = maxW
    val yMid = (maxW + minW) / 2f
    val yBottom = minW
    val labelStyle = MaterialTheme.typography.labelSmall
    val colorLine = MaterialTheme.colorScheme.primary
    val colorGrid = Color.Gray.copy(alpha = 0.35f)

    val t0 = parseRecordedAtMillis(sorted.first().recordedAt)
    val tLast = parseRecordedAtMillis(sorted.last().recordedAt)
    val timeSpan = max(tLast - t0, 1L)
    val midTime = t0 + timeSpan / 2

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(
            Modifier
                .width(48.dp)
                .height(188.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            Text(String.format(Locale.US, "%.1f", yTop), style = labelStyle)
            Text(String.format(Locale.US, "%.1f", yMid), style = labelStyle)
            Text(String.format(Locale.US, "%.1f", yBottom), style = labelStyle)
        }
        Column(Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 4.dp),
            ) {
                val plotLeft = 4.dp.toPx()
                val plotRight = size.width - 4.dp.toPx()
                val plotTop = 6.dp.toPx()
                val plotBottom = size.height - 6.dp.toPx()
                for (i in 0..3) {
                    val y = plotTop + (plotBottom - plotTop) * i / 3f
                    drawLine(
                        color = colorGrid,
                        start = Offset(plotLeft, y),
                        end = Offset(plotRight, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                fun xFor(index: Int, item: WeightLogItem): Float {
                    val useTime = sorted.size >= 2 && tLast > t0
                    return if (useTime) {
                        val t = parseRecordedAtMillis(item.recordedAt)
                        plotLeft + (plotRight - plotLeft) * ((t - t0).toFloat() / timeSpan)
                    } else {
                        plotLeft + (plotRight - plotLeft) * index / (sorted.size - 1).coerceAtLeast(1)
                    }
                }
                fun yFor(w: Double): Float {
                    val yNorm = (w.toFloat() - minW) / span
                    return plotBottom - (plotBottom - plotTop) * yNorm
                }
                val path = Path()
                sorted.forEachIndexed { i, item ->
                    val x = xFor(i, item)
                    val y = yFor(item.weightKg)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = colorLine, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                sorted.forEachIndexed { i, item ->
                    val x = xFor(i, item)
                    val y = yFor(item.weightKg)
                    drawCircle(color = colorLine, radius = 5.dp.toPx(), center = Offset(x, y))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatDateShort(t0), style = labelStyle)
                if (sorted.size > 2) Text(formatDateShort(midTime), style = labelStyle)
                Text(formatDateShort(tLast), style = labelStyle)
            }
            Text(
                "Δ ${String.format(Locale.US, "%.1f", sorted.first().weightKg)} → ${String.format(Locale.US, "%.1f", sorted.last().weightKg)} кг",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}
