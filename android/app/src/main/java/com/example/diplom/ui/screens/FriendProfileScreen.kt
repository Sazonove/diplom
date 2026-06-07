package com.example.diplom.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.diplom.data.AchievementDto
import com.example.diplom.data.ApiException
import com.example.diplom.data.FriendPublicProfileResponse
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.apiAbsoluteUrl
import com.example.diplom.data.apiCall

@Composable
fun FriendProfileScreen(
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<FriendPublicProfileResponse?>(null) }

    LaunchedEffect(userId) {
        loading = true
        error = null
        val r = apiCall { RemoteApi.api.friendProfile(userId) }
        r.onSuccess { data = it }.onFailure { error = (it as? ApiException)?.message ?: it.message }
        loading = false
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Назад") }
        Text("Профиль друга", style = MaterialTheme.typography.headlineSmall)

        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            data == null -> Text("Нет данных")
            else -> {
                val d = data!!
                val avatar = apiAbsoluteUrl(d.user.avatarUrl)
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (avatar != null) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(d.user.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Тренировок: ${d.workoutCount} · Рекорд серии: ${d.maxStreakDays} дн.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text("Достижения", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    d.achievements.orEmpty().forEach { ach ->
                        FriendAchievementCard(ach)
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendAchievementCard(a: AchievementDto) {
    val unlocked = a.unlocked
    Card(
        modifier = Modifier.width(196.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
        ),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (unlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
            )
            Text(
                a.title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (unlocked) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                a.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 4,
                color = if (unlocked) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                },
            )
        }
    }
}
