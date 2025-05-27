package com.example.stockcryptotracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.stockcryptotracker.service.PriceAlertWorker
import com.example.stockcryptotracker.ui.screens.CryptoDetailScreen
import com.example.stockcryptotracker.ui.screens.CryptoListScreen
import com.example.stockcryptotracker.ui.screens.FavoritesScreen
import com.example.stockcryptotracker.ui.screens.PortfolioScreen
import com.example.stockcryptotracker.ui.screens.PriceAlertsScreen
import com.example.stockcryptotracker.ui.screens.StockDetailScreen
import com.example.stockcryptotracker.ui.screens.StockListScreen
import com.example.stockcryptotracker.ui.theme.StockCryptoTrackerTheme
import com.example.stockcryptotracker.viewmodel.CryptoViewModel
import com.example.stockcryptotracker.viewmodel.PortfolioViewModel
import com.example.stockcryptotracker.viewmodel.PriceAlertViewModel
import com.example.stockcryptotracker.viewmodel.StockViewModel
import com.example.stockcryptotracker.viewmodel.Tab
import androidx.compose.ui.platform.LocalContext

private const val CHANNEL_ID = "price_alerts"

enum class BottomNavItem(val title: String, val route: String) {
    CRYPTO_LIST("Crypto", "crypto_list"),
    STOCKS("Stocks", "stocks_list"),
    FAVORITES("Favorites", "favorites"),
    PORTFOLIO("Portfolio", "portfolio"),
    ALERTS("Alerts", "price_alerts"),
}

class MainActivity : ComponentActivity() {
    
