package com.budzetdomowy.feature.categories

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val categoriesModule = module {
    viewModel { CategoriesViewModel(get()) }
}
