package com.budzetdomowy.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: BudgetRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(
                object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            INSERT INTO categories (id, name, builtInKey, type, archived) VALUES
                            (1, '', 'FOOD', 'EXPENSE', 0),
                            (2, '', 'TRANSPORT', 'EXPENSE', 0),
                            (3, '', 'BILLS', 'EXPENSE', 0),
                            (4, '', 'ENTERTAINMENT', 'EXPENSE', 0),
                            (5, '', 'OTHER', 'EXPENSE', 0),
                            (6, '', 'SALARY', 'INCOME', 0)
                            """.trimIndent()
                        )
                    }
                }
            )
            .build()
        db.openHelper.writableDatabase
        repo = BudgetRepository(db.budgetDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun goalContributionIsNotAnExpense() = runTest {
        val goalId = repo.saveGoal(SavingsGoalEntity(name = "Wakacje", targetCents = 500_000))
        repo.addContribution(
            GoalContributionEntity(
                goalId = goalId,
                amountCents = 10_000,
                epochDay = LocalDate.of(2026, 10, 1).toEpochDay()
            )
        )
        repo.saveTransaction(
            TransactionEntity(
                amountCents = 2_000,
                type = TransactionType.EXPENSE,
                categoryId = 1,
                epochDay = LocalDate.of(2026, 10, 1).toEpochDay()
            )
        )
        val txs = repo.observeTransactionsBetween(
            LocalDate.of(2026, 10, 1).toEpochDay(),
            LocalDate.of(2026, 10, 31).toEpochDay()
        ).first()
        val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
        val saved = repo.observeContributionsSum(
            LocalDate.of(2026, 10, 1).toEpochDay(),
            LocalDate.of(2026, 10, 31).toEpochDay()
        ).first()
        val goals = repo.observeActiveGoals().first()
        assertEquals(2_000L, expense)
        assertEquals(10_000L, saved)
        assertEquals(10_000L, goals.single().savedCents)
    }

    @Test
    fun cannotArchiveLastExpenseCategory() = runTest {
        val cats = repo.observeActiveCategories().first().filter { it.type == TransactionType.EXPENSE }
        cats.dropLast(1).forEach { repo.setCategoryArchived(it, true) }
        val last = repo.observeActiveCategories().first().single { it.type == TransactionType.EXPENSE }
        assertFalse(repo.setCategoryArchived(last, true))
        val remaining = repo.observeActiveCategories().first().count { it.type == TransactionType.EXPENSE }
        assertEquals(1, remaining)
    }

    @Test
    fun customCategoryAndBudget() = runTest {
        repo.addCategory("Zdrowie", TransactionType.EXPENSE)
        val custom = repo.observeActiveCategories().first().single { it.name == "Zdrowie" }
        repo.setBudget(custom.id, 20_000)
        val budgets = repo.observeBudgets().first()
        assertEquals(20_000L, budgets.single { it.categoryId == custom.id }.limitCents)
        repo.setBudget(custom.id, null)
        assertTrue(repo.observeBudgets().first().none { it.categoryId == custom.id })
    }

    @Test
    fun materializeDueInsertsMissedMonths() = runTest {
        val start = LocalDate.of(2026, 8, 10)
        repo.addRecurringRule(
            RecurringRuleEntity(
                amountCents = 8_000,
                type = TransactionType.EXPENSE,
                categoryId = 3,
                note = "Netflix",
                dayOfMonth = 10,
                startEpochDay = start.toEpochDay(),
                nextEpochDay = start.toEpochDay(),
                active = true
            )
        )
        repo.materializeDue(LocalDate.of(2026, 10, 10))
        val txs = repo.observeTransactionsBetween(
            LocalDate.of(2026, 8, 1).toEpochDay(),
            LocalDate.of(2026, 10, 31).toEpochDay()
        ).first()
        assertEquals(3, txs.size)
        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 10, 10)
            ),
            txs.map { LocalDate.ofEpochDay(it.epochDay) }.sorted()
        )
        val rule = repo.observeRecurringRules().first().single()
        assertEquals(LocalDate.of(2026, 11, 10), LocalDate.ofEpochDay(rule.nextEpochDay))
    }

    @Test
    fun materializeDueStopsAtEndDate() = runTest {
        val start = LocalDate.of(2026, 8, 10)
        repo.addRecurringRule(
            RecurringRuleEntity(
                amountCents = 8_000,
                type = TransactionType.EXPENSE,
                categoryId = 3,
                note = "Netflix",
                dayOfMonth = 10,
                startEpochDay = start.toEpochDay(),
                nextEpochDay = start.toEpochDay(),
                active = true,
                endEpochDay = LocalDate.of(2026, 9, 10).toEpochDay()
            )
        )
        repo.materializeDue(LocalDate.of(2026, 10, 10))
        val txs = repo.observeTransactionsBetween(
            LocalDate.of(2026, 8, 1).toEpochDay(),
            LocalDate.of(2026, 10, 31).toEpochDay()
        ).first()
        assertEquals(2, txs.size)
        val rule = repo.observeRecurringRules().first().single()
        assertFalse(rule.active)
    }
}
