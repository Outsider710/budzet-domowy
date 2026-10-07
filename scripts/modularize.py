#!/usr/bin/env python3
"""Modularize BudzetDomowy into core + feature modules."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP_SRC = ROOT / "app" / "src"


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def lib_gradle(
    namespace: str,
    *,
    compose: bool = False,
    room: bool = False,
    koin: bool = False,
    deps: list[str] | None = None,
    unit_tests: bool = False,
    android_tests: bool = False,
) -> str:
    plugins = ['id("com.android.library")', 'id("org.jetbrains.kotlin.android")']
    if compose:
        plugins.append('id("org.jetbrains.kotlin.plugin.compose")')
    if room:
        plugins.append('id("com.google.devtools.ksp")')
    lines = ["plugins {"]
    for p in plugins:
        lines.append(f"    {p}")
    lines += [
        "}",
        "",
        "android {",
        f'    namespace = "{namespace}"',
        "    compileSdk = 36",
        "",
        "    defaultConfig {",
        "        minSdk = 26",
        '        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"',
        '        consumerProguardFiles("consumer-rules.pro")',
        "    }",
        "",
        "    compileOptions {",
        "        sourceCompatibility = JavaVersion.VERSION_17",
        "        targetCompatibility = JavaVersion.VERSION_17",
        "    }",
        "",
        "    kotlinOptions {",
        '        jvmTarget = "17"',
        "    }",
    ]
    if compose:
        lines += ["", "    buildFeatures {", "        compose = true", "    }"]
    lines += ["}", "", "dependencies {"]
    if compose:
        lines += [
            '    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")',
            "    implementation(composeBom)",
            '    implementation("androidx.compose.ui:ui")',
            '    implementation("androidx.compose.ui:ui-tooling-preview")',
            '    implementation("androidx.compose.material3:material3")',
            '    implementation("androidx.compose.material:material-icons-extended")',
            '    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")',
            '    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")',
        ]
    lines.append('    implementation("androidx.core:core-ktx:1.16.0")')
    if room:
        lines += [
            '    implementation("androidx.room:room-runtime:2.7.2")',
            '    implementation("androidx.room:room-ktx:2.7.2")',
            '    ksp("androidx.room:room-compiler:2.7.2")',
        ]
    if koin:
        lines += [
            '    val koinBom = platform("io.insert-koin:koin-bom:4.2.2")',
            "    implementation(koinBom)",
            '    implementation("io.insert-koin:koin-android")',
            '    implementation("io.insert-koin:koin-androidx-compose")',
        ]
    for d in deps or []:
        lines.append(f"    {d}")
    if unit_tests:
        lines.append('    testImplementation("junit:junit:4.13.2")')
    if android_tests:
        lines += [
            '    androidTestImplementation("androidx.test.ext:junit:1.2.1")',
            '    androidTestImplementation("androidx.test:runner:1.6.2")',
            '    androidTestImplementation("androidx.test:core:1.6.1")',
            '    androidTestImplementation("androidx.room:room-testing:2.7.2")',
            '    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")',
        ]
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def transform_kotlin(
    content: str,
    new_package: str,
    r_import: str,
) -> str:
    content = re.sub(r"^package .+$", f"package {new_package}", content, count=1, flags=re.M)
    content = content.replace("com.budzetdomowy.app.data.", "com.budzetdomowy.core.data.")
    content = content.replace("com.budzetdomowy.app.util.", "com.budzetdomowy.core.ui.util.")
    content = content.replace("com.budzetdomowy.app.ui.ScrollColumn", "com.budzetdomowy.core.ui.ScrollColumn")
    content = content.replace("com.budzetdomowy.app.ui.theme.", "com.budzetdomowy.core.ui.theme.")
    content = content.replace("import com.budzetdomowy.app.R", f"import {r_import}")
    content = content.replace("com.budzetdomowy.app.ui.add.", "com.budzetdomowy.feature.transactions.")
    content = content.replace("com.budzetdomowy.app.ui.home.", "com.budzetdomowy.feature.home.")
    content = content.replace("com.budzetdomowy.app.ui.categories.", "com.budzetdomowy.feature.categories.")
    content = content.replace("com.budzetdomowy.app.ui.goals.", "com.budzetdomowy.feature.goals.")
    content = content.replace("com.budzetdomowy.app.ui.recurring.", "com.budzetdomowy.feature.recurring.")
    content = content.replace("com.budzetdomowy.app.ui.report.", "com.budzetdomowy.feature.report.")
    content = content.replace("com.budzetdomowy.app.ui.settings.", "com.budzetdomowy.feature.settings.")
    return content


def copy_src(src: Path, dest: Path, new_package: str, r_import: str) -> None:
    write(dest, transform_kotlin(read(src), new_package, r_import))


def setup_feature(
    name: str,
    files: list[tuple[Path, str]],
    module_name: str,
    module_body: str,
    *,
    unit_test: tuple[Path, str] | None = None,
) -> None:
    ns = f"com.budzetdomowy.feature.{name}"
    base = ROOT / "feature" / name
    write(
        base / "build.gradle.kts",
        lib_gradle(
            ns,
            compose=True,
            koin=True,
            unit_tests=unit_test is not None,
            deps=[
                'implementation(project(":core:data"))',
                'implementation(project(":core:ui"))',
            ],
        ),
    )
    write(base / "consumer-rules.pro", "")
    write(base / "src/main/AndroidManifest.xml", "<manifest />\n")
    # empty strings - all UI strings live in core:ui
    write(base / "src/main/res/values/strings.xml", '<?xml version="1.0" encoding="utf-8"?>\n<resources />\n')
    for src, dest_name in files:
        copy_src(
            src,
            base / "src/main/java/com/budzetdomowy/feature" / name / dest_name,
            ns,
            "com.budzetdomowy.core.ui.R",
        )
    write(base / f"src/main/java/com/budzetdomowy/feature/{name}/{module_name}", module_body)
    if unit_test:
        src, dest_name = unit_test
        copy_src(
            src,
            base / "src/test/java/com/budzetdomowy/feature" / name / dest_name,
            ns,
            "com.budzetdomowy.core.ui.R",
        )


def main() -> None:
    write(
        ROOT / "build.gradle.kts",
        """plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.android.library") version "9.0.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
}
""",
    )
    write(
        ROOT / "settings.gradle.kts",
        """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BudzetDomowy"
