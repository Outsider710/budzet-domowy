package com.budzetdomowy.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budzetdomowy.app.ui.add.EditTransactionScreen
import com.budzetdomowy.app.ui.add.EditTransactionViewModel
import com.budzetdomowy.app.ui.categories.CategoriesScreen
import com.budzetdomowy.app.ui.categories.CategoriesViewModel
import com.budzetdomowy.app.ui.goals.GoalDetailScreen
import com.budzetdomowy.app.ui.goals.GoalDetailViewModel
import com.budzetdomowy.app.ui.goals.GoalsScreen
import com.budzetdomowy.app.ui.goals.GoalsViewModel
import com.budzetdomowy.app.ui.home.HomeScreen
import com.budzetdomowy.app.ui.home.HomeViewModel
import com.budzetdomowy.app.ui.recurring.EditRecurringScreen
import com.budzetdomowy.app.ui.recurring.EditRecurringViewModel
import com.budzetdomowy.app.ui.recurring.RecurringScreen
import com.budzetdomowy.app.ui.recurring.RecurringViewModel
import com.budzetdomowy.app.ui.report.ReportScreen
import com.budzetdomowy.app.ui.report.ReportViewModel
import com.budzetdomowy.app.ui.settings.SettingsScreen
import com.budzetdomowy.app.ui.theme.BudzetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 35) {
            // Android 15+: avoid deprecated setStatusBarColor / setNavigationBarColor / SHORT_EDGES
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        } else {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
            )
        }
        val app = application as BudzetApp
        setContent {
            val themeMode by app.themePreferences.mode.collectAsStateWithLifecycle()
            BudzetTheme(themeMode = themeMode) {
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
                onReport = { navController.navigate("report") },
                onGoals = { navController.navigate("goals") },
                onRecurring = { navController.navigate("recurring") },
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
        composable("report") {
            val vm: ReportViewModel = viewModel(
                factory = ReportViewModel.Factory(app, app.repository)
            )
            ReportScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            val themeMode by app.themePreferences.mode.collectAsStateWithLifecycle()
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = app.themePreferences::setMode,
                onBack = { navController.popBackStack() },
                onCategories = { navController.navigate("categories") },
                onRecurring = { navController.navigate("recurring") }
            )
        }
        composable("categories") {
            val vm: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory(app.repository))
            CategoriesScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable("recurring") {
            val vm: RecurringViewModel = viewModel(factory = RecurringViewModel.Factory(app.repository))
            RecurringScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("recurring/$id") }
            )
        }
        composable(
            route = "recurring/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: EditRecurringViewModel = viewModel(
                factory = EditRecurringViewModel.Factory(app.repository, id)
            )
            EditRecurringScreen(
                viewModel = vm,
                onDone = { navController.popBackStack() }
            )
        }
        composable("goals") {
            val vm: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory(app.repository))
            GoalsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenGoal = { id -> navController.navigate("goal/$id") }
            )
        }
        composable(
            route = "goal/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: GoalDetailViewModel = viewModel(
                factory = GoalDetailViewModel.Factory(app.repository, id)
            )
            GoalDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
