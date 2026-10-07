package com.budzetdomowy.feature.transactions

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val transactionsModule = module {
    viewModel { params -> EditTransactionViewModel(get(), params.get()) }
}
