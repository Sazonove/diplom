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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diplom.DiplomApplication
import com.example.diplom.data.PendingSurveyData
import kotlinx.coroutines.launch

private const val TOTAL_STEPS = 4

@Composable
fun OnboardingFlow(app: DiplomApplication, onFinished: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var height by rememberSaveable { mutableStateOf("175") }
    var weight by rememberSaveable { mutableStateOf("70") }
    var age by rememberSaveable { mutableStateOf("22") }
    var sex by rememberSaveable { mutableStateOf<String?>(null) }
    var trainingGoal by rememberSaveable { mutableStateOf("GENERAL_FITNESS") }
    var experience by rememberSaveable { mutableStateOf("BEGINNER") }
    var gym by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        LinearProgressIndicator(progress = { (step + 1) / TOTAL_STEPS.toFloat() }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        when (step) {
            0 -> {
                Text("Добро пожаловать", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Это приложение поможет подобрать программу тренировок и следить за прогрессом. " +
                        "Сначала соберём антропометрию и привычки — данные можно будет уточнить в профиле после регистрации.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Далее") }
            }
            1 -> {
                Text("Ваши данные", style = MaterialTheme.typography.headlineSmall)
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
                Text("Пол", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MALE" to "М", "FEMALE" to "Ж", "OTHER" to "Другое").forEach { (k, label) ->
                        FilterChip(
                            selected = sex == k,
                            onClick = { sex = if (sex == k) null else k },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { step = 0 }, modifier = Modifier.weight(1f)) { Text("Назад") }
                    Button(
                        onClick = {
                            val h = height.toIntOrNull()
                            val w = weight.toDoubleOrNull()
                            val a = age.toIntOrNull()
                            if (h != null && w != null && a != null) step = 2
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Далее") }
                }
            }
            2 -> {
                Text("Цель тренировок", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text(
                    "От неё зависит подбор нагрузки и акцентов в программе.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                val goals = listOf(
                    "WEIGHT_LOSS" to "Похудение",
                    "MUSCLE_GAIN" to "Набор массы",
                    "MAINTENANCE" to "Поддержание формы",
                    "ENDURANCE" to "Выносливость",
                    "GENERAL_FITNESS" to "Общая активность",
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { (k, label) ->
                        FilterChip(
                            selected = trainingGoal == k,
                            onClick = { trainingGoal = k },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { step = 1 }, modifier = Modifier.weight(1f)) { Text("Назад") }
                    Button(onClick = { step = 3 }, modifier = Modifier.weight(1f)) { Text("Далее") }
                }
            }
            else -> {
                Text("Тренировки", style = MaterialTheme.typography.headlineSmall)
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
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { step = 2 }, modifier = Modifier.weight(1f)) { Text("Назад") }
                    Button(
                        onClick = {
                            val h = height.toIntOrNull()
                            val w = weight.toDoubleOrNull()
                            val a = age.toIntOrNull()
                            if (h == null || w == null || a == null) return@Button
                            scope.launch {
                                app.onboardingStore.setPendingSurvey(
                                    PendingSurveyData(
                                        heightCm = h,
                                        weightKg = w,
                                        age = a,
                                        sex = sex,
                                        experienceLevel = experience,
                                        trainingGoal = trainingGoal,
                                        gymAccess = gym,
                                    ),
                                )
                                app.onboardingStore.setOnboardingCompleted(true)
                                onFinished()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Начать") }
                }
            }
        }
    }
}
