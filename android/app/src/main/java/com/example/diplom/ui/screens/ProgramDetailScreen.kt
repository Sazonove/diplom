package com.example.diplom.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.diplom.ui.components.PremiumOfferDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.diplom.data.ApiException
import com.example.diplom.data.ProgramDayDto
import com.example.diplom.data.ProgramDetailDto
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.apiCall

private fun bodyFocusRu(code: String?): String? {
    if (code == null) return null
    return when (code) {
        "LEGS" -> "Ноги"
        "ARMS" -> "Руки"
        "CHEST" -> "Грудь"
        "BACK" -> "Спина"
        "SHOULDERS" -> "Плечи"
        "CORE" -> "Пресс"
        "FULL_BODY" -> "Всё тело"
        "CARDIO" -> "Кардио"
        "WEIGHT_LOSS" -> "Похудение"
        else -> null
    }
}

@Composable
fun ProgramDetailScreen(
    programId: String,
    onWorkout: (programId: String, dayId: String) -> Unit,
    onBack: () -> Unit,
    onSubscribe: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var program by remember { mutableStateOf<ProgramDetailDto?>(null) }
    var premiumGate by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(programId) {
        premiumGate = false
        error = null
        program = null
        val r = apiCall { RemoteApi.api.program(programId) }
        r.onSuccess { program = it.program }
            .onFailure { e ->
                val ex = e as? ApiException
                if (ex?.httpCode == 403) premiumGate = true
                else error = ex?.message ?: e.message
            }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (loading) {
            CircularProgressIndicator()
            return@Column
        }
        if (premiumGate) {
            Text("👑", style = MaterialTheme.typography.displaySmall)
            Text(
                "Премиум-программа",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Оформите подписку, чтобы открыть эту программу и упражнения.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = { showPremiumDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) { Text("Узнать о подписке") }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
            return@Column
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        program?.let { p ->
            val cover = p.coverImageUrl
            if (!cover.isNullOrBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(p.title, style = MaterialTheme.typography.headlineSmall)
            p.description?.let { Text(it) }
            Spacer(Modifier.padding(8.dp))
            val sectionTitle =
                if (p.days.size <= 1) "Тренировка"
                else "Дни программы"
            Text(sectionTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(p.days, key = { it.id }) { d: ProgramDayDto ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWorkout(p.id, d.id) },
                    ) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            val focus = bodyFocusRu(d.bodyFocus)
                            if (focus != null) {
                                Text(focus, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                d.title?.takeIf { it.isNotBlank() } ?: "Тренировка",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text("${d.exercises.size} упражнений", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }

    if (showPremiumDialog) {
        PremiumOfferDialog(
            onDismiss = { showPremiumDialog = false },
            onGoSubscribe = {
                showPremiumDialog = false
                onSubscribe()
            },
        )
    }
}
