package com.budzetdomowy.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budzetdomowy.app.ui.BudzetBottomBar
import com.budzetdomowy.app.ui.BudzetBottomTab
import com.budzetdomowy.core.data.ThemePreferences
import com.budzetdomowy.core.ui.R as UiR
import com.budzetdomowy.core.ui.theme.BudzetTheme
import com.budzetdomowy.feature.categories.CategoriesScreen
import com.budzetdomowy.feature.goals.GoalDetailScreen
import com.budzetdomowy.feature.goals.GoalsScreen
import com.budzetdomowy.feature.home.HomeScreen
import com.budzetdomowy.feature.recurring.EditRecurringScreen
import com.budzetdomowy.feature.recurring.RecurringScreen
import com.budzetdomowy.feature.report.ReportScreen
import com.budzetdomowy.feature.settings.SettingsScreen
import com.budzetdomowy.feature.splash.SplashScreen
import com.budzetdomowy.feature.transactions.EditTransactionScreen
import java.util.concurrent.atomic.AtomicBoolean
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private val bottomTabs = listOf(
    BudzetBottomTab("home", UiR.string.nav_home, Icons.Default.Home),
    BudzetBottomTab("report", UiR.string.report, Icons.Default.BarChart),
    BudzetBottomTab("goals", UiR.string.nav_goals, Icons.Default.Savings),
    BudzetBottomTab("recurring", UiR.string.nav_recurring, Icons.Default.Autorenew)
)

private val bottomBarRoutes = bottomTabs.map { it.route }.toSet()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val composeReady = AtomicBoolean(false)
        splashScreen.setKeepOnScreenCondition { !composeReady.get() }

        if (Build.VERSION.SDK_INT >= 35) {
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
        // Prevent the system translucent scrim above the gesture/nav bar ("przesłona").
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val showIntro = savedInstanceState == null
        setContent {
            SideEffect { composeReady.set(true) }
            val themePreferences: ThemePreferences = koinInject()
            val themeMode by themePreferences.mode.collectAsStateWithLifecycle()
            BudzetTheme(themeMode = themeMode) {
                var showSplash by remember { mutableStateOf(showIntro) }
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    BudzetNavHost(themePreferences)
                }
            }
        }
    }
}

@Composable
private fun BudzetNavHost(themePreferences: ThemePreferences) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Floating overlay bar — content scrolls underneath (no empty cream band / "przesłona").
    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = koinViewModel(),
                    onEdit = { id -> navController.navigate("edit/$id") },
                    onGoals = { navigateToTab("goals") },
                    onSettings = { navController.navigate("settings") }
                )
            }
            composable(
                route = "edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                EditTransactionScreen(
                    viewModel = koinViewModel { parametersOf(id) },
                    onDone = { navController.popBackStack() }
                )
            }
            composable("report") {
                ReportScreen(
                    viewModel = koinViewModel(),
                    showBack = false,
                    onBack = { navigateToTab("home") }
                )
            }
            composable("settings") {
                val themeMode by themePreferences.mode.collectAsStateWithLifecycle()
                SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = themePreferences::setMode,
                    onBack = { navController.popBackStack() },
                    onCategories = { navController.navigate("categories") },
                    onRecurring = { navigateToTab("recurring") },
                    appName = stringResource(R.string.app_name),
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    privacyUrl = stringResource(R.string.privacy_policy_url)
                )
            }
            composable("categories") {
                CategoriesScreen(
                    viewModel = koinViewModel(),
                    onBack = { navController.popBackStack() }
                )
            }
            composable("recurring") {
                RecurringScreen(
                    viewModel = koinViewModel(),
                    showBack = false,
                    onBack = { navigateToTab("home") },
                    onEdit = { id -> navController.navigate("recurring/$id") }
                )
            }
            composable(
                route = "recurring/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                EditRecurringScreen(
                    viewModel = koinViewModel { parametersOf(id) },
                    onDone = { navController.popBackStack() }
                )
            }
            composable("goals") {
                GoalsScreen(
                    viewModel = koinViewModel(),
                    showBack = false,
                    onBack = { navigateToTab("home") },
                    onOpenGoal = { id -> navController.navigate("goal/$id") }
                )
            }
            composable(
                route = "goal/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                GoalDetailScreen(
                    viewModel = koinViewModel { parametersOf(id) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showBottomBar) {
            BudzetBottomBar(
                tabs = bottomTabs,
                selectedRoute = currentRoute,
                onTabSelected = ::navigateToTab,
                onAddClick = { navController.navigate("edit/0") },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
