package de.drtobiasprinz.summitbook.core.widget

/**
 * Abstraction for refreshing the app widget, so lower layers (e.g. ViewModels)
 * do not need to depend on the widget/Glance implementation.
 */
interface WidgetUpdater {
    suspend fun updateWidget()
}
