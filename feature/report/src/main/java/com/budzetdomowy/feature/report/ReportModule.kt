package com.budzetdomowy.feature.report

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reportModule = module {
    viewModel { ReportViewModel(androidApplication(), get(), get()) }
}
