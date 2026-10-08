package com.budzetdomowy.app

import android.app.Application
import com.budzetdomowy.core.data.coreDataModule
import com.budzetdomowy.feature.categories.categoriesModule
import com.budzetdomowy.feature.goals.goalsModule
import com.budzetdomowy.feature.home.homeModule
import com.budzetdomowy.feature.recurring.RecurringNotificationHelper
import com.budzetdomowy.feature.recurring.RecurringNotifyWorker
import com.budzetdomowy.feature.recurring.recurringModule
import com.budzetdomowy.feature.report.reportModule
import com.budzetdomowy.feature.settings.settingsModule
import com.budzetdomowy.feature.transactions.transactionsModule
import com.budzetdomowy.feature.widget.widgetModule
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
                widgetModule,
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
        RecurringNotificationHelper.ensureChannel(this)
        RecurringNotifyWorker.enqueuePeriodic(this)
        RecurringNotifyWorker.enqueueNow(this)
    }
}
