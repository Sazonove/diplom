package com.example.diplom.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PremiumOfferDialog(
    onDismiss: () -> Unit,
    onGoSubscribe: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Премиум-программа") },
        text = {
            Text(
                "Эта программа доступна по подписке. Оформите подписку, чтобы открыть все дни, тренировки и материалы.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onGoSubscribe) { Text("К подписке") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Позже") }
        },
    )
}
