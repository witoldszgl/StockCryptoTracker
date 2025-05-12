package com.example.stockcryptotracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.ui.components.EditPortfolioDialog
import com.example.stockcryptotracker.ui.components.PortfolioListItem
import com.example.stockcryptotracker.viewmodel.PortfolioItemUiState
import com.example.stockcryptotracker.viewmodel.PortfolioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: PortfolioViewModel = viewModel()
) {
    val portfolioItems by viewModel.filteredPortfolioItems.collectAsState()
    val allCryptos by viewModel.allCryptoList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val portfolioValue by viewModel.portfolioValue.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showAddCryptoScreen by remember { mutableStateOf(false) }
    var showAddCryptoDialog by remember { mutableStateOf(false) }
    var editCryptoData by remember { mutableStateOf<Pair<CryptoCurrency, Double?>?>(null) }
    var selectedCryptoForPortfolio by remember { mutableStateOf<CryptoCurrency?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (showAddCryptoScreen) "Add Cryptocurrency" else "Portfolio") 
                },
                navigationIcon = {
                    if (showAddCryptoScreen) {
                        IconButton(onClick = { showAddCryptoScreen = false }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!showAddCryptoScreen) {
                FloatingActionButton(
                    onClick = { showAddCryptoScreen = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add, 
                        contentDescription = "Add Cryptocurrency"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showAddCryptoScreen) {
                // ADD CRYPTO SCREEN
                // Search field for all cryptos
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search cryptocurrencies...") },
                    leadingIcon = { 
                        Icon(
                            imageVector = Icons.Filled.Search, 
                            contentDescription = "Search"
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear, 
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true
                )
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading && allCryptos.isEmpty() -> {
                            CircularProgressIndicator()
                        }
                        error != null && allCryptos.isEmpty() -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Error: $error",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            val filteredCryptos = if (searchQuery.isEmpty()) {
                                allCryptos
                            } else {
                                allCryptos.filter { 
                                    it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.symbol.contains(searchQuery, ignoreCase = true)
                                }
                            }
                            
                            if (filteredCryptos.isEmpty() && searchQuery.isNotEmpty()) {
                                Text(
                                    text = "No cryptocurrencies found for: \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredCryptos) { crypto ->
                                        com.example.stockcryptotracker.ui.components.CryptoListItem(
                                            cryptocurrency = crypto,
                                            onItemClick = { 
                                                selectedCryptoForPortfolio = crypto
                                                showAddCryptoDialog = true
                                            },
                                            isFavorite = false, // Not relevant here
                                            onFavoriteClick = { }  // Not relevant here
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // PORTFOLIO HOLDINGS SCREEN
                // Search field only if we have items
                if (portfolioItems.isNotEmpty()) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search portfolio...") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Filled.Search, 
                                contentDescription = "Search"
                            ) 
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear, 
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true
                    )
                }
                
                // Portfolio value summary card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Portfolio Value",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = portfolioValue,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading && portfolioItems.isEmpty() -> {
                            CircularProgressIndicator()
                        }
                        error != null && portfolioItems.isEmpty() -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Error: $error",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        portfolioItems.isEmpty() -> {
                            Text(
                                text = "No assets in portfolio. Add cryptocurrencies by clicking the button below.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(portfolioItems) { portfolioItem ->
                                    val crypto = allCryptos.find { it.id == portfolioItem.id }
                                    PortfolioListItem(
                                        portfolioItem = portfolioItem,
                                        cryptoImageUrl = crypto?.image,
                                        onEditClick = {
                                            if (crypto != null) {
                                                editCryptoData = Pair(crypto, portfolioItem.quantity)
                                            }
                                        },
                                        onDeleteClick = {
                                            viewModel.removePortfolioItem(portfolioItem.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialog for adding to portfolio
    if (showAddCryptoDialog && selectedCryptoForPortfolio != null) {
        EditPortfolioDialog(
            crypto = selectedCryptoForPortfolio!!,
            currentQuantity = null,
            onDismiss = { 
                showAddCryptoDialog = false
                selectedCryptoForPortfolio = null
            },
            onSave = { quantity ->
                viewModel.addUpdatePortfolioItem(selectedCryptoForPortfolio!!.id, quantity)
                showAddCryptoDialog = false
                selectedCryptoForPortfolio = null
                showAddCryptoScreen = false
            }
        )
    }
    
    // Dialog for editing portfolio item
    if (editCryptoData != null) {
        val (crypto, quantity) = editCryptoData!!
        EditPortfolioDialog(
            crypto = crypto,
            currentQuantity = quantity,
            onDismiss = { editCryptoData = null },
            onSave = { newQuantity ->
                viewModel.addUpdatePortfolioItem(crypto.id, newQuantity)
                editCryptoData = null
            }
        )
    }
} 