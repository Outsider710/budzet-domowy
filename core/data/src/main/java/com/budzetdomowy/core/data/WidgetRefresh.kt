package com.budzetdomowy.core.data

/** Hook for refreshing home-screen widgets without depending on UI modules. */
fun interface WidgetRefresh {
    fun requestUpdate()
}

object NoOpWidgetRefresh : WidgetRefresh {
    override fun requestUpdate() = Unit
}
