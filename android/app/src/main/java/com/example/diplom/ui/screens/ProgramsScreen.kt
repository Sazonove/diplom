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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.example.diplom.ui.components.PremiumOfferDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.diplom.data.ApiException
import com.example.diplom.data.ProgramSummaryDto
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.apiCall

@Composable
fun ProgramsScreen(
    onOpenProgram: (String) -> Unit,
    onBack: () -> Unit,
    onSubscribe: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<ProgramSummaryDto>>(emptyList()) }
    var showPremiumOffer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val r = apiCall { RemoteApi.api.programs() }
        r.onSuccess { items = it.programs }.onFailure { error = (it as? ApiException)?.message ?: it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Программы", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.padding(8.dp))
        if (loading) {
            CircularProgressIndicator()
            return@Column
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(items, key = { it.id }) { p ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (p.locked) showPremiumOffer = true
                            else onOpenProgram(p.id)
                        },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val cover = p.coverImageUrl
                        if (!cover.isNullOrBlank()) {
                            AsyncImage(
                                model = cover,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(64.dp),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Column(
                            Modifier
                                .padding(start = 12.dp)
                                .weight(1f),
                        ) {
                            Text(p.title, style = MaterialTheme.typography.titleMedium)
                            p.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(
                                if (p.locked) "Премиум" else "Дней: ${p.dayCount}",
                                style = MaterialTheme.typography.labelMedium,
                            )
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
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }

    if (showPremiumOffer) {
        PremiumOfferDialog(
            onDismiss = { showPremiumOffer = false },
            onGoSubscribe = {
                showPremiumOffer = false
                onSubscribe()
            },
        )
    }
}
