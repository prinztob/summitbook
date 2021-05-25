package de.drtobiasprinz.summitbook.models

import android.graphics.Rect
import org.osmdroid.util.BoundingBox
import kotlin.math.ceil
import kotlin.math.floor

class TrackBoundingBox constructor(
        var latNorth: Double, var latSouth: Double, var lonWest: Double, var lonEast: Double
) {
    private var factor = 100
    var trackBoundingRect = Rect(floor(lonWest*factor).toInt(), ceil(latSouth*factor).toInt(), ceil(lonEast*factor).toInt(), floor(latNorth*factor).toInt())

    fun intersects(boundingBox: BoundingBox): Boolean {
        val boundingRect = Rect(floor(boundingBox.lonWest*factor).toInt(), ceil(boundingBox.latSouth*factor).toInt(), ceil(boundingBox.lonEast*factor).toInt(), floor(boundingBox.latNorth*factor).toInt())
        return boundingRect.intersect(trackBoundingRect)
    }

}