package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import io.reactivex.Observable
import java.text.FieldPosition
import java.text.SimpleDateFormat
import java.util.*

/**
 * @param date: Timestamp in milliseconds since epoch
 */
data class Time(val date: Long?) : XmlWritable {

    companion object {
        const val TAG_TIME = "time"
        private val dateFormat = object : SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH) {

            init {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            // Workaround to generate a 'Z' at the end of the string to denote UTC
            // which does not rely on the 'X' format string character (only available from Android 24)
            // See: https://developer.android.com/reference/java/text/SimpleDateFormat.html
            override fun format(date: Date, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
                return super.format(date, toAppendTo, pos).append('Z')
            }

        }


        @JvmStatic
        fun parse(time: String): Time {
            return Time(dateFormat.parse(time).time)
        }
    }

    override val writeOperations: Observable<XmlWrite>
        get() = if (date != null) newTag(TAG_TIME, withText(dateFormat.format(Date(date))))
                else Observable.empty()

}
