package de.drtobiasprinz.summitbook.widget

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import de.drtobiasprinz.summitbook.data.db.entities.Summit

@Immutable
data class WidgetData(
    val yearlyStats: YearlyStats = YearlyStats(),
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val yearlyChartBitmap: Bitmap? = null,
    val recentSummits: List<Summit> = emptyList(),
    /** True while data is still being loaded — the widget shows a loading hint
     *  instead of zeroed stats. */
    val isLoading: Boolean = false,
    /** True when loading failed — the widget should show an error hint
     *  instead of silently rendering zeroed stats. */
    val isError: Boolean = false
)
@Immutable
sealed class Stats {
    abstract val activities: StatItem
    abstract val heightMeter: StatItem
    abstract val kilometers: StatItem
}

@Immutable
data class YearlyStats(
    override val activities: StatItem = StatItem(),
    override val heightMeter: StatItem = StatItem(),
    override val kilometers: StatItem = StatItem()
) : Stats()

@Immutable
data class MonthlyStats(
    override val activities: StatItem = StatItem(),
    override val heightMeter: StatItem = StatItem(),
    override val kilometers: StatItem = StatItem()
) : Stats()

@Immutable
data class StatItem(
    val actualValue: Int = 0,
    val expectedValue: Int = 0,
    val unit: String = "",
    val isAchieved: Boolean = false
)