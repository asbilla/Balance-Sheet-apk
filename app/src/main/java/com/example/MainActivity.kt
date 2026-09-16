package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.TransactionRepository
import com.example.ui.appointments.AppointmentsScreen
import com.example.ui.balancesheet.BalanceSheetScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.entry.EntryScreen
import com.example.ui.setup.SetupScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TransactionRepository.getInstance(applicationContext)
        
        setContent {
            val themeMode by repository.themeMode.collectAsState(initial = repository.getThemeMode())
            
            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(repository = repository)
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Dashboard : Screen("dashboard")
    data object BalanceSheet : Screen("balancesheet")
    data object Appointments : Screen("appointments")
    data object Entry : Screen("entry/{entryType}") {
        fun createRoute(entryType: String): String = "entry/${Uri.encode(entryType)}"
    }
}

@Composable
fun AppNavigation(
    repository: TransactionRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val startDestination = Screen.Dashboard.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Setup Screen
        composable(Screen.Setup.route) {
            SetupScreen(
                repository = repository,
                onConfigured = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                },
                onNavigateBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else null
            )
        }

        // Main Dashboard Screen
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                repository = repository,
                onNavigateToEntry = { entryType ->
                    navController.navigate(Screen.Entry.createRoute(entryType))
                },
                onNavigateToBalanceSheet = {
                    navController.navigate(Screen.BalanceSheet.route)
                },
                onNavigateToAppointments = {
                    navController.navigate(Screen.Appointments.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Setup.route)
                }
            )
        }

        // Appointments & Phone Bookings Screen
        composable(Screen.Appointments.route) {
            AppointmentsScreen(
                repository = repository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Setup.route)
                }
            )
        }

        // Transaction Entry Input Screen
        composable(
            route = Screen.Entry.route,
            arguments = listOf(
                navArgument("entryType") {
                    type = NavType.StringType
                    defaultValue = "Daily Income"
                }
            )
        ) { backStackEntry ->
            val rawType = backStackEntry.arguments?.getString("entryType") ?: "Daily Income"
            val decodedType = Uri.decode(rawType)
            EntryScreen(
                entryType = decodedType,
                repository = repository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Day-by-Day Balance Sheet Screen
        composable(Screen.BalanceSheet.route) {
            BalanceSheetScreen(
                repository = repository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
