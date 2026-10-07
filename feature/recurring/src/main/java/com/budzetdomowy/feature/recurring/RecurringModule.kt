package com.budzetdomowy.feature.recurring

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val recurringModule = module {
    viewModel { RecurringViewModel(get()) }
    viewModel { params -> EditRecurringViewModel(get(), params.get()) }
}
