package com.budzetdomowy.feature.widget

import android.content.Context
import android.content.Intent

object WidgetNav {
    const val EXTRA_NAVIGATE = "com.budzetdomowy.extra.NAVIGATE"
    const val EXTRA_TRANSACTION_ID = "com.budzetdomowy.extra.TRANSACTION_ID"

    const val DEST_HOME = "home"
    const val DEST_EDIT = "edit"
    const val DEST_RECURRING = "recurring"

    private const val MAIN_ACTIVITY = "com.budzetdomowy.app.MainActivity"

    fun homeIntent(context: Context): Intent =
        baseIntent(context).putExtra(EXTRA_NAVIGATE, DEST_HOME)

    fun addExpenseIntent(context: Context): Intent =
        baseIntent(context)
            .putExtra(EXTRA_NAVIGATE, DEST_EDIT)
            .putExtra(EXTRA_TRANSACTION_ID, 0L)

    fun recurringIntent(context: Context): Intent =
        baseIntent(context).putExtra(EXTRA_NAVIGATE, DEST_RECURRING)

    fun parseDestination(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_NAVIGATE)

    fun parseTransactionId(intent: Intent?): Long =
        intent?.getLongExtra(EXTRA_TRANSACTION_ID, 0L) ?: 0L

    private fun baseIntent(context: Context): Intent =
        Intent().setClassName(context.packageName, MAIN_ACTIVITY).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
}
