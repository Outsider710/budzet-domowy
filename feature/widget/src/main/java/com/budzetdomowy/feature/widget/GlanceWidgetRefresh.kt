package com.budzetdomowy.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.budzetdomowy.core.data.WidgetRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GlanceWidgetRefresh(
    private val context: Context
) : WidgetRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun requestUpdate() {
        scope.launch {
            BudzetAppWidget().updateAll(context.applicationContext)
        }
    }
}
