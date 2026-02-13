package de.drtobiasprinz.summitbook.models

/**
 * Custom chart entry data class to replace com.github.mikephil.charting.data.Entry
 * Represents a single data point in a chart with x and y coordinates
 *
 * @param x The x-coordinate of the entry
 * @param y The y-coordinate of the entry
 * @param data Optional data object associated with this entry
 */
data class ChartEntry(
    var x: Float,
    var y: Float,
    var data: Any? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChartEntry

        if (x != other.x) return false
        if (y != other.y) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + (data?.hashCode() ?: 0)
        return result
    }
}