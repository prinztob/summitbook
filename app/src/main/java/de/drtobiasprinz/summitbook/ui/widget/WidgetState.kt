package de.drtobiasprinz.summitbook.ui.widget

import androidx.compose.runtime.Immutable

@Immutable
data class WidgetData(
    val yearlyStats: YearlyStats = YearlyStats(),
    val monthlyStats: MonthlyStats? = null
)

@Immutable
data class YearlyStats(
    val activities: StatItem = StatItem(),
    val heightMeter: StatItem = StatItem(),
    val kilometers: StatItem = StatItem()
)

@Immutable
data class MonthlyStats(
    val activities: StatItem = StatItem(),
    val heightMeter: StatItem = StatItem(),
    val kilometers: StatItem = StatItem()
)

@Immutable
data class StatItem(
    val actualValue: Int = 0,
    val expectedValue: Int = 0,
    val unit: String = "",
    val isAchieved: Boolean = false
)