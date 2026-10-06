package com.budzetdomowy.app

import android.app.Application
import com.budzetdomowy.app.data.AppDatabase
import com.budzetdomowy.app.data.BudgetRepository
import com.budzetdomowy.app.data.ThemePreferences

class BudzetApp : Application() {
    lateinit var repository: BudgetRepository
        private set
    lateinit var themePreferences: ThemePreferences
        private set

    override fun onCreate() {
        super.onCreate()
        repository = BudgetRepository(AppDatabase.get(this).budgetDao())
        themePreferences = ThemePreferences(this)
    }
}
