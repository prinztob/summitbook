package de.drtobiasprinz.summitbook.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Named replacements for the raw Color(0x…) literals that used to be
 * scattered across the chart screens. These are palette values for the
 * MPAndroidChart-backed diagrams and dark canvases, not semantic theme
 * roles — for those use MaterialTheme.colorScheme.
 */

// Record / highlight palette
val RecordGreen = Color(0xFF4CAF50)
val ChartOrange = Color(0xFFFF9800)
val ChartRed = Color(0xFFFF0000)
val ChartBlue = Color(0xFF0000FF)
val ChartGold = Color(0xFFFFD700)
val ChartLime = Color(0xFF00FF00)

// Dark canvases the charts draw on
val DarkCanvas = Color(0xFF1E1E1E)
val DarkCanvasDeep = Color(0xFF121212)
val DarkGrid = Color(0xFF333333)
val DarkCard = Color(0xFF2C2C2C)

// Chart text and grid neutrals
val ChartTextLightGray = Color(0xFFCCCCCC)
val ChartTextDarkGray = Color(0xFF444444)
val IconGray = Color(0xFF424242)
val SurfaceLightGray = Color(0xFFF5F5F5)
val SurfaceMidGray = Color(0xFFE0E0E0)

// Translucent scrim over map/chart backgrounds
val Scrim = Color(0x55000000)
