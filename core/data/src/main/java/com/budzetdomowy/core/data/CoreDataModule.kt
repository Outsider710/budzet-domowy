package com.budzetdomowy.core.data

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreDataModule = module {
    single { AppDatabase.get(androidContext()) }
    single { get<AppDatabase>().budgetDao() }
    single { BudgetRepository(get()) }
    single { ThemePreferences(androidContext()) }
    single { SelectedMonthStore() }
}
