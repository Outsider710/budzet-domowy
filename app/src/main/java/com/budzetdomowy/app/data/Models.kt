package com.budzetdomowy.app.data

import androidx.annotation.StringRes
import com.budzetdomowy.app.R

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class BuiltInCategory(
    @StringRes val labelRes: Int,
    val forType: TransactionType
) {
    FOOD(R.string.category_food, TransactionType.EXPENSE),
    TRANSPORT(R.string.category_transport, TransactionType.EXPENSE),
    BILLS(R.string.category_bills, TransactionType.EXPENSE),
    ENTERTAINMENT(R.string.category_entertainment, TransactionType.EXPENSE),
    OTHER(R.string.category_other, TransactionType.EXPENSE),
    SALARY(R.string.category_salary, TransactionType.INCOME);

    companion object {
        fun fromKey(key: String?): BuiltInCategory? =
            key?.let { value -> entries.find { it.name == value } }
    }
}
