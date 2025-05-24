package com.example.stockcryptotracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Banner informujący o źródle danych (mock vs zapisane)
 */
@Composable
fun DataSourceBanner(
    isVisible: Boolean,
    isUsingMockData: Boolean,
    isUsingSavedData: Boolean,
    onRefreshClick: () -> Unit
) {
    if (isVisible) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isUsingSavedData -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isUsingSavedData -> Icons.Default.Info
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when {
                            isUsingSavedData -> "Używam zapisanych danych"
                            else -> "Używam danych demonstracyjnych"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isUsingSavedData -> "Limit API został przekroczony. Pokazuję ostatnio pobrane dane."
                            else -> "Limit API został przekroczony lub wystąpił problem z połączeniem."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = onRefreshClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Odśwież",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(text = "Spróbuj ponownie")
                }
            }
        }
    }
} 