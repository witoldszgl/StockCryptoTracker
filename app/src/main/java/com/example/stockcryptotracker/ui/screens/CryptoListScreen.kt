package com.example.stockcryptotracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockcryptotracker.ui.components.CryptoListItem
import com.example.stockcryptotracker.viewmodel.CryptoCategory
import com.example.stockcryptotracker.viewmodel.CryptoViewModel
import com.example.stockcryptotracker.viewmodel.Tab as ViewModelTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: CryptoViewModel = viewModel()
) {
    // Force Tab.ALL when entering this screen
    viewModel.setSelectedTab(ViewModelTab.ALL)
    
    val cryptoList by viewModel.cryptoList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cryptocurrency List") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search cryptocurrencies...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Category filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(CryptoCategory.values()) { category ->
                    FilterChip(
                        selected = category == selectedCategory,
                        onClick = { viewModel.setCategory(category) },
                        label = {
                            Text(
                                when (category) {
                                    CryptoCategory.ALL -> "All"
                                    CryptoCategory.MOST_ACTIVE -> "Most Active"
                                    CryptoCategory.TOP_GAINERS -> "Top Gainers"
                                    CryptoCategory.TOP_LOSERS -> "Top Losers"
                                }
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Main content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading && cryptoList.isEmpty() -> {
                        CircularProgressIndicator()
                    }
                    error != null && cryptoList.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error: $error",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    cryptoList.isEmpty() && searchQuery.isNotEmpty() -> {
                        Text(
                            text = "No cryptocurrencies found for: \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    cryptoList.isEmpty() -> {
                        Text(
                            text = "No cryptocurrency data available",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        val listState = rememberLazyListState()
                        
                        // Check if we should load more items
                        val shouldLoadMore = remember {
                            derivedStateOf {
                                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                val totalItems = cryptoList.size
                                lastVisibleItem >= totalItems - 5 && canLoadMore
                            }
                        }
                        
                        // Trigger loading more items when scrolling near the end
                        LaunchedEffect(shouldLoadMore.value) {
                            if (shouldLoadMore.value) {
                                viewModel.loadMoreItems()
                            }
                        }
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState
                        ) {
                            itemsIndexed(cryptoList) { index, crypto ->
                                val isFavorite by viewModel.isFavorite(crypto.id).collectAsState()
                                
                                CryptoListItem(
                                    cryptocurrency = crypto,
                                    onItemClick = { cryptoId ->
                                        onNavigateToDetail(cryptoId)
                                    },
                                    isFavorite = isFavorite,
                                    onFavoriteClick = {
                                        viewModel.toggleFavorite(crypto)
                                    }
                                )
                            }
                            
                            // Show loading indicator at the bottom when loading more items
                            if (isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
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
} 