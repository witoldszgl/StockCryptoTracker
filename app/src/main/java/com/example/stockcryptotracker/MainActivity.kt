package com.example.stockcryptotracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.stockcryptotracker.ui.screens.CryptoDetailScreen
import com.example.stockcryptotracker.ui.screens.CryptoListScreen
import com.example.stockcryptotracker.ui.screens.PortfolioScreen
import com.example.stockcryptotracker.ui.theme.StockCryptoTrackerTheme
import com.example.stockcryptotracker.viewmodel.CryptoViewModel
import com.example.stockcryptotracker.viewmodel.PortfolioViewModel
import com.example.stockcryptotracker.viewmodel.Tab

enum class BottomNavItem(val route: String, val title: String) {
    CRYPTO_LIST("crypto_list", "Cryptocurrencies"),
    FAVORITES("favorites", "Favorites"),
    PORTFOLIO("portfolio", "Portfolio")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockCryptoTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CryptoApp()
                }
            }
        }
    }
}

@Composable
fun CryptoApp() {
    val navController = rememberNavController()
    val cryptoViewModel: CryptoViewModel = viewModel()
    val portfolioViewModel: PortfolioViewModel = viewModel()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var selectedNavItem by remember { mutableStateOf(BottomNavItem.CRYPTO_LIST) }
    
    // Update the tab based on the current destination
    LaunchedEffect(currentRoute) {
        when {
            currentRoute == BottomNavItem.CRYPTO_LIST.route -> {
                cryptoViewModel.setSelectedTab(Tab.ALL)
                selectedNavItem = BottomNavItem.CRYPTO_LIST
            }
            currentRoute == BottomNavItem.FAVORITES.route -> {
                cryptoViewModel.setSelectedTab(Tab.FAVORITES)
                selectedNavItem = BottomNavItem.FAVORITES
            }
            currentRoute == BottomNavItem.PORTFOLIO.route -> {
                selectedNavItem = BottomNavItem.PORTFOLIO
            }
        }
    }
    
    // Determine if bottom bar should be shown (not on detail screen)
    val showBottomBar = when {
        currentRoute?.startsWith("crypto_detail") == true -> false
        else -> true
    }
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.values().forEach { item ->
                        val selected = item == selectedNavItem
                        NavigationBarItem(
                            icon = { 
                                when (item) {
                                    BottomNavItem.CRYPTO_LIST -> Icon(
                                        imageVector = if (selected) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = item.title
                                    )
                                    BottomNavItem.FAVORITES -> Icon(
                                        imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = item.title
                                    )
                                    BottomNavItem.PORTFOLIO -> Icon(
                                        imageVector = if (selected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = item.title
                                    )
                                }
                            },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                if (item != selectedNavItem) {
                                    // Only update UI state when actually changing tabs
                                    selectedNavItem = item
                                    
                                    // Navigate only if we're not already on this route
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            // Pop back stack up to the start destination
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            // Avoid duplicate navigation
                                            launchSingleTop = true
                                            // Restore state if needed
                                            restoreState = true
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController, 
            startDestination = BottomNavItem.CRYPTO_LIST.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.CRYPTO_LIST.route) {
                CryptoListScreen(
                    onNavigateToDetail = { cryptoId ->
                        navController.navigate("crypto_detail/$cryptoId")
                    },
                    viewModel = cryptoViewModel
                )
            }
            
            composable(BottomNavItem.FAVORITES.route) {
                CryptoListScreen(
                    onNavigateToDetail = { cryptoId ->
                        navController.navigate("crypto_detail/$cryptoId")
                    },
                    viewModel = cryptoViewModel
                )
            }
            
            composable(BottomNavItem.PORTFOLIO.route) {
                PortfolioScreen(
                    onNavigateToDetail = { cryptoId ->
                        navController.navigate("crypto_detail/$cryptoId")
                    },
                    viewModel = portfolioViewModel
                )
            }
            
            composable(
                route = "crypto_detail/{cryptoId}",
                arguments = listOf(
                    navArgument("cryptoId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val cryptoId = backStackEntry.arguments?.getString("cryptoId") ?: ""
                CryptoDetailScreen(
                    cryptoId = cryptoId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
