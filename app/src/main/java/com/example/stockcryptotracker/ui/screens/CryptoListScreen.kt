package com.example.stockcryptotracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockcryptotracker.ui.components.CryptoListItem
import com.example.stockcryptotracker.viewmodel.CryptoViewModel
import com.example.stockcryptotracker.viewmodel.Tab as ViewModelTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: CryptoViewModel = viewModel()
) {
    val cryptoList by viewModel.cryptoList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (selectedTab == ViewModelTab.ALL) "Cryptocurrency List" else "Favorite Cryptocurrencies"
                    ) 
                },
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
            // Search TextField
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or symbol...") },
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
                modifier = Modifier
                    .fillMaxSize(),
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
                    cryptoList.isEmpty() && selectedTab == ViewModelTab.FAVORITES -> {
                        Text(
                            text = "No favorite cryptocurrencies. Add cryptocurrencies to your favorites list using the star icon.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
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
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(cryptoList) { crypto ->
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
                        }
                    }
                }
            }
        }
    }
} 