include(
    ":app",
    ":core:data",
    ":core:ui",
    ":feature:home",
    ":feature:transactions",
    ":feature:categories",
    ":feature:goals",
    ":feature:recurring",
    ":feature:report",
    ":feature:settings",
)
""",
    )

    # core:data
    write(
        ROOT / "core/data/build.gradle.kts",
        lib_gradle(
            "com.budzetdomowy.core.data",
            room=True,
            koin=True,
            unit_tests=True,
            android_tests=True,
        ),
    )
    write(ROOT / "core/data/consumer-rules.pro", "")
    write(ROOT / "core/data/src/main/AndroidManifest.xml", "<manifest />\n")
    write(
        ROOT / "core/data/src/main/res/values/strings.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="category_food">Jedzenie</string>
    <string name="category_transport">Transport</string>
    <string name="category_bills">Rachunki</string>
    <string name="category_entertainment">Rozrywka</string>
    <string name="category_other">Inne</string>
    <string name="category_salary">Przychód</string>
</resources>
""",
    )
    write(
        ROOT / "core/data/src/main/res/values-en/strings.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="category_food">Food</string>
    <string name="category_transport">Transport</string>
    <string name="category_bills">Bills</string>
    <string name="category_entertainment">Entertainment</string>
    <string name="category_other">Other</string>
    <string name="category_salary">Income</string>
</resources>
""",
    )
    data_pkg = "com.budzetdomowy.core.data"
    for name in [
        "AppDatabase.kt",
        "BudgetDao.kt",
        "BudgetRepository.kt",
        "Models.kt",
        "ThemePreferences.kt",
        "TransactionEntity.kt",
    ]:
        copy_src(
            APP_SRC / "main/java/com/budzetdomowy/app/data" / name,
            ROOT / "core/data/src/main/java/com/budzetdomowy/core/data" / name,
            data_pkg,
            "com.budzetdomowy.core.data.R",
        )
    write(
        ROOT / "core/data/src/main/java/com/budzetdomowy/core/data/CoreDataModule.kt",
        """package com.budzetdomowy.core.data

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreDataModule = module {
    single { AppDatabase.get(androidContext()) }
    single { get<AppDatabase>().budgetDao() }
    single { BudgetRepository(get()) }
    single { ThemePreferences(androidContext()) }
}
""",
    )
    copy_src(
        APP_SRC / "test/java/com/budzetdomowy/app/data/RecurringDateTest.kt",
        ROOT / "core/data/src/test/java/com/budzetdomowy/core/data/RecurringDateTest.kt",
        data_pkg,
        "com.budzetdomowy.core.data.R",
    )
    copy_src(
        APP_SRC / "androidTest/java/com/budzetdomowy/app/data/BudgetRepositoryTest.kt",
        ROOT / "core/data/src/androidTest/java/com/budzetdomowy/core/data/BudgetRepositoryTest.kt",
        data_pkg,
        "com.budzetdomowy.core.data.R",
    )

    # core:ui — all non-category UI strings
    write(
        ROOT / "core/ui/build.gradle.kts",
        lib_gradle(
            "com.budzetdomowy.core.ui",
            compose=True,
            unit_tests=True,
            deps=['api(project(":core:data"))'],
        ),
    )
    write(ROOT / "core/ui/consumer-rules.pro", "")
    write(ROOT / "core/ui/src/main/AndroidManifest.xml", "<manifest />\n")
    # Move full strings minus app_name/privacy and category_* (those stay in data)
    pl = read(APP_SRC / "main/res/values/strings.xml")
    en = read(APP_SRC / "main/res/values-en/strings.xml")
    for label, text in (("pl", pl), ("en", en)):
        # remove app_name, privacy_policy_url, category_*
        text = re.sub(r'\s*<string name="app_name">.*?</string>\n?', "\n", text)
        text = re.sub(r'\s*<string name="privacy_policy_url"[^>]*>.*?</string>\n?', "\n", text)
        text = re.sub(r'\s*<string name="category_[^"]+">.*?</string>\n?', "\n", text)
        folder = "values" if label == "pl" else "values-en"
        write(ROOT / f"core/ui/src/main/res/{folder}/strings.xml", text)

    fmt = read(APP_SRC / "main/java/com/budzetdomowy/app/util/Formatters.kt")
    fmt = transform_kotlin(fmt, "com.budzetdomowy.core.ui.util", "com.budzetdomowy.core.data.R")
    write(ROOT / "core/ui/src/main/java/com/budzetdomowy/core/ui/util/Formatters.kt", fmt)

    theme = transform_kotlin(
        read(APP_SRC / "main/java/com/budzetdomowy/app/ui/theme/Theme.kt"),
        "com.budzetdomowy.core.ui.theme",
        "com.budzetdomowy.core.ui.R",
    )
    write(ROOT / "core/ui/src/main/java/com/budzetdomowy/core/ui/theme/Theme.kt", theme)

    scroll = transform_kotlin(
        read(APP_SRC / "main/java/com/budzetdomowy/app/ui/ScrollColumn.kt"),
        "com.budzetdomowy.core.ui",
        "com.budzetdomowy.core.ui.R",
    )
    write(ROOT / "core/ui/src/main/java/com/budzetdomowy/core/ui/ScrollColumn.kt", scroll)

    for test in ("MoneyFormatTest.kt", "SummarizeTest.kt"):
        copy_src(
            APP_SRC / "test/java/com/budzetdomowy/app/util" / test,
            ROOT / "core/ui/src/test/java/com/budzetdomowy/core/ui/util" / test,
            "com.budzetdomowy.core.ui.util",
            "com.budzetdomowy.core.ui.R",
        )

    # features
    setup_feature(
        "home",
        [
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/home/HomeScreen.kt", "HomeScreen.kt"),
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/home/HomeViewModel.kt", "HomeViewModel.kt"),
        ],
        "HomeModule.kt",
        """package com.budzetdomowy.feature.home

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel { HomeViewModel(get()) }
}
""",
    )
    setup_feature(
        "transactions",
        [
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/add/EditTransactionScreen.kt", "EditTransactionScreen.kt"),
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/add/EditTransactionViewModel.kt", "EditTransactionViewModel.kt"),
        ],
        "TransactionsModule.kt",
        """package com.budzetdomowy.feature.transactions

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val transactionsModule = module {
    viewModel { params -> EditTransactionViewModel(get(), params.get()) }
}
""",
    )
    setup_feature(
        "categories",
        [
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/categories/CategoriesScreen.kt", "CategoriesScreen.kt"),
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/categories/CategoriesViewModel.kt", "CategoriesViewModel.kt"),
        ],
        "CategoriesModule.kt",
        """package com.budzetdomowy.feature.categories

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val categoriesModule = module {
    viewModel { CategoriesViewModel(get()) }
}
""",
    )
    setup_feature(
        "goals",
        [
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/goals/GoalsScreen.kt", "GoalsScreen.kt"),
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/goals/GoalsViewModel.kt", "GoalsViewModel.kt"),
        ],
        "GoalsModule.kt",
        """package com.budzetdomowy.feature.goals

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val goalsModule = module {
    viewModel { GoalsViewModel(get()) }
    viewModel { params -> GoalDetailViewModel(get(), params.get()) }
}
""",
    )
    # GoalsScreen.kt may contain GoalDetailScreen - check; GoalsViewModel contains GoalDetailViewModel
    setup_feature(
        "recurring",
        [
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/recurring/RecurringScreen.kt", "RecurringScreen.kt"),
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/recurring/RecurringViewModel.kt", "RecurringViewModel.kt"),
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/recurring/EditRecurringScreen.kt", "EditRecurringScreen.kt"),
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/recurring/EditRecurringViewModel.kt", "EditRecurringViewModel.kt"),
        ],
        "RecurringModule.kt",
        """package com.budzetdomowy.feature.recurring

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val recurringModule = module {
    viewModel { RecurringViewModel(get()) }
    viewModel { params -> EditRecurringViewModel(get(), params.get()) }
}
""",
    )
    setup_feature(
        "report",
        [
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/report/ReportScreen.kt", "ReportScreen.kt"),
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/report/ReportViewModel.kt", "ReportViewModel.kt"),
        ],
        "ReportModule.kt",
        """package com.budzetdomowy.feature.report

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reportModule = module {
    viewModel { ReportViewModel(androidApplication(), get()) }
}
""",
        unit_test=(
            APP_SRC / "test/java/com/budzetdomowy/app/ui/report/ReportRangeTest.kt",
            "ReportRangeTest.kt",
        ),
    )
    setup_feature(
        "settings",
        [
            (APP_SRC / "main/java/com/budzetdomowy/app/ui/settings/SettingsScreen.kt", "SettingsScreen.kt"),
        ],
        "SettingsModule.kt",
        """package com.budzetdomowy.feature.settings

