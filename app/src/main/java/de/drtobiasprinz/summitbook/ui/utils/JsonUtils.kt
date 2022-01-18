package de.drtobiasprinz.summitbook.ui.utils

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets


class JsonUtils {

    companion object {
        @JvmStatic
        fun getJsonData(file: File): String? {
            return try {
                val stream: InputStream = FileInputStream(file)
                val size = stream.available()
                val buffer = ByteArray(size)
                stream.read(buffer)
                stream.close()
                String(buffer, StandardCharsets.UTF_8)
            } catch (ex: IOException) {
                ex.printStackTrace()
                null
            }
        }
    }
}