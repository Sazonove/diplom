package com.example.diplom.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.diplom.data.CatalogWorkoutItemDto
import com.example.diplom.data.MeResponse
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.WorkoutCatalogResponse
import com.example.diplom.data.apiCall

@Composable
fun TrainingsHomeScreen(
    modifier: Modifier = Modifier,
    rootNav: NavController,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var me by remember { mutableStateOf<MeResponse?>(null) }
    var streak by remember { mutableIntStateOf(0) }
    var catalog by remember { mutableStateOf<WorkoutCatalogResponse?>(null) }

    LaunchedEffect(Unit) {
        val m = apiCall { RemoteApi.api.me() }
        val s = apiCall { RemoteApi.api.streak() }
        val c = apiCall { RemoteApi.api.workoutCatalog() }
        m.onSuccess { me = it }.onFailure { error = (it as? ApiException)?.message ?: it.message }
        s.onSuccess { streak = it.streak }
        c.onSuccess { catalog = it }.onFailure {
            if (error == null) error = (it as? ApiException)?.message ?: it.message
        }
        loading = false
    }

    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Тренировки", style = MaterialTheme.typography.headlineSmall)
        }
        if (loading) {
            item { CircularProgressIndicator() }
            return@LazyColumn
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item {
            Card(colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Серия: $streak дн.", style = MaterialTheme.typography.titleMedium)
                    me?.assignment?.let {
                        Text("Текущая программа: ${it.programTitle ?: it.programId}", style = MaterialTheme.typography.bodyMedium)
                    } ?: Text("Программа пока не назначена", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text(
                "Выберите раздел и тренировку — откроется список упражнений.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val groups = catalog?.groups.orEmpty()
        if (groups.isEmpty() && !loading) {
            item { Text("Нет доступных тренировок.", style = MaterialTheme.typography.bodyMedium) }
        }
        groups.forEach { g ->
            item(key = "head-${g.bodyFocus}") {
                Text(
                    g.label ?: g.bodyFocus,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(g.items, key = { it.programDayId }) { row ->
                CatalogWorkoutCard(row, rootNav)
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Персональный подбор и каталог программ — вкладка «Для вас».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CatalogWorkoutCard(row: CatalogWorkoutItemDto, rootNav: NavController) {
    var showPremiumOffer by remember { mutableStateOf(false) }

    Card(
        Modifier
            .fillMaxWidth()
            .clickable {
                if (row.locked) showPremiumOffer = true
                else rootNav.navigate("workout/${row.programId}/${row.programDayId}")
            },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val img = row.coverImageUrl
            if (!img.isNullOrBlank()) {
                AsyncImage(
                    model = img,
                    contentDescription = null,
                    modifier = Modifier
                        .width(96.dp)
                        .height(72.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(row.programTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    row.workoutTitle?.takeIf { it.isNotBlank() }
                        ?: row.dayTitle?.takeIf { it.isNotBlank() }
                        ?: "Тренировка",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (row.locked) "По подписке · ${row.exerciseCount} упражнений"
                    else "${row.exerciseCount} упражнений",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (row.locked) {
                Text(
                    "👑",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 4.dp),
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