import org.koin.dsl.module

val settingsModule = module { }
""",
    )

    # Split GoalDetailScreen if in separate file - GoalsScreen.kt may have both
    goals_dir = APP_SRC / "main/java/com/budzetdomowy/app/ui/goals"
    for p in goals_dir.glob("*.kt"):
        dest = ROOT / "feature/goals/src/main/java/com/budzetdomowy/feature/goals" / p.name
        if not dest.exists() or p.name == "GoalsScreen.kt":
            copy_src(p, dest, "com.budzetdomowy.feature.goals", "com.budzetdomowy.core.ui.R")

    # app shell sources
    write(
        ROOT / "app/src/main/java/com/budzetdomowy/app/BudzetApp.kt",
        """package com.budzetdomowy.app

import android.app.Application
import com.budzetdomowy.core.data.coreDataModule
import com.budzetdomowy.feature.categories.categoriesModule
import com.budzetdomowy.feature.goals.goalsModule
import com.budzetdomowy.feature.home.homeModule
import com.budzetdomowy.feature.recurring.recurringModule
import com.budzetdomowy.feature.report.reportModule
import com.budzetdomowy.feature.settings.settingsModule
import com.budzetdomowy.feature.transactions.transactionsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class BudzetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@BudzetApp)
            modules(
                coreDataModule,
                homeModule,
                transactionsModule,
                categoriesModule,
                goalsModule,
                recurringModule,
                reportModule,
                settingsModule,
            )
        }
    }
}
""",
    )
    write(
        ROOT / "app/src/main/java/com/budzetdomowy/app/MainActivity.kt",
        """package com.budzetdomowy.app

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
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budzetdomowy.core.data.ThemePreferences
import com.budzetdomowy.core.ui.theme.BudzetTheme
import com.budzetdomowy.feature.categories.CategoriesScreen
import com.budzetdomowy.feature.goals.GoalDetailScreen
import com.budzetdomowy.feature.goals.GoalsScreen
import com.budzetdomowy.feature.home.HomeScreen
import com.budzetdomowy.feature.recurring.EditRecurringScreen
import com.budzetdomowy.feature.recurring.RecurringScreen
import com.budzetdomowy.feature.report.ReportScreen
import com.budzetdomowy.feature.settings.SettingsScreen
import com.budzetdomowy.feature.transactions.EditTransactionScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        setContent {
            val themePreferences: ThemePreferences = koinInject()
            val themeMode by themePreferences.mode.collectAsStateWithLifecycle()
            BudzetTheme(themeMode = themeMode) {
                BudzetNavHost(themePreferences)
            }
        }
    }
}

