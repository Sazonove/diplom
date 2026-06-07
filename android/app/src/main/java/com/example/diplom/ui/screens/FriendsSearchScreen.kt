package com.example.diplom.ui.screens

import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.diplom.data.ApiException
import com.example.diplom.data.FriendIncomingRequestDto
import com.example.diplom.data.FriendSearchResponse
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.SendFriendRequestBody
import com.example.diplom.data.apiAbsoluteUrl
import com.example.diplom.data.apiCall
import kotlinx.coroutines.launch

@Composable
fun FriendsSearchScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    var emailQuery by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<FriendSearchResponse?>(null) }
    var incoming by remember { mutableStateOf<List<FriendIncomingRequestDto>>(emptyList()) }
    var loadingIncoming by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadIncoming() {
        scope.launch {
            loadingIncoming = true
            val r = apiCall { RemoteApi.api.friendsIncoming() }
            r.onSuccess { incoming = it.requests }.onFailure {
                if (error == null) error = (it as? ApiException)?.message ?: it.message
            }
            loadingIncoming = false
        }
    }

    LaunchedEffect(Unit) { loadIncoming() }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) { Text("Назад") }
            Text("Друзья", style = MaterialTheme.typography.headlineSmall)
        }

        OutlinedTextField(
            value = emailQuery,
            onValueChange = { emailQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Email пользователя") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Button(
            onClick = {
                val q = emailQuery.trim().lowercase()
                if (q.isBlank()) {
                    error = "Введите email"
                    return@Button
                }
                error = null
                searching = true
                scope.launch {
                    val r = apiCall { RemoteApi.api.friendsSearch(q) }
                    searching = false
                    r.onSuccess { searchResult = it }.onFailure {
                        error = (it as? ApiException)?.message ?: it.message
                        searchResult = null
                    }
                }
            },
            enabled = !searching,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (searching) {
                CircularProgressIndicator(
                    Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Найти")
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        searchResult?.let { res ->
            SearchResultBlock(
                res = res,
                sending = sending,
                onSendRequest = { body ->
                    sending = true
                    error = null
                    scope.launch {
                        val r = apiCall { RemoteApi.api.friendsSendRequest(body) }
                        sending = false
                        r.onSuccess {
                            val q = emailQuery.trim().lowercase()
                            if (q.isNotBlank()) {
                                val refresh = apiCall { RemoteApi.api.friendsSearch(q) }
                                refresh.onSuccess { searchResult = it }
                            }
                            loadIncoming()
                        }.onFailure {
                            error = (it as? ApiException)?.message ?: it.message
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("Входящие заявки", style = MaterialTheme.typography.titleMedium)
        when {
            loadingIncoming -> CircularProgressIndicator()
            incoming.isEmpty() -> Text(
                "Нет входящих заявок",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    incoming.forEach { req ->
                        IncomingRequestRow(
                            req = req,
                            onAccept = {
                                scope.launch {
                                    val r = apiCall { RemoteApi.api.friendsAcceptRequest(req.id) }
                                    r.onSuccess { loadIncoming() }.onFailure {
                                        error = (it as? ApiException)?.message ?: it.message
                                    }
                                }
                            },
                            onDecline = {
                                scope.launch {
                                    val r = apiCall { RemoteApi.api.friendsDeclineRequest(req.id) }
                                    r.onSuccess { loadIncoming() }.onFailure {
                                        error = (it as? ApiException)?.message ?: it.message
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultBlock(
    res: FriendSearchResponse,
    sending: Boolean,
    onSendRequest: (SendFriendRequestBody) -> Unit,
) {
    val rel = res.relationship
    when (rel) {
        "NOT_FOUND" -> Text("Пользователь не найден.", style = MaterialTheme.typography.bodyMedium)
        "SELF" -> Text("Это ваш email.", style = MaterialTheme.typography.bodyMedium)
        else -> {
            val u = res.user ?: return
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val avatar = apiAbsoluteUrl(u.avatarUrl)
                    if (avatar != null) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(u.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(u.email, style = MaterialTheme.typography.bodySmall)
                        when (rel) {
                            "FRIEND" -> Text("Уже в друзьях", style = MaterialTheme.typography.labelMedium)
                            "OUTGOING_PENDING" -> Text("Заявка отправлена", style = MaterialTheme.typography.labelMedium)
                            "INCOMING_PENDING" -> Text(
                                "Хочет дружить с вами — примите ниже во входящих",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            "NONE" -> { }
                        }
                    }
                    when (rel) {
                        "NONE" -> {
                            Button(
                                onClick = {
                                    onSendRequest(SendFriendRequestBody(toUserId = u.id))
                                },
                                enabled = !sending,
                            ) { Text("В друзья") }
                        }
                        "INCOMING_PENDING" -> {
                            Text(
                                "↓",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        else -> { }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestRow(
    req: FriendIncomingRequestDto,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val u = req.from
            val avatar = apiAbsoluteUrl(u.avatarUrl)
            if (avatar != null) {
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(u.displayName, style = MaterialTheme.typography.titleSmall)
                Text(u.email, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onDecline) { Text("Отклонить") }
            Spacer(Modifier.width(4.dp))
            Button(onClick = onAccept) { Text("Принять") }
        }
    }
}
