package com.example.diplom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diplom.DiplomApplication
import com.example.diplom.data.ApiException
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.SurveyRequest
import com.example.diplom.data.apiCall
import kotlinx.coroutines.launch

@Composable
fun SurveyScreen(app: DiplomApplication, onDone: () -> Unit) {
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("70") }
    var age by remember { mutableStateOf("20") }
    var sex by remember { mutableStateOf<String?>(null) }
    var trainingGoal by remember { mutableStateOf("GENERAL_FITNESS") }
    var experience by remember { mutableStateOf("BEGINNER") }
    var gym by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val p = app.onboardingStore.getPendingSurvey()
        if (p != null) {
            height = p.heightCm.toString()
            weight = p.weightKg.toString()
            age = p.age.toString()
            sex = p.sex
            experience = p.experienceLevel
            trainingGoal = p.trainingGoal ?: "GENERAL_FITNESS"
            gym = p.gymAccess
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text("Анкета")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = height,
            onValueChange = { height = it.filter { ch -> ch.isDigit() } },
            label = { Text("Рост (см)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.replace(',', '.') },
            label = { Text("Вес (кг)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = age,
            onValueChange = { age = it.filter { ch -> ch.isDigit() } },
            label = { Text("Возраст") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text("Пол")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MALE" to "М", "FEMALE" to "Ж", "OTHER" to "Другое").forEach { (k, label) ->
                FilterChip(
                    selected = sex == k,
                    onClick = { sex = if (sex == k) null else k },
                    label = { Text(label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Цель тренировок")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "WEIGHT_LOSS" to "Похудение",
                "MUSCLE_GAIN" to "Набор массы",
                "MAINTENANCE" to "Поддержание формы",
                "ENDURANCE" to "Выносливость",
                "GENERAL_FITNESS" to "Общая активность",
            ).forEach { (k, label) ->
                FilterChip(
                    selected = trainingGoal == k,
                    onClick = { trainingGoal = k },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Опыт")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("BEGINNER" to "Начинающий", "INTERMEDIATE" to "Средний", "ADVANCED" to "Продвинутый").forEach { (k, label) ->
                FilterChip(
                    selected = experience == k,
                    onClick = { experience = k },
                    label = { Text(label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Доступ к залу")
            Spacer(Modifier.weight(1f))
            Switch(checked = gym, onCheckedChange = { gym = it })
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        if (loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    error = null
                    val h = height.toIntOrNull()
                    val w = weight.toDoubleOrNull()
                    val a = age.toIntOrNull()
                    if (h == null || w == null || a == null) {
                        error = "Проверьте числовые поля"
                        return@Button
                    }
                    loading = true
                    scope.launch {
                        val result = apiCall {
                            RemoteApi.api.submitSurvey(
                                SurveyRequest(
                                    heightCm = h,
                                    weightKg = w,
                                    age = a,
                                    sex = sex,
                                    experienceLevel = experience,
                                    trainingGoal = trainingGoal,
                                    gymAccess = gym,
                                ),
                            )
                        }
                        loading = false
                        result.onSuccess {
                            app.onboardingStore.clearPendingSurvey()
                            onDone()
                        }.onFailure { e ->
                            error = (e as? ApiException)?.message ?: e.message ?: "Ошибка"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить и получить программу")
            }
        }
    }
}
