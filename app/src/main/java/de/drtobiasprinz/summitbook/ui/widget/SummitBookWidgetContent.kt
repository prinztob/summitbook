package de.drtobiasprinz.summitbook.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.MainActivityCompose

@Composable
fun SummitBookWidgetContent(
    widgetData: WidgetData
) {
    val context = LocalContext.current

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(Color.Transparent.copy(alpha = 0.7f), Color.Transparent.copy(alpha = 0.7f))
            .cornerRadius(16.dp)
            .padding(8.dp)
            .clickable(actionStartActivity<MainActivityCompose>())
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            // Yearly chart (if available)
            if (widgetData.yearlyChartBitmap != null) {
                Image(
                    provider = ImageProvider(widgetData.yearlyChartBitmap),
                    contentDescription = "Yearly overview chart",
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(175.dp)
                )
                Spacer(GlanceModifier.height(4.dp))
            }

            // Stats row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Yearly stats column
                StatsColumn(
                    title = context.getString(R.string.current_year),
                    stats = widgetData.yearlyStats
                )

                Spacer(GlanceModifier.width(8.dp))

                // Monthly stats column
                if (widgetData.monthlyStats != null) {
                    StatsColumn(
                        title = context.getString(R.string.current_month),
                        stats = widgetData.monthlyStats
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsColumn(
    title: String,
    stats: Stats
) {
    Column(
        modifier = GlanceModifier.width(175.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = TextStyle,
        )

        Spacer(GlanceModifier.height(4.dp))

        StatRow(
            statItem = stats.activities,
            iconRes = if (stats.activities.isAchieved) {
                R.drawable.ic_baseline_directions_run_24_green
            } else {
                R.drawable.ic_baseline_directions_run_24_red
            },
            label = ""
        )

        StatRow(
            statItem = stats.heightMeter,
            iconRes = if (stats.heightMeter.isAchieved) {
                R.drawable.ic_baseline_trending_up_24_green
            } else {
                R.drawable.ic_baseline_trending_up_24_red
            },
            label = "hm"
        )

        StatRow(
            statItem = stats.kilometers,
            iconRes = if (stats.kilometers.isAchieved) {
                R.drawable.ic_baseline_compare_arrows_24_green
            } else {
                R.drawable.ic_baseline_compare_arrows_24_red
            },
            label = "km"
        )
    }
}

@Composable
private fun StatRow(
    statItem: StatItem,
    iconRes: Int,
    label: String
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(16.dp)
        )

        Spacer(GlanceModifier.width(8.dp))

        Text(
            text = if (label.isEmpty()) {
                "${statItem.actualValue} ${LocalContext.current.getString(R.string.of)} ${statItem.expectedValue}"
            } else {
                "${statItem.actualValue} ${LocalContext.current.getString(R.string.of)} ${statItem.expectedValue} $label"
            },
            style = TextStyle
        )
    }

    Spacer(GlanceModifier.height(2.dp))
}

private val TextStyle = androidx.glance.text.TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Normal,
    textAlign = TextAlign.Start,
    color = ColorProvider(Color.White, Color.White)
)