    // Launcher do prośby o uprawnienia
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Uprawnienie zostało przyznane, możemy wysyłać powiadomienia
            Toast.makeText(this, "Powiadomienia włączone!", Toast.LENGTH_SHORT).show()
        } else {
            // Uprawnienie zostało odrzucone, poinformuj użytkownika
            Toast.makeText(this, 
                "Powiadomienia są wyłączone. Włącz je w ustawieniach, aby otrzymywać alerty cenowe.", 
                Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule the price alert worker to periodically check for alerts
        PriceAlertWorker.schedulePriceAlertChecks(this)
        
        // Create notification channel
        createNotificationChannel()
        
        // Sprawdź uprawnienia do powiadomień
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
        
        enableEdgeToEdge()
        setContent {
            StockCryptoTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == 
                        PackageManager.PERMISSION_GRANTED -> {
                    // Uprawnienie jest już przyznane
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Wyjaśnij, dlaczego potrzebujemy uprawnienia
                    Toast.makeText(this, 
                        "Potrzebujemy uprawnienia do wysyłania powiadomień, aby informować o alertach cenowych.", 
                        Toast.LENGTH_LONG).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Poproś o uprawnienie
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == 
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // Dla starszych wersji Androida uprawnienie jest przyznawane w manifeście
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Price Alerts"
            val descriptionText = "Notifications when asset prices reach target levels"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun sendTestNotification() {
        // Check for notification permission first
        if (!checkNotificationPermission()) {
            requestNotificationPermission()
            Toast.makeText(
                this,
                "Please grant notification permission to receive alerts",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Create notification channel first
        createNotificationChannel()
        
        // Create an intent for tapping on notification
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            1234, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Price Alert Test")
            .setContentText("This is a test notification! It works!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            try {
                notify(1234, builder.build())
                Toast.makeText(
                    this@MainActivity,
                    "Test notification has been sent!",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: SecurityException) {
                // Handle missing notification permission
                Toast.makeText(
                    this@MainActivity,
                    "Error: Missing notification permission",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val cryptoViewModel: CryptoViewModel = viewModel()
    val stockViewModel: StockViewModel = viewModel()
    val portfolioViewModel: PortfolioViewModel = viewModel()
    val priceAlertViewModel: PriceAlertViewModel = viewModel()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var selectedNavItem by remember { mutableStateOf(BottomNavItem.CRYPTO_LIST) }
    
    // Update the tab based on the current destination
    LaunchedEffect(currentRoute) {
        when {
            currentRoute == BottomNavItem.CRYPTO_LIST.route -> {
                // Always reset to ALL tab when navigating to Crypto list
                cryptoViewModel.setSelectedTab(Tab.ALL)
                selectedNavItem = BottomNavItem.CRYPTO_LIST
            }
            currentRoute == BottomNavItem.FAVORITES.route -> {
                // No need to set Tab here as it's done in FavoritesScreen
                selectedNavItem = BottomNavItem.FAVORITES
                // Pobierz ulubione akcje z cache'u (bez wywoływania API)
                val favoriteIds = stockViewModel.favoriteIds.value
                if (favoriteIds.isNotEmpty()) {
                    stockViewModel.loadFavoriteStocksFromCache(favoriteIds)
                }
            }
            currentRoute == BottomNavItem.PORTFOLIO.route -> {
                selectedNavItem = BottomNavItem.PORTFOLIO
                // Tutaj również używamy cache'u zamiast API
                portfolioViewModel.loadPortfolioItemsFromCache()
            }
            currentRoute == BottomNavItem.STOCKS.route -> {
                selectedNavItem = BottomNavItem.STOCKS
                // Przy wejściu do zakładki Stocks, aktualizujemy cache
                stockViewModel.loadStocksAndUpdateCache()
            }
            currentRoute == BottomNavItem.ALERTS.route -> {
                selectedNavItem = BottomNavItem.ALERTS
            }
        }
    }
    
    // Determine if bottom bar should be shown (not on detail screen)
    val showBottomBar = when {
        currentRoute?.startsWith("crypto_detail") == true -> false
        currentRoute?.startsWith("stock_detail") == true -> false
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
                                        imageVector = if (selected) Icons.Filled.CurrencyBitcoin else Icons.Outlined.CurrencyBitcoin,
                                        contentDescription = item.title
                                    )
                                    BottomNavItem.STOCKS -> Icon(
                                        imageVector = if (selected) Icons.AutoMirrored.Filled.ShowChart else Icons.AutoMirrored.Outlined.ShowChart,
                                        contentDescription = item.title
                                    )
                                    BottomNavItem.FAVORITES -> Icon(
                                        imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = item.title
                                    )
                                    BottomNavItem.PORTFOLIO -> Icon(
                                        imageVector = if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = item.title
                                    )
                                    BottomNavItem.ALERTS -> {
                                        val activeAlerts by priceAlertViewModel.alerts.collectAsState()
                                        val activeAlertsCount = activeAlerts.size
                                        
                                        if (activeAlertsCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge {
                                                        Text(activeAlertsCount.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (selected) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                                    contentDescription = item.title
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (selected) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                                contentDescription = item.title
                                            )
                                        }
                                    }
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
            
            composable(BottomNavItem.STOCKS.route) {
                StockListScreen(
                    onNavigateToDetail = { symbol ->
                        navController.navigate("stock_detail/$symbol")
                    },
                    viewModel = viewModel(),
                    context = LocalContext.current
                )
            }
            
            composable(BottomNavItem.FAVORITES.route) {
                FavoritesScreen(
                    onNavigateToCryptoDetail = { cryptoId ->
                        navController.navigate("crypto_detail/$cryptoId")
                    },
                    onNavigateToStockDetail = { symbol ->
                        navController.navigate("stock_detail/$symbol")
                    },
                    cryptoViewModel = cryptoViewModel,
                    stockViewModel = viewModel()
                )
            }
            
            composable(BottomNavItem.PORTFOLIO.route) {
                PortfolioScreen(
                    onNavigateToDetail = { id, isCrypto ->
                        if (isCrypto) {
                            navController.navigate("crypto_detail/$id")
                        } else {
                            navController.navigate("stock_detail/$id")
                        }
                    }
                )
            }
            
            composable(BottomNavItem.ALERTS.route) {
                PriceAlertsScreen()
            }
            
            composable(
                "crypto_detail/{cryptoId}",
                arguments = listOf(navArgument("cryptoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val cryptoId = backStackEntry.arguments?.getString("cryptoId") ?: return@composable
                CryptoDetailScreen(
                    cryptoId = cryptoId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(
                "stock_detail/{symbol}",
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("symbol") ?: return@composable
                StockDetailScreen(
                    symbol = symbol,
                    onBackClick = { navController.popBackStack() },
                    viewModel = viewModel(),
                    alertViewModel = priceAlertViewModel,
                    onSetAlert = { symbol, price -> 
                        priceAlertViewModel.setPriceAlert(symbol, price)
                    }
                )
            }
        }
    }
}
