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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.diplom.DiplomApplication
import com.example.diplom.data.ApiException
import com.example.diplom.data.LoginRequest
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.Session
import com.example.diplom.data.apiCall
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    app: DiplomApplication,
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Вход")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
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
                    loading = true
                    scope.launch {
                        val result = apiCall {
                            RemoteApi.api.login(LoginRequest(email.trim(), password))
                        }
                        loading = false
                        result.onSuccess { resp ->
                            Session.token = resp.token
                            app.tokenStore.setToken(resp.token)
                            onLoggedIn()
                        }.onFailure { e ->
                            error = (e as? ApiException)?.message ?: e.message ?: "Ошибка"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Войти")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
                Text("Регистрация")
            }
        }
    }
}
