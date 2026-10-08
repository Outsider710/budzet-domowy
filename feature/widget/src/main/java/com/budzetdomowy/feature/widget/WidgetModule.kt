package com.budzetdomowy.feature.widget

import com.budzetdomowy.core.data.WidgetRefresh
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val widgetModule = module {
    single { MonthSnapshotLoader(get()) }
    single<WidgetRefresh> { GlanceWidgetRefresh(androidContext()) }
}
