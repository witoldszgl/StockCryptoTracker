package com.example.stockcryptotracker.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.room.PriceAlert
import com.example.stockcryptotracker.ui.components.EditPortfolioDialog
import com.example.stockcryptotracker.ui.components.EditStockPortfolioDialog
import com.example.stockcryptotracker.ui.components.PortfolioListItem
import com.example.stockcryptotracker.ui.components.PriceAlertDialog
import com.example.stockcryptotracker.ui.components.StockPriceAlertDialog
import com.example.stockcryptotracker.viewmodel.PortfolioItemType
import com.example.stockcryptotracker.viewmodel.PortfolioItemUiState
import com.example.stockcryptotracker.viewmodel.PortfolioViewModel
import com.example.stockcryptotracker.viewmodel.PriceAlertViewModel

// Define enum outside the composable
enum class AddAssetScreenType { NONE, CRYPTO, STOCK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToStockDetail: (String) -> Unit,
    viewModel: PortfolioViewModel = viewModel(),
    priceAlertViewModel: PriceAlertViewModel = viewModel()
) {
    val portfolioItems by viewModel.filteredPortfolioItems.collectAsState()
    val allCryptos by viewModel.allCryptoList.collectAsState()
    val allStocks by viewModel.allStocksList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val portfolioValue by viewModel.portfolioValue.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    
    // Alert states
    val alertLoading by priceAlertViewModel.loading.collectAsState()
    val alertError by priceAlertViewModel.error.collectAsState()
    val alertSuccess by priceAlertViewModel.successMessage.collectAsState()
    val allAlerts by priceAlertViewModel.allAlerts.collectAsState()
    
    // Create a map of asset ID to list of active alerts
    val alertsByAssetId = remember(allAlerts) {
        allAlerts.filter { it.isActive }.groupBy { 
            if (it.isCrypto) it.assetId else it.assetId 
        }
    }
    
    // For showing snackbar messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle alert success or error messages
    LaunchedEffect(alertSuccess, alertError) {
        when {
            alertSuccess != null -> {
                snackbarHostState.showSnackbar(message = alertSuccess ?: "Success")
                priceAlertViewModel.clearMessages()
            }
            alertError != null -> {
                snackbarHostState.showSnackbar(message = alertError ?: "Error occurred")
                priceAlertViewModel.clearMessages()
            }
        }
    }
    
    // Asset selection UI state
    var addAssetScreenType by remember { mutableStateOf(AddAssetScreenType.NONE) }
    
    // Dialog state
    var showAddAssetTypeDialog by remember { mutableStateOf(false) }
    var showAddCryptoDialog by remember { mutableStateOf(false) }
    var showAddStockDialog by remember { mutableStateOf(false) }
    var editCryptoData by remember { mutableStateOf<Pair<CryptoCurrency, Double?>?>(null) }
    var editStockData by remember { mutableStateOf<Pair<Stock, Double?>?>(null) }
    var selectedCryptoForPortfolio by remember { mutableStateOf<CryptoCurrency?>(null) }
    var selectedStockForPortfolio by remember { mutableStateOf<Stock?>(null) }
    
    // Price alert dialog state
    var showCryptoAlertDialog by remember { mutableStateOf(false) }
    var showStockAlertDialog by remember { mutableStateOf(false) }
    var selectedCryptoForAlert by remember { mutableStateOf<CryptoCurrency?>(null) }
    var selectedStockForAlert by remember { mutableStateOf<Stock?>(null) }
    
    // UI components
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    when (addAssetScreenType) {
                        AddAssetScreenType.CRYPTO -> Text("Add Cryptocurrency")
                        AddAssetScreenType.STOCK -> Text("Add Stock")
                        AddAssetScreenType.NONE -> Text("Portfolio")
                    }
                },
                navigationIcon = {
                    if (addAssetScreenType != AddAssetScreenType.NONE) {
                        IconButton(onClick = { addAssetScreenType = AddAssetScreenType.NONE }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            if (addAssetScreenType == AddAssetScreenType.NONE) {
                FloatingActionButton(
                    onClick = { 
                        // Show dialog to choose asset type
                        showAddAssetTypeDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add, 
                        contentDescription = "Add Asset"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (addAssetScreenType) {
                AddAssetScreenType.CRYPTO -> {
                    // Search field for all cryptos
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Search cryptocurrencies...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )
                    
                    // Cryptocurrency list
                    if (allCryptos.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(allCryptos) { crypto ->
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    onClick = {
                                        selectedCryptoForPortfolio = crypto
                                        showAddCryptoDialog = true
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = crypto.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = crypto.symbol.uppercase(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator()
                            } else {
                                Text(
                                    text = "No cryptocurrencies available",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
                
                AddAssetScreenType.STOCK -> {
                    // Search field for all stocks
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Search stocks...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )
                    
                    // Stocks list
                    if (allStocks.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(allStocks) { stock ->
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    onClick = {
                                        selectedStockForPortfolio = stock
                                        showAddStockDialog = true
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = stock.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stock.symbol.uppercase(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator()
                            } else {
                                Text(
                                    text = "No stocks available",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
                
                AddAssetScreenType.NONE -> {
                    // Portfolio overview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Portfolio Value",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Text(
                            text = portfolioValue,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Tab selector for portfolio sections
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PortfolioViewModel.PortfolioTab.values().forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTab.ordinal == index,
                                onClick = { viewModel.setSelectedTab(tab) },
                                text = { 
                                    Text(
                                        text = when(tab) {
                                            PortfolioViewModel.PortfolioTab.ALL -> "All"
                                            PortfolioViewModel.PortfolioTab.CRYPTO -> "Crypto"
                                            PortfolioViewModel.PortfolioTab.STOCKS -> "Stocks"
                                        }
                                    )
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Search bar for filtering portfolio
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search portfolio...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )
                    
                    // Portfolio items list
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator()
                            }
                            error != null -> {
                                Text(
                                    text = error ?: "Unknown error",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            portfolioItems.isEmpty() -> {
                                Text(
                                    text = when(selectedTab) {
                                        PortfolioViewModel.PortfolioTab.ALL -> "Your portfolio is empty. Add assets using the + button."
                                        PortfolioViewModel.PortfolioTab.CRYPTO -> "No cryptocurrencies in your portfolio. Add some using the + button."
                                        PortfolioViewModel.PortfolioTab.STOCKS -> "No stocks in your portfolio. Add some using the + button."
                                    },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(portfolioItems) { portfolioItem ->
                                        // Get active alerts for this portfolio item
                                        val itemAlerts = alertsByAssetId[portfolioItem.id] ?: emptyList()
                                        
                                        PortfolioListItem(
                                            portfolioItem = portfolioItem,
                                            activeAlerts = itemAlerts,
                                            onEditClick = {
                                                when (portfolioItem.type) {
                                                    PortfolioItemType.CRYPTO -> {
                                                        val crypto = allCryptos.find { it.id == portfolioItem.id }
                                                        if (crypto != null) {
                                                            editCryptoData = Pair(crypto, portfolioItem.quantity)
                                                        }
                                                    }
                                                    PortfolioItemType.STOCK -> {
                                                        val stock = allStocks.find { it.symbol == portfolioItem.id }
                                                        if (stock != null) {
                                                            editStockData = Pair(stock, portfolioItem.quantity)
                                                        }
                                                    }
                                                }
                                            },
                                            onDeleteClick = {
                                                viewModel.removePortfolioItem(portfolioItem.id, portfolioItem.type)
                                            },
                                            onAlertClick = {
                                                when (portfolioItem.type) {
                                                    PortfolioItemType.CRYPTO -> {
                                                        val crypto = allCryptos.find { it.id == portfolioItem.id }
                                                        if (crypto != null) {
                                                            selectedCryptoForAlert = crypto
                                                            showCryptoAlertDialog = true
                                                        }
                                                    }
                                                    PortfolioItemType.STOCK -> {
                                                        val stock = allStocks.find { it.symbol == portfolioItem.id }
                                                        if (stock != null) {
                                                            selectedStockForAlert = stock
                                                            showStockAlertDialog = true
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                when (portfolioItem.type) {
                                                    PortfolioItemType.CRYPTO -> onNavigateToDetail(portfolioItem.id)
                                                    PortfolioItemType.STOCK -> onNavigateToStockDetail(portfolioItem.id)
                                                }
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
    }
    
    // Dialog for choosing asset type
    if (showAddAssetTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddAssetTypeDialog = false },
            title = { Text("Add to Portfolio") },
            text = { Text("What type of asset would you like to add?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddAssetTypeDialog = false
                        addAssetScreenType = AddAssetScreenType.CRYPTO
                    }
                ) {
                    Text("Cryptocurrency")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddAssetTypeDialog = false
                        addAssetScreenType = AddAssetScreenType.STOCK
                    }
                ) {
                    Text("Stock")
                }
            }
        )
    }
    
    // Dialog for adding crypto to portfolio
    if (showAddCryptoDialog && selectedCryptoForPortfolio != null) {
        EditPortfolioDialog(
            crypto = selectedCryptoForPortfolio!!,
            currentQuantity = null,
            onDismiss = { 
                showAddCryptoDialog = false
                selectedCryptoForPortfolio = null
            },
            onSave = { quantity ->
                viewModel.addUpdateCryptoPortfolioItem(selectedCryptoForPortfolio!!.id, quantity)
                showAddCryptoDialog = false
                selectedCryptoForPortfolio = null
                addAssetScreenType = AddAssetScreenType.NONE
            }
        )
    }
    
    // Dialog for adding stock to portfolio
    if (showAddStockDialog && selectedStockForPortfolio != null) {
        EditStockPortfolioDialog(
            stock = selectedStockForPortfolio!!,
            currentQuantity = null,
            onDismiss = { 
                showAddStockDialog = false
                selectedStockForPortfolio = null
            },
            onSave = { quantity ->
                viewModel.addUpdateStockPortfolioItem(selectedStockForPortfolio!!.symbol, quantity)
                showAddStockDialog = false
                selectedStockForPortfolio = null
                addAssetScreenType = AddAssetScreenType.NONE
            }
        )
    }
    
    // Dialog for editing crypto portfolio item
    if (editCryptoData != null) {
        val (crypto, quantity) = editCryptoData!!
        EditPortfolioDialog(
            crypto = crypto,
            currentQuantity = quantity,
            onDismiss = { editCryptoData = null },
            onSave = { newQuantity ->
                viewModel.addUpdateCryptoPortfolioItem(crypto.id, newQuantity)
                editCryptoData = null
            }
        )
    }
    
    // Dialog for editing stock portfolio item
    if (editStockData != null) {
        val (stock, quantity) = editStockData!!
        EditStockPortfolioDialog(
            stock = stock,
            currentQuantity = quantity,
            onDismiss = { editStockData = null },
            onSave = { newQuantity ->
                viewModel.addUpdateStockPortfolioItem(stock.symbol, newQuantity)
                editStockData = null
            }
        )
    }
    
    // Dialog for setting crypto price alert
    if (showCryptoAlertDialog && selectedCryptoForAlert != null) {
        val crypto = selectedCryptoForAlert!!
        PriceAlertDialog(
            cryptoCurrency = crypto,
            currentPrice = crypto.currentPrice,
            onDismiss = { 
                showCryptoAlertDialog = false
                selectedCryptoForAlert = null
            },
            onSetAlert = { targetPrice, isAboveTarget ->
                priceAlertViewModel.addCryptoAlert(crypto, targetPrice, isAboveTarget)
                showCryptoAlertDialog = false
                selectedCryptoForAlert = null
            }
        )
    }
    
    // Dialog for setting stock price alert
    if (showStockAlertDialog && selectedStockForAlert != null) {
        val stock = selectedStockForAlert!!
        StockPriceAlertDialog(
            stock = stock,
            currentPrice = stock.price,
            onDismiss = { 
                showStockAlertDialog = false
                selectedStockForAlert = null
            },
            onSetAlert = { targetPrice, isAboveTarget ->
                priceAlertViewModel.addStockAlert(stock, targetPrice, isAboveTarget)
                showStockAlertDialog = false
                selectedStockForAlert = null
            }
        )
    }
    
    // Show loading indicator when setting alerts
    if (alertLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
} 