@Composable
private fun BudzetNavHost(themePreferences: ThemePreferences) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = koinViewModel(),
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
            EditTransactionScreen(
                viewModel = koinViewModel { parametersOf(id) },
                onDone = { navController.popBackStack() }
            )
        }
        composable("report") {
            ReportScreen(
                viewModel = koinViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            val themeMode by themePreferences.mode.collectAsStateWithLifecycle()
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = themePreferences::setMode,
                onBack = { navController.popBackStack() },
                onCategories = { navController.navigate("categories") },
                onRecurring = { navController.navigate("recurring") },
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
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() },
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
}
""",
    )

    write(
        ROOT / "app/build.gradle.kts",
        """import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.budzetdomowy.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.budzetdomowy.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)

    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:transactions"))
    implementation(project(":feature:categories"))
    implementation(project(":feature:goals"))
    implementation(project(":feature:recurring"))
    implementation(project(":feature:report"))
    implementation(project(":feature:settings"))

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    val koinBom = platform("io.insert-koin:koin-bom:4.2.2")
    implementation(koinBom)
    implementation("io.insert-koin:koin-android")
    implementation("io.insert-koin:koin-androidx-compose")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
""",
    )

    # slim app strings
    write(
        ROOT / "app/src/main/res/values/strings.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Budżet Domowy</string>
    <string name="privacy_policy_url" translatable="false">https://outsider710.github.io/budzet-domowy/</string>
</resources>
""",
    )
    write(
        ROOT / "app/src/main/res/values-en/strings.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Home Budget</string>
</resources>
""",
    )

    # delete old app sources that moved
    remove_paths = [
        APP_SRC / "main/java/com/budzetdomowy/app/data",
        APP_SRC / "main/java/com/budzetdomowy/app/di",
        APP_SRC / "main/java/com/budzetdomowy/app/util",
        APP_SRC / "main/java/com/budzetdomowy/app/ui",
        APP_SRC / "test",
        APP_SRC / "androidTest",
    ]
    for p in remove_paths:
        if p.exists():
            shutil.rmtree(p)

    print("Modularization files written.")


if __name__ == "__main__":
    main()
