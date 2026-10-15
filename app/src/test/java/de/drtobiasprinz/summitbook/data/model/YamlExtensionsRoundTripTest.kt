package de.drtobiasprinz.summitbook.data.model

import de.drtobiasprinz.summitbook.data.db.entities.RoadType
import de.drtobiasprinz.summitbook.data.db.entities.Surface
import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlDecodingException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Documents how lenient the YAML extensions reader is:
 * missing fields, unknown keys, duplicates and truncated files decode
 * fine (defaults are used), but a value that does not fit its field type
 * makes decoding the whole file fail - the app then falls back to empty
 * extensions and that activity loses its per-point data. This is why the
 * YAML files must always be written atomically.
 */
@RunWith(RobolectricTestRunner::class)
class YamlExtensionsRoundTripTest {

    private fun assertDecodes(yaml: String): ExtensionsFromYaml =
        Yaml.Default.decodeFromString(ExtensionsFromYaml.serializer(), yaml)

    @Test
    fun testRoundTripWithRealisticDistances() {
        val extensions = (0 until 4300).map { i ->
            ExtensionFromYaml(
                distance = i * 12.3456789,
                hr = if (i % 7 == 0) 0 else 120 + (i % 40),
                power = if (i % 5 == 0) 0 else 200 + (i % 100),
                power60s = 0,
                cadence = if (i % 3 == 0) 0 else 85,
                slope = if (i % 11 == 0) 0.0 else -1.2 + (i % 30) * 0.1,
                speed = if (i % 13 == 0) 0.0 else 5.0 + (i % 20) * 0.3,
                verticalVelocity = if (i % 17 == 0) 0.0 else (i % 9) * 0.05 - 0.2,
                surface = if (i % 4 == 0) Surface.LOSE_GROUND else Surface.ASPHALT,
                roadType = if (i % 6 == 0) RoadType.CYCLE_WAY else RoadType.SIDE_STREET
            )
        }
        val yamlString = Yaml.encodeToString(ExtensionsFromYaml(extensions))
        val decoded = Yaml.Default.decodeFromString(
            ExtensionsFromYaml.serializer(),
            yamlString
        )
        Assert.assertEquals(4300, decoded.extensions.size)
        Assert.assertEquals(Surface.LOSE_GROUND, decoded.extensions[4].surface)
        Assert.assertEquals(RoadType.SIDE_STREET, decoded.extensions.last().roadType)
    }

    @Test
    fun testTruncatedFileIsStillDecoded() {
        val yamlString = Yaml.encodeToString(
            ExtensionsFromYaml(
                (0 until 100).map {
                    ExtensionFromYaml(distance = it * 10.0, hr = 120)
                }
            )
        )
        // File ends in the middle of an element: the last elements simply
        // decode with defaults for the missing fields
        val truncated = yamlString.lines().dropLast(2).joinToString("\n")
        val decoded = assertDecodes(truncated)
        Assert.assertEquals(100, decoded.extensions.size)
    }

    @Test
    fun testUnknownKeysAndDuplicatesAreTolerated() {
        assertDecodes(
            "extensions: \n" +
                    "  - distance: 0.0\n" +
                    "    hr: 120\n" +
                    "    oldField: 5.0\n" +
                    "    hr: 121\n" +
                    "    surface: ASPHALT\n"
        ).also {
            Assert.assertEquals(1, it.extensions.size)
            Assert.assertEquals(Surface.ASPHALT, it.extensions.single().surface)
        }
    }

    @Test
    fun testFloatForIntFieldFailsWholeFile() {
        val element =
            "  - distance: 0.0\n" +
                    "    hr: 130.0\n" +
                    "    surface: ASPHALT\n"
        try {
            assertDecodes("extensions: \n" + element)
            Assert.fail("expected a YamlDecodingException")
        } catch (e: YamlDecodingException) {
            Assert.assertTrue(
                "message was: ${e.message}",
                e.message?.contains("deserializing nested class") == true
            )
        }
    }

    @Test
    fun testInvalidEnumValueFailsWholeFile() {
        try {
            assertDecodes(
                "extensions: \n" +
                        "  - distance: 0.0\n" +
                        "    hr: 120\n" +
                        "    surface: ASPHALT\n" +
                        "    roadType: NOT_A_ROAD_TYPE\n"
            )
            Assert.fail("expected a YamlDecodingException")
        } catch (e: YamlDecodingException) {
            Assert.assertNotNull(e.message)
        }
    }
}
