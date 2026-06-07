package com.example.diplom.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore("onboarding")

data class PendingSurveyData(
    val heightCm: Int,
    val weightKg: Double,
    val age: Int,
    val sex: String?,
    val experienceLevel: String,
    /** WEIGHT_LOSS, MUSCLE_GAIN, …; null для старых сохранений до обновления */
    val trainingGoal: String? = null,
    val gymAccess: Boolean,
)

class OnboardingStore(private val context: Context) {
    private val gson = Gson()
    private val keyDone = booleanPreferencesKey("onboarding_completed")
    private val keyPending = stringPreferencesKey("pending_survey_json")

    suspend fun isOnboardingCompleted(): Boolean {
        return context.onboardingDataStore.data.map { it[keyDone] == true }.first()
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.onboardingDataStore.edit { it[keyDone] = done }
    }

    suspend fun getPendingSurvey(): PendingSurveyData? {
        val json = context.onboardingDataStore.data.map { it[keyPending] }.first() ?: return null
        return runCatching { gson.fromJson(json, PendingSurveyData::class.java) }.getOrNull()
    }

    suspend fun setPendingSurvey(data: PendingSurveyData?) {
        context.onboardingDataStore.edit { prefs ->
            if (data == null) prefs.remove(keyPending)
            else prefs[keyPending] = gson.toJson(data)
        }
    }

    suspend fun clearPendingSurvey() {
        context.onboardingDataStore.edit { it.remove(keyPending) }
    }
}
