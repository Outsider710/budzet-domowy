package com.budzetdomowy.feature.recurring

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.budzetdomowy.core.data.BuiltInCategory
import com.budzetdomowy.core.data.BudgetRepository
import com.budzetdomowy.core.data.CategoryEntity
import com.budzetdomowy.core.data.RecurringRuleEntity
import com.budzetdomowy.core.data.ThemePreferences
import com.budzetdomowy.core.data.WidgetRefresh
import com.budzetdomowy.core.data.isDueOn
import com.budzetdomowy.core.ui.R as UiR
import com.budzetdomowy.core.ui.util.MoneyFormat
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

object RecurringNotificationHelper : KoinComponent {
    private const val CHANNEL_ID = "recurring_due"
    private const val NOTIFICATION_ID = 7101
    private const val PREFS = "recurring_notify_dedupe"
    private const val MAIN_ACTIVITY = "com.budzetdomowy.app.MainActivity"
    private const val EXTRA_NAVIGATE = "com.budzetdomowy.extra.NAVIGATE"
    private const val DEST_RECURRING = "recurring"

    private val repository: BudgetRepository by inject()
    private val themePreferences: ThemePreferences by inject()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(UiR.string.recurring_notify_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(UiR.string.recurring_notify_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    suspend fun notifyDueTodayIfNeeded(context: Context, today: LocalDate = LocalDate.now()) {
        ensureChannel(context)
        if (!themePreferences.areNotificationsEnabled()) return
        if (!canPostNotifications(context)) return

        val rules = repository.observeRecurringRules().first()
            .filter { it.notifyEnabled && it.isDueOn(today) }
        if (rules.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val todayEpoch = today.toEpochDay()
        val pending = rules.filter { rule ->
            prefs.getLong(dedupeKey(rule.id), Long.MIN_VALUE) != todayEpoch
        }
        if (pending.isEmpty()) return

        val categories = repository.observeCategories().first().associateBy { it.id }
        val lines = pending.map { rule ->
            val label = displayLabel(context, rule, categories[rule.categoryId])
            context.getString(
                UiR.string.recurring_notify_line,
                label,
                MoneyFormat.fromCents(rule.amountCents)
            )
        }

        val title = context.getString(UiR.string.recurring_notify_title)
        val body = if (lines.size == 1) {
            context.getString(UiR.string.recurring_notify_single, lines.single())
        } else {
            context.getString(UiR.string.recurring_notify_multi, lines.size) +
                "\n" + lines.joinToString("\n")
        }

        val intent = Intent().setClassName(context.packageName, MAIN_ACTIVITY).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAVIGATE, DEST_RECURRING)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = if (lines.size == 1) {
            body
        } else {
            context.getString(UiR.string.recurring_notify_multi, lines.size)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)

        prefs.edit().apply {
            pending.forEach { putLong(dedupeKey(it.id), todayEpoch) }
            apply()
        }

        runCatching { get<WidgetRefresh>().requestUpdate() }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun dedupeKey(ruleId: Long) = "notified_$ruleId"

    private fun displayLabel(
        context: Context,
        rule: RecurringRuleEntity,
        category: CategoryEntity?
    ): String {
        if (rule.note.isNotBlank()) return rule.note
        if (category == null) return MoneyFormat.fromCents(rule.amountCents)
        val builtIn = BuiltInCategory.fromKey(category.builtInKey)
        return if (builtIn != null) {
            context.getString(builtIn.labelRes)
        } else if (category.name.isNotBlank()) {
            category.name
        } else {
            MoneyFormat.fromCents(rule.amountCents)
        }
    }
}
