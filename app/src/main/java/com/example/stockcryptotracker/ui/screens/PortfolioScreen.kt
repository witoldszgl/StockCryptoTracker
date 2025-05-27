package com.example.stockcryptotracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockcryptotracker.viewmodel.PortfolioViewModel
import com.example.stockcryptotracker.viewmodel.PortfolioItemUiState
import com.example.stockcryptotracker.viewmodel.PortfolioItemType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = viewModel(),
    onNavigateToDetail: (String, Boolean) -> Unit
) {
    val portfolioItems by viewModel.filteredPortfolioItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val totalValue by viewModel.portfolioValue.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAssetId by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf("") }
    var isAddingCrypto by remember { mutableStateOf(true) }
    
    val cryptoList by viewModel.allCryptoList.collectAsState()
    val stocksList by viewModel.allStocksList.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }
    val icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portfolio") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add to portfolio",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Portfolio Value Card
            PortfolioValueCard(totalValue = totalValue)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tab Row
            PortfolioTabRow(
                selectedTab = selectedTab,
                onTabSelected = viewModel::setSelectedTab
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Portfolio Items List
            if (isLoading) {
                LoadingIndicator()
            } else if (error != null) {
                ErrorMessage(error = error!!)
            } else if (portfolioItems.isEmpty()) {
                EmptyPortfolioMessage()
            } else {
                PortfolioItemsList(
                    items = portfolioItems,
                    onItemClick = { item ->
                        onNavigateToDetail(item.id, item.type == PortfolioItemType.CRYPTO)
                    },
                    onDeleteClick = { item ->
                        viewModel.removePortfolioItem(item.id, item.type)
                    }
                )
            }
        }
        
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add to Portfolio") },
                text = {
                    Column {
                        // Asset type selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { 
                                    isAddingCrypto = true
                                    selectedAssetId = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAddingCrypto) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Crypto")
                            }
                            Button(
                                onClick = { 
                                    isAddingCrypto = false
                                    selectedAssetId = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isAddingCrypto) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Stocks")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Asset selection dropdown
                        Box {
                            OutlinedTextField(
                                value = if (isAddingCrypto) {
                                    cryptoList.find { it.id == selectedAssetId }?.name ?: ""
                                } else {
                                    stocksList.find { it.symbol == selectedAssetId }?.name ?: ""
                                },
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(if (isAddingCrypto) "Select Cryptocurrency" else "Select Stock") },
                                trailingIcon = {
                                    Icon(icon, "dropdown arrow",
                                        Modifier.clickable { expanded = !expanded })
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        textFieldSize = coordinates.size.toSize()
                                    }
                            )
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                            ) {
                                if (isAddingCrypto) {
                                    cryptoList.forEach { crypto ->
                                        DropdownMenuItem(
                                            text = { Text("${crypto.name} (${crypto.symbol})") },
                                            onClick = {
                                                selectedAssetId = crypto.id
                                                expanded = false
                                            }
                                        )
                                    }
                                } else {
                                    stocksList.forEach { stock ->
                                        DropdownMenuItem(
                                            text = { Text("${stock.name} (${stock.symbol})") },
                                            onClick = {
                                                selectedAssetId = stock.symbol
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Quantity input
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = selectedQuantity,
                            onValueChange = { 
                                // Allow only numbers and decimal point
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    selectedQuantity = it
                                }
                            },
                            label = { Text("Quantity") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val quantity = selectedQuantity.toDoubleOrNull() ?: 0.0
                            if (quantity > 0 && selectedAssetId.isNotEmpty()) {
                                if (isAddingCrypto) {
                                    viewModel.addUpdateCryptoPortfolioItem(selectedAssetId, quantity)
                                } else {
                                    viewModel.addUpdateStockPortfolioItem(selectedAssetId, quantity)
                                }
                                showAddDialog = false
                                selectedAssetId = ""
                                selectedQuantity = ""
                                expanded = false
                            }
                        },
                        enabled = selectedAssetId.isNotEmpty() && 
                                 selectedQuantity.isNotEmpty() && 
                                 selectedQuantity.toDoubleOrNull() != null && 
                                 selectedQuantity.toDoubleOrNull()!! > 0
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showAddDialog = false
                            selectedAssetId = ""
                            selectedQuantity = ""
                            expanded = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun PortfolioValueCard(totalValue: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = "Total Portfolio Value",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = totalValue,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search portfolio") },
        singleLine = true
    )
}

@Composable
private fun PortfolioTabRow(
    selectedTab: PortfolioViewModel.PortfolioTab,
    onTabSelected: (PortfolioViewModel.PortfolioTab) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal
    ) {
        PortfolioViewModel.PortfolioTab.values().forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.name) }
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessage(error: String) {
    Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun EmptyPortfolioMessage() {
    Text(
        text = "Your portfolio is empty",
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun PortfolioItemsList(
    items: List<PortfolioItemUiState>,
    onItemClick: (PortfolioItemUiState) -> Unit,
    onDeleteClick: (PortfolioItemUiState) -> Unit
) {
    LazyColumn {
        items(items) { item ->
            PortfolioItemCard(
                item = item,
                onClick = { onItemClick(item) },
                onDeleteClick = { onDeleteClick(item) }
            )
        }
    }
}

@Composable
private fun PortfolioItemCard(
    item: PortfolioItemUiState,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${item.quantity} ${item.symbol}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.formattedValue,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.formattedPrice,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
} 