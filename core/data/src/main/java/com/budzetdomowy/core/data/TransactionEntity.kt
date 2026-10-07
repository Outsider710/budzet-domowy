package com.budzetdomowy.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val type: TransactionType,
    val categoryId: Long,
    val note: String = "",
    val epochDay: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val builtInKey: String? = null,
    val type: TransactionType,
    val archived: Boolean = false
)

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey val categoryId: Long,
    val limitCents: Long
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetCents: Long,
    val archived: Boolean = false
)

@Entity(tableName = "goal_contributions")
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amountCents: Long,
    val epochDay: Long,
    val note: String = ""
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val type: TransactionType,
    val categoryId: Long,
    val note: String = "",
    val dayOfMonth: Int,
    val startEpochDay: Long,
    val nextEpochDay: Long,
    val active: Boolean = true,
    val endEpochDay: Long? = null
)

data class GoalWithSaved(
    val id: Long,
    val name: String,
    val targetCents: Long,
    val archived: Boolean,
    val savedCents: Long
)

data class DisplayTransaction(
    val entity: TransactionEntity,
    val category: CategoryEntity?
)
