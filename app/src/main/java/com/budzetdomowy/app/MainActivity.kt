package com.budzetdomowy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budzetdomowy.app.ui.add.EditTransactionScreen
import com.budzetdomowy.app.ui.add.EditTransactionViewModel
import com.budzetdomowy.app.ui.home.HomeScreen
import com.budzetdomowy.app.ui.home.HomeViewModel
import com.budzetdomowy.app.ui.settings.SettingsScreen
import com.budzetdomowy.app.ui.theme.BudzetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BudzetApp
        setContent {
            BudzetTheme {
                BudzetNavHost(app)
            }
        }
    }
}

@Composable
private fun BudzetNavHost(app: BudzetApp) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.repository))
            HomeScreen(
                viewModel = vm,
                onAdd = { navController.navigate("edit/0") },
                onEdit = { id -> navController.navigate("edit/$id") },
                onSettings = { navController.navigate("settings") }
            )
        }
        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: EditTransactionViewModel = viewModel(
                factory = EditTransactionViewModel.Factory(app.repository, id)
            )
            EditTransactionScreen(
                viewModel = vm,
                onDone = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
