package com.budzetdomowy.core.data

import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BudgetRepository(
    private val dao: BudgetDao,
    private val widgetRefresh: WidgetRefresh = NoOpWidgetRefresh
) {
    fun observeTransactionsBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionEntity>> =
        dao.observeTransactionsBetween(startEpochDay, endEpochDay)

    fun observeDisplayTransactions(startEpochDay: Long, endEpochDay: Long): Flow<List<DisplayTransaction>> =
        combine(
            dao.observeTransactionsBetween(startEpochDay, endEpochDay),
            dao.observeCategories()
        ) { transactions, categories ->
            val byId = categories.associateBy { it.id }
            transactions.map { DisplayTransaction(it, byId[it.categoryId]) }
        }

    suspend fun getTransaction(id: Long): TransactionEntity? = dao.getTransaction(id)

    suspend fun saveTransaction(entity: TransactionEntity): Long {
        val id = if (entity.id == 0L) {
            dao.insertTransaction(entity)
        } else {
            dao.updateTransaction(entity)
            entity.id
        }
        notifyWidgets()
        return id
    }

    suspend fun deleteTransaction(entity: TransactionEntity) {
        dao.deleteTransaction(entity)
        notifyWidgets()
    }

    fun observeCategories(): Flow<List<CategoryEntity>> = dao.observeCategories()

    fun observeActiveCategories(): Flow<List<CategoryEntity>> = dao.observeActiveCategories()

    suspend fun addCategory(name: String, type: TransactionType) {
        dao.insertCategory(
            CategoryEntity(name = name.trim(), type = type, archived = false)
        )
    }

    suspend fun renameCategory(entity: CategoryEntity, name: String) {
        if (entity.builtInKey != null) return
        dao.updateCategory(entity.copy(name = name.trim()))
    }

    suspend fun setCategoryArchived(entity: CategoryEntity, archived: Boolean): Boolean {
        if (archived) {
            val remaining = dao.countActiveCategories(entity.type)
            if (remaining <= 1) return false
        }
        dao.updateCategory(entity.copy(archived = archived))
        return true
    }

    fun observeBudgets(): Flow<List<CategoryBudgetEntity>> = dao.observeBudgets()

    suspend fun setBudget(categoryId: Long, limitCents: Long?) {
        if (limitCents == null || limitCents <= 0L) {
            dao.deleteBudget(categoryId)
        } else {
            dao.upsertBudget(CategoryBudgetEntity(categoryId, limitCents))
        }
        notifyWidgets()
    }

    fun observeActiveGoals(): Flow<List<GoalWithSaved>> = dao.observeActiveGoals()

    fun observeGoal(id: Long): Flow<GoalWithSaved?> = dao.observeGoal(id)

    suspend fun saveGoal(entity: SavingsGoalEntity): Long {
        return if (entity.id == 0L) {
            dao.insertGoal(entity)
        } else {
            dao.updateGoal(entity)
            entity.id
        }
    }

    suspend fun archiveGoal(id: Long) {
        val goal = dao.getGoal(id) ?: return
        dao.updateGoal(goal.copy(archived = true))
    }

    fun observeContributions(goalId: Long): Flow<List<GoalContributionEntity>> =
        dao.observeContributions(goalId)

    fun observeContributionsSum(startEpochDay: Long, endEpochDay: Long): Flow<Long> =
        dao.observeContributionsSum(startEpochDay, endEpochDay)

    suspend fun addContribution(entity: GoalContributionEntity) {
        dao.insertContribution(entity)
        notifyWidgets()
    }

    suspend fun deleteContribution(entity: GoalContributionEntity) {
        dao.deleteContribution(entity)
        notifyWidgets()
    }

    fun observeRecurringRules(): Flow<List<RecurringRuleEntity>> = dao.observeRecurringRules()

    suspend fun addRecurringRule(entity: RecurringRuleEntity) {
        dao.insertRecurringRule(entity)
    }

    suspend fun getRecurringRule(id: Long): RecurringRuleEntity? = dao.getRecurringRule(id)

    suspend fun updateRecurringRule(entity: RecurringRuleEntity) {
        dao.updateRecurringRule(entity)
    }

    suspend fun setRecurringActive(rule: RecurringRuleEntity, active: Boolean) {
        dao.updateRecurringRule(rule.copy(active = active))
    }

    suspend fun setRecurringEnd(rule: RecurringRuleEntity, endEpochDay: Long?) {
        dao.updateRecurringRule(rule.copy(endEpochDay = endEpochDay))
    }

    suspend fun deleteRecurringRule(rule: RecurringRuleEntity) {
        dao.deleteRecurringRule(rule)
    }

    suspend fun materializeDue(today: LocalDate = LocalDate.now()) {
        val rules = dao.getActiveRecurringRules()
        var changedAny = false
        for (rule in rules) {
            val start = LocalDate.ofEpochDay(rule.startEpochDay)
            var next = LocalDate.ofEpochDay(rule.nextEpochDay)
            if (next.isBefore(start)) {
                next = occurrenceOnOrAfter(start, rule.dayOfMonth)
            }
            val end = rule.endEpochDay?.let { LocalDate.ofEpochDay(it) }
            var changed = false
            while (!next.isAfter(today) &&
                !next.isBefore(start) &&
                (end == null || !next.isAfter(end))
            ) {
                dao.insertTransaction(
                    TransactionEntity(
                        amountCents = rule.amountCents,
                        type = rule.type,
                        categoryId = rule.categoryId,
                        note = rule.note,
                        epochDay = next.toEpochDay()
                    )
                )
                next = nextOccurrence(next, rule.dayOfMonth)
                changed = true
                changedAny = true
            }
            val expired = end != null && next.isAfter(end)
            if (changed || expired) {
                dao.updateRecurringRule(
                    rule.copy(
                        nextEpochDay = next.toEpochDay(),
                        active = if (expired) false else rule.active
                    )
                )
            }
        }
        if (changedAny) notifyWidgets()
    }

    private fun notifyWidgets() {
        widgetRefresh.requestUpdate()
    }

    companion object {
        fun nextOccurrence(from: LocalDate, dayOfMonth: Int): LocalDate {
            val nextMonth = YearMonth.from(from).plusMonths(1)
            val day = dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth())
            return nextMonth.atDay(day)
        }

        /** First occurrence on or after [from] for a monthly day-of-month rule. */
        fun occurrenceOnOrAfter(from: LocalDate, dayOfMonth: Int): LocalDate {
            val month = YearMonth.from(from)
            val day = dayOfMonth.coerceIn(1, 31).coerceAtMost(month.lengthOfMonth())
            val candidate = month.atDay(day)
            return if (!candidate.isBefore(from)) candidate else nextOccurrence(from, dayOfMonth)
        }

        fun previewOccurrences(
            start: LocalDate,
            dayOfMonth: Int,
            end: LocalDate?,
            max: Int = 12
        ): List<LocalDate> {
            val out = mutableListOf<LocalDate>()
            var current = start
            repeat(max) {
                if (end != null && current.isAfter(end)) return out
                out += current
                current = nextOccurrence(current, dayOfMonth)
            }
            return out
        }
    }
}
