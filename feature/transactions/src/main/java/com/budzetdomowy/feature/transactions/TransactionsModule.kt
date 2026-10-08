package com.budzetdomowy.feature.transactions

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val transactionsModule = module {
    viewModel { params ->
        EditTransactionViewModel(
            repository = get(),
            transactionId = params.get(),
            defaultRepeatMonthly = params.getOrNull<Boolean>() ?: false
        )
    }
}
