package de.drtobiasprinz.summitbook.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
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
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SummitBookWidgetContent(
    widgetData: WidgetData
) {
    val context = LocalContext.current

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(8.dp)
            .clickable(actionStartActivity<MainActivityCompose>())
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
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
                StatsColumn(
                    title = context.getString(R.string.current_month),
                    stats = widgetData.monthlyStats
                )
            }

            // Yearly chart (if available) - moved to bottom
            if (widgetData.yearlyChartBitmap != null) {
                Spacer(GlanceModifier.height(4.dp))
                Image(
                    provider = ImageProvider(widgetData.yearlyChartBitmap),
                    contentDescription = "Yearly overview chart",
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(175.dp)
                )
            }

            // Recent summits card
            if (widgetData.recentSummits.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                RecentSummitsCard(recentSummits = widgetData.recentSummits)
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
        modifier = GlanceModifier
            .width(175.dp)
            .background(DarkGray.copy(alpha = 0.5f), DarkGray.copy(alpha = 0.5f))
            .cornerRadius(12.dp)
            .padding(8.dp),
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

        val numberFormat = NumberFormat.getIntegerInstance()
        Text(
            text = if (label.isEmpty()) {
                "${numberFormat.format(statItem.actualValue)} ${LocalContext.current.getString(R.string.of)} ${numberFormat.format(statItem.expectedValue)}"
            } else {
                "${numberFormat.format(statItem.actualValue)} ${LocalContext.current.getString(R.string.of)} ${numberFormat.format(statItem.expectedValue)} $label"
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

@Composable
private fun RecentSummitsCard(
    recentSummits: List<Summit>
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(DarkGray.copy(alpha = 0.5f), DarkGray.copy(alpha = 0.5f))
            .cornerRadius(12.dp)
            .padding(8.dp)
    ) {
        Text(
            text = LocalContext.current.getString(R.string.recent_activities),
            style = TextStyle.copy(fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(bottom = 4.dp)
        )

        recentSummits.forEach { summit ->
            SummitItem(summit = summit)
            if (summit != recentSummits.last()) {
                Spacer(GlanceModifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SummitItem(
    summit: Summit
) {
    val garminData = summit.garminData
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sport type icon
        Image(
            provider = ImageProvider(summit.sportType.imageIdWhite),
            contentDescription = null,
            modifier = GlanceModifier.size(16.dp)
        )

        Spacer(GlanceModifier.width(8.dp))

        // Summit details
        Column(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Text(
                text = "${summit.getDateAsString()} ${summit.name}",
                style = TextStyle.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val numberFormat = NumberFormat.getIntegerInstance()
                Text(
                    text = "${numberFormat.format(summit.kilometers.roundToInt())} km",
                    style = TextStyle.copy(fontSize = 10.sp),
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = "${numberFormat.format(summit.elevationData.elevationGain)} hm",
                    style = TextStyle.copy(fontSize = 10.sp),
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = if (garminData?.power?.avgPower != null && garminData.power.avgPower > 0) {
                        "${numberFormat.format(garminData.power.avgPower.roundToInt())} W"
                    } else {
                        String.format(Locale.getDefault(), "%.1f km/h", summit.getAverageVelocity())
                    },
                    style = TextStyle.copy(fontSize = 10.sp),
                )
            }
        }
    }
}
