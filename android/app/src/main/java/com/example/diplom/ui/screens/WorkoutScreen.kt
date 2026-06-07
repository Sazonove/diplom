package com.example.diplom.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.diplom.data.ApiException
import com.example.diplom.data.ExerciseDto
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.WorkoutCompleteRequest
import com.example.diplom.data.apiAbsoluteUrl
import com.example.diplom.data.apiCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TimerKind { REST, WORK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    programId: String,
    dayId: String,
    onFinished: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var exercises by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }
    var workoutTitle by remember { mutableStateOf("") }
    var idx by remember { mutableIntStateOf(0) }

    var timerKind by remember { mutableStateOf(TimerKind.REST) }
    var timerRunning by remember { mutableStateOf(false) }
    var remainingSec by remember { mutableIntStateOf(0) }
    var phaseTotalSec by remember { mutableIntStateOf(0) }

    val startMs = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(programId, dayId) {
        idx = 0
        timerRunning = false
        remainingSec = 0
        phaseTotalSec = 0
        loading = true
        error = null
        exercises = emptyList()
        val r = apiCall { RemoteApi.api.program(programId) }
        r.onSuccess { resp ->
            val day = resp.program.days.find { it.id == dayId }
            if (day == null) {
                error = "Тренировка не найдена"
            } else {
                workoutTitle = day.title?.takeIf { it.isNotBlank() } ?: resp.program.title
                exercises = day.exercises.sortedBy { it.orderIndex }
            }
        }.onFailure { error = (it as? ApiException)?.message ?: it.message }
        loading = false
    }

    LaunchedEffect(timerRunning) {
        while (timerRunning && remainingSec > 0) {
            delay(1000)
            remainingSec--
            if (remainingSec <= 0) {
                timerRunning = false
                break
            }
        }
    }

    fun resetTimer() {
        timerRunning = false
        remainingSec = 0
        phaseTotalSec = 0
    }

    fun startPhase(kind: TimerKind, ex: ExerciseDto) {
        val total = when (kind) {
            TimerKind.REST -> ex.restSeconds.coerceAtLeast(1)
            TimerKind.WORK -> ex.exerciseSeconds.coerceAtLeast(1)
        }
        timerKind = kind
        phaseTotalSec = total
        remainingSec = total
        timerRunning = true
    }

    fun toggleTimerCircle(ex: ExerciseDto) {
        if (timerRunning) {
            timerRunning = false
            return
        }
        when (timerKind) {
            TimerKind.REST -> startPhase(TimerKind.REST, ex)
            TimerKind.WORK ->
                if (ex.exerciseSeconds > 0) {
                    startPhase(TimerKind.WORK, ex)
                } else {
                    startPhase(TimerKind.REST, ex)
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        workoutTitle.ifBlank { "Тренировка" },
                        maxLines = 1,
                    )
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Выйти")
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    val exForBtn = exercises.getOrNull(idx)
                    if (exForBtn != null) {
                        Button(
                            onClick = {
                                idx += 1
                                resetTimer()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (idx < exercises.lastIndex) {
                                    "Следующее упражнение"
                                } else {
                                    "Завершить тренировку"
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.padding(24.dp))
                return@Column
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (exercises.isEmpty()) {
                if (error == null) {
                    Text("Нет упражнений")
                }
                return@Column
            }

            if (idx >= exercises.size) {
                Text("Готово!", style = MaterialTheme.typography.headlineSmall)
                val dur = ((System.currentTimeMillis() - startMs) / 1000).toInt().coerceAtLeast(1)
                Text(
                    "Длительность: ${dur / 60} мин ${dur % 60} с",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = {
                        scope.launch {
                            val r = apiCall {
                                RemoteApi.api.completeWorkout(
                                    WorkoutCompleteRequest(
                                        programId = programId,
                                        programDayId = dayId,
                                        durationSeconds = dur,
                                    ),
                                )
                            }
                            r.onSuccess { onFinished() }.onFailure {
                                error = (it as? ApiException)?.message ?: it.message
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Сохранить результат") }
                return@Column
            }

            val ex = exercises.getOrNull(idx)
            if (ex == null) {
                Text("Ошибка отображения упражнения", color = MaterialTheme.colorScheme.error)
                return@Column
            }
            val total = exercises.size
            val progress = (idx + 1).toFloat() / total.coerceAtLeast(1).toFloat()

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Упражнение ${idx + 1} из $total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(ex.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${ex.sets} подхода × ${ex.reps} повторов", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Отдых между подходами: ${ex.restSeconds} с",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (ex.exerciseSeconds > 0) {
                        Text(
                            "Работа по таймеру: ${ex.exerciseSeconds} с на подход",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Text("Демонстрация", style = MaterialTheme.typography.titleSmall)
            ExerciseDemoMedia(gifUrl = ex.gifUrl, imageUrl = ex.imageUrl, name = ex.name)

            Text("Таймер", style = MaterialTheme.typography.titleSmall)
            RowFilterTimerKind(
                timerKind = timerKind,
                exerciseSeconds = ex.exerciseSeconds,
                onKindChange = { k ->
                    if (!timerRunning) {
                        timerKind = k
                        remainingSec = 0
                        phaseTotalSec = 0
                    }
                },
            )

            val totalPhase = phaseTotalSec.coerceAtLeast(1)
            val progressCircle =
                if (!timerRunning || remainingSec <= 0) {
                    0f
                } else {
                    (totalPhase - remainingSec).toFloat() / totalPhase.toFloat()
                }

            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(192.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { toggleTimerCircle(ex) },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { progressCircle.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    strokeWidth = 10.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (remainingSec > 0) "$remainingSec" else "—",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            timerRunning -> if (timerKind == TimerKind.REST) "Отдых" else "Упражнение"
                            timerKind == TimerKind.REST -> "Отдых · старт"
                            else -> if (ex.exerciseSeconds > 0) "Работа · старт" else "Отдых (нет секунд работы)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Text(
                "Нажмите на круг — старт или пауза. Режим выберите чипами выше.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RowFilterTimerKind(
    timerKind: TimerKind,
    exerciseSeconds: Int,
    onKindChange: (TimerKind) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = timerKind == TimerKind.REST,
            onClick = { onKindChange(TimerKind.REST) },
            label = { Text("Отдых") },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = timerKind == TimerKind.WORK,
            onClick = { if (exerciseSeconds > 0) onKindChange(TimerKind.WORK) },
            enabled = exerciseSeconds > 0,
            label = { Text("Работа") },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExerciseDemoMedia(gifUrl: String?, imageUrl: String?, name: String) {
    val context = LocalContext.current
    val primary = apiAbsoluteUrl(gifUrl)
    val fallback = apiAbsoluteUrl(imageUrl)
    val url = primary?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }
    if (url.isNullOrBlank()) {
        Text(
            "Для этого упражнения не указан GIF или изображение.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    val request = remember(context, url) {
        ImageRequest.Builder(context).data(url).crossfade(true).build()
    }
    SubcomposeAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = name,
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(MaterialTheme.shapes.medium),
        contentScale = ContentScale.Fit,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(40.dp))
                }
            is AsyncImagePainter.State.Error ->
                Text(
                    "Не удалось загрузить демонстрацию",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            else -> SubcomposeAsyncImageContent()
        }
    }
}
