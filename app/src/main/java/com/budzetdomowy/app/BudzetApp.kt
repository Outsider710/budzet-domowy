package com.budzetdomowy.app

import android.app.Application
import com.budzetdomowy.app.data.AppDatabase
import com.budzetdomowy.app.data.TransactionRepository

class BudzetApp : Application() {
    lateinit var repository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TransactionRepository(AppDatabase.get(this).transactionDao())
    }
}
