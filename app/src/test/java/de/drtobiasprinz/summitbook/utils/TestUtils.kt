package de.drtobiasprinz.summitbook.utils

import java.io.InputStreamReader

object TestUtils {
    fun loadTestJson(resourceName: String = "activity_with_cycling_dynamics.json"): String {
        val inputStream = TestUtils::class.java.classLoader?.getResourceAsStream(resourceName)
            ?: throw IllegalStateException("Test JSON '$resourceName' not found")
        return InputStreamReader(inputStream).use { it.readText() }
    }
}