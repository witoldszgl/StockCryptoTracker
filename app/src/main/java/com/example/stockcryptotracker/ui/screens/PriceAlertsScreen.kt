package com.example.stockcryptotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockcryptotracker.MainActivity
import com.example.stockcryptotracker.data.room.PriceAlert
import com.example.stockcryptotracker.viewmodel.PriceAlertViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertsScreen(
    viewModel: PriceAlertViewModel = viewModel()
) {
    val allAlerts by viewModel.filteredAlerts.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle success or error messages
    LaunchedEffect(successMessage, error) {
        when {
            successMessage != null -> {
                snackbarHostState.showSnackbar(message = successMessage ?: "Success")
                viewModel.clearMessages()
            }
            error != null -> {
                snackbarHostState.showSnackbar(message = error ?: "Error occurred")
                viewModel.clearMessages()
            }
        }
    }
    
    // Dialog state for confirming alert deletion
    var showDeleteDialog by remember { mutableStateOf(false) }
    var alertToDelete by remember { mutableStateOf<PriceAlert?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Price Alerts") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Add test notification button
                    val context = LocalContext.current
                    IconButton(
                        onClick = { 
                            val mainActivity = context as? MainActivity
                            mainActivity?.sendTestNotification()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Test Notification"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter tabs
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                PriceAlertViewModel.AlertFilter.values().forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = currentFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PriceAlertViewModel.AlertFilter.values().size
                        ),
                        label = {
                            Text(
                                text = when(filter) {
                                    PriceAlertViewModel.AlertFilter.ALL -> "All"
                                    PriceAlertViewModel.AlertFilter.CRYPTO -> "Crypto"
                                    PriceAlertViewModel.AlertFilter.STOCKS -> "Stocks"
                                    PriceAlertViewModel.AlertFilter.ACTIVE -> "Active"
                                }
                            )
                        }
                    )
                }
            }
            
            // Alerts list
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator()
                    }
                    allAlerts.isEmpty() -> {
                        Text(
                            text = "No price alerts set.\nAdd alerts from the portfolio screen.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(allAlerts) { alert ->
                                AlertListItem(
                                    alert = alert,
                                    onToggleActive = { isActive ->
                                        viewModel.toggleAlertActive(alert.id, isActive)
                                    },
                                    onDelete = {
                                        alertToDelete = alert
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && alertToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                alertToDelete = null
            },
            title = { Text("Delete Alert") },
            text = { 
                Text(
                    "Are you sure you want to delete the price alert for ${alertToDelete?.assetSymbol}?" 
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alertToDelete?.let { viewModel.deleteAlert(it.id) }
                        showDeleteDialog = false
                        alertToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        alertToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AlertListItem(
    alert: PriceAlert,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Asset info column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = alert.assetName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Asset type badge
                        Text(
                            text = if (alert.isCrypto) "CRYPTO" else "STOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    
                    Text(
                        text = alert.assetSymbol,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Price condition
                    val direction = if (alert.isAboveTarget) "rises above" else "falls below"
                    Text(
                        text = "Alert when price $direction ${formatAlertPrice(alert.targetPrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Date created
                    Text(
                        text = "Created: ${formatDate(alert.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Controls column
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Switch(
                        checked = alert.isActive,
                        onCheckedChange = onToggleActive
                    )
                    
                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete alert",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun formatAlertPrice(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 