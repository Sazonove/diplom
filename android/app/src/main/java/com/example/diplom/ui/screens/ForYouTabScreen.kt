package com.example.diplom.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.ui.components.PremiumOfferDialog
import com.example.diplom.data.ApiException
import com.example.diplom.data.ForYouResponse
import com.example.diplom.data.RecommendedProgramDto
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.apiCall

@Composable
fun ForYouTabScreen(modifier: Modifier = Modifier, rootNav: NavController) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<ForYouResponse?>(null) }

    LaunchedEffect(Unit) {
        val r = apiCall { RemoteApi.api.forYou() }
        r.onSuccess { data = it }.onFailure { error = (it as? ApiException)?.message ?: it.message }
        loading = false
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Для вас", style = MaterialTheme.typography.headlineSmall)
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            data == null -> Text("Нет данных", style = MaterialTheme.typography.bodyMedium)
            data!!.needsSurvey == true -> {
                Text(
                    data!!.message ?: "Завершите анкету после входа — тогда появятся персональные рекомендации.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> {
                val d = data!!
                Text(
                    d.goalLabel ?: "Цель",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(d.summary ?: "", style = MaterialTheme.typography.bodyLarge)
                d.tips?.forEach { tip ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(tip, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Рекомендованные программы", style = MaterialTheme.typography.titleSmall)
                val rec = d.recommendedPrograms.orEmpty()
                if (rec.isEmpty()) {
                    Text("Пока нет программ в подборке.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.height(236.dp),
                    ) {
                        items(rec, key = { it.id }) { p ->
                            RecommendedProgramCard(p, rootNav)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { rootNav.navigate("programs") }, Modifier.fillMaxWidth()) {
                    Text("Все программы")
                }
            }
        }
    }
}

@Composable
private fun RecommendedProgramCard(p: RecommendedProgramDto, rootNav: NavController) {
    var showPremiumOffer by remember { mutableStateOf(false) }

    Card(
        Modifier
            .width(220.dp)
            .clickable {
                if (p.locked) showPremiumOffer = true
                else rootNav.navigate("program/${p.id}")
            },
    ) {
        Column {
            val url = p.coverImageUrl
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        "Программа",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (p.isPrimary) {
                    Text("Основная", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(p.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                    }
                    if (p.locked) {
                        Text(
                            "👑",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                p.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Text(
                    if (p.locked) "По подписке" else "Нажмите, чтобы открыть",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }

    if (showPremiumOffer) {
        PremiumOfferDialog(
            onDismiss = { showPremiumOffer = false },
            onGoSubscribe = {
                showPremiumOffer = false
                rootNav.navigate("subscription")
            },
        )
    }
}
