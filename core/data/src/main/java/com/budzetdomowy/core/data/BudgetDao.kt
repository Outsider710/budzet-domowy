package com.budzetdomowy.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query(
        """
        SELECT * FROM transactions
        WHERE epochDay BETWEEN :startInclusive AND :endInclusive
        ORDER BY epochDay DESC, id DESC
        """
    )
    fun observeTransactionsBetween(startInclusive: Long, endInclusive: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransaction(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(entity: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(entity: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(entity: TransactionEntity)

    @Query("SELECT * FROM categories ORDER BY type ASC, id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY type ASC, id ASC")
    fun observeActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories WHERE type = :type AND archived = 0")
    suspend fun countActiveCategories(type: TransactionType): Int

    @Insert
    suspend fun insertCategory(entity: CategoryEntity): Long

    @Update
    suspend fun updateCategory(entity: CategoryEntity)

    @Query("SELECT * FROM category_budgets")
    fun observeBudgets(): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(entity: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets WHERE categoryId = :categoryId")
    suspend fun deleteBudget(categoryId: Long)

    @Query(
        """
        SELECT g.id, g.name, g.targetCents, g.archived,
            IFNULL((SELECT SUM(amountCents) FROM goal_contributions WHERE goalId = g.id), 0) AS savedCents
        FROM savings_goals g
        WHERE g.archived = 0
        ORDER BY g.id DESC
        """
    )
    fun observeActiveGoals(): Flow<List<GoalWithSaved>>

    @Query(
        """
        SELECT g.id, g.name, g.targetCents, g.archived,
            IFNULL((SELECT SUM(amountCents) FROM goal_contributions WHERE goalId = g.id), 0) AS savedCents
        FROM savings_goals g
        WHERE g.id = :id
        LIMIT 1
        """
    )
    fun observeGoal(id: Long): Flow<GoalWithSaved?>

    @Insert
    suspend fun insertGoal(entity: SavingsGoalEntity): Long

    @Update
    suspend fun updateGoal(entity: SavingsGoalEntity)

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getGoal(id: Long): SavingsGoalEntity?

    @Query(
        """
        SELECT * FROM goal_contributions
        WHERE goalId = :goalId
        ORDER BY epochDay DESC, id DESC
        """
    )
    fun observeContributions(goalId: Long): Flow<List<GoalContributionEntity>>

    @Query(
        """
        SELECT IFNULL(SUM(amountCents), 0) FROM goal_contributions
        WHERE epochDay BETWEEN :startInclusive AND :endInclusive
        """
    )
    fun observeContributionsSum(startInclusive: Long, endInclusive: Long): Flow<Long>

    @Insert
    suspend fun insertContribution(entity: GoalContributionEntity): Long

    @Delete
    suspend fun deleteContribution(entity: GoalContributionEntity)

    @Query("SELECT * FROM recurring_rules ORDER BY id DESC")
    fun observeRecurringRules(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE active = 1")
    suspend fun getActiveRecurringRules(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun getRecurringRule(id: Long): RecurringRuleEntity?

    @Insert
    suspend fun insertRecurringRule(entity: RecurringRuleEntity): Long

    @Update
    suspend fun updateRecurringRule(entity: RecurringRuleEntity)

    @Delete
    suspend fun deleteRecurringRule(entity: RecurringRuleEntity)
}
