package de.drtobiasprinz.summitbook.models

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackBoundingBoxTest {
    @Test
    @Throws(Exception::class)
    fun testIntersect() {
        val rect1 = TrackBoundingBox.Rectangle(11.756, 47.706, 12.131, 47.316)
        val rectIn = TrackBoundingBox.Rectangle(11.738, 47.517, 11.792, 47.504)
        val rectOut = TrackBoundingBox.Rectangle(8.960, 50.081, 9.095, 50.034)
        assertTrue(rect1.intersect(rectIn))
        assertFalse(rect1.intersect(rectOut))
    }
}