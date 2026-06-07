package com.example.diplom.ui

import com.example.diplom.DiplomApplication
import com.example.diplom.data.PendingSurveyData
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.SurveyRequest
import com.example.diplom.data.apiCall

sealed class AfterAuthDestination {
    data object Main : AfterAuthDestination()
    data class Survey(val prefill: PendingSurveyData?) : AfterAuthDestination()
}

/** После логина/регистрации: анкета на сервере уже есть → главная; иначе пробуем отправить данные с онбординга. */
suspend fun resolveDestinationAfterAuth(app: DiplomApplication, surveyCompletedAt: String?): AfterAuthDestination {
    if (surveyCompletedAt != null) return AfterAuthDestination.Main
    val pending = app.onboardingStore.getPendingSurvey()
    if (pending == null) return AfterAuthDestination.Survey(null)
    val r = apiCall {
        RemoteApi.api.submitSurvey(
            SurveyRequest(
                heightCm = pending.heightCm,
                weightKg = pending.weightKg,
                age = pending.age,
                sex = pending.sex,
                experienceLevel = pending.experienceLevel,
                trainingGoal = pending.trainingGoal ?: "GENERAL_FITNESS",
                gymAccess = pending.gymAccess,
            ),
        )
    }
    return if (r.isSuccess) {
        app.onboardingStore.clearPendingSurvey()
        AfterAuthDestination.Main
    } else {
        AfterAuthDestination.Survey(pending)
    }
}
