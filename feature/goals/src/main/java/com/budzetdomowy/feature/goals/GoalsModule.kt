package com.budzetdomowy.feature.goals

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val goalsModule = module {
    viewModel { GoalsViewModel(get()) }
    viewModel { params -> GoalDetailViewModel(get(), params.get()) }
}
