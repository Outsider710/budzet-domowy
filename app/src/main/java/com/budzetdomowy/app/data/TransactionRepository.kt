package com.budzetdomowy.app.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {
    fun observeMonth(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionEntity>> =
        dao.observeBetween(startEpochDay, endEpochDay)

    suspend fun get(id: Long): TransactionEntity? = dao.getById(id)

    suspend fun save(entity: TransactionEntity) {
        if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
        }
    }

    suspend fun delete(entity: TransactionEntity) = dao.delete(entity)
}
