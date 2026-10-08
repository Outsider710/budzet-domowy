package com.budzetdomowy.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromType(value: TransactionType): String = value.name
}

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CategoryBudgetEntity::class,
        SavingsGoalEntity::class,
        GoalContributionEntity::class,
        RecurringRuleEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        builtInKey TEXT,
                        type TEXT NOT NULL,
                        archived INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
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
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transactions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        epochDay INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO transactions_new (id, amountCents, type, categoryId, note, epochDay)
                    SELECT id, amountCents, type,
                        CASE category
                            WHEN 'FOOD' THEN 1
                            WHEN 'TRANSPORT' THEN 2
                            WHEN 'BILLS' THEN 3
                            WHEN 'ENTERTAINMENT' THEN 4
                            WHEN 'OTHER' THEN 5
                            WHEN 'SALARY' THEN 6
                            ELSE 5
                        END,
                        note, epochDay
                    FROM transactions
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS category_budgets (
                        categoryId INTEGER PRIMARY KEY NOT NULL,
                        limitCents INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS savings_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        targetCents INTEGER NOT NULL,
                        archived INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goal_contributions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalId INTEGER NOT NULL,
                        amountCents INTEGER NOT NULL,
                        epochDay INTEGER NOT NULL,
                        note TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        dayOfMonth INTEGER NOT NULL,
                        nextEpochDay INTEGER NOT NULL,
                        active INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recurring_rules ADD COLUMN endEpochDay INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recurring_rules ADD COLUMN startEpochDay INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE recurring_rules SET startEpochDay = nextEpochDay")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recurring_rules ADD COLUMN notifyEnabled INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budzet.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(CALLBACK)
                    .build()
                    .also { instance = it }
            }
    }
}
