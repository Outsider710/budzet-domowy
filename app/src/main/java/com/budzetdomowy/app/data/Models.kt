package com.budzetdomowy.app.data

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class Category(val labelPl: String, val forType: TransactionType) {
    FOOD("Jedzenie", TransactionType.EXPENSE),
    TRANSPORT("Transport", TransactionType.EXPENSE),
    BILLS("Rachunki", TransactionType.EXPENSE),
    ENTERTAINMENT("Rozrywka", TransactionType.EXPENSE),
    OTHER("Inne", TransactionType.EXPENSE),
    SALARY("Przychód", TransactionType.INCOME);

    companion object {
        fun forType(type: TransactionType): List<Category> =
            entries.filter { it.forType == type }
    }
}
