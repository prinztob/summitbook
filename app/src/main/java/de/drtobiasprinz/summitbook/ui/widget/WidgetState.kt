package de.drtobiasprinz.summitbook.ui.widget

import androidx.compose.runtime.Immutable

@Immutable
data class WidgetData(
    val yearlyStats: YearlyStats = YearlyStats(),
    val monthlyStats: MonthlyStats? = null
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