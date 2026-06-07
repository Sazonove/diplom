package com.example.diplom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diplom.data.ApiException
import com.example.diplom.data.MeResponse
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.apiCall
import kotlinx.coroutines.launch

@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var me by remember { mutableStateOf<MeResponse?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val r = apiCall { RemoteApi.api.me() }
        r.onSuccess { me = it }.onFailure { error = (it as? ApiException)?.message ?: it.message }
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Подписка", style = MaterialTheme.typography.headlineSmall)
        if (loading) {
            CircularProgressIndicator()
            return@Column
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        me?.let { m ->
            Text("Премиум: ${if (m.user.hasPremium) "активен" else "нет"}")
            m.user.premiumUntil?.let { Text("До: $it") }
        }
        Text(
            "Демо-премиум на 30 дней без оплаты. Часть программ откроется после активации.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        if (busy) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        val r = apiCall { RemoteApi.api.premiumDemo() }
                        busy = false
                        r.onSuccess {
                            val m = apiCall { RemoteApi.api.me() }
                            m.onSuccess { me = it }
                        }.onFailure {
                            error = (it as? ApiException)?.message ?: it.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Демо-премиум на 30 дней")
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}
