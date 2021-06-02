package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import io.reactivex.Observable

data class Track(
        val segments: Observable<TrackSegment>,
        val name: String? = null,
        val desc: String? = null,
        val cmt: String? = null,
        val src: String? = null,
        val number: Int? = null,
        val link: String? = null,
        val type: String? = null
) : XmlWritable {

    override val writeOperations: Observable<XmlWrite>
        get() = newTag(TAG_TRACK,
                optionalTagWithText(TAG_NAME, name),
                optionalTagWithText(TAG_NUMBER, number?.toString()),
                optionalTagWithText(TAG_DESC, desc),
                optionalTagWithText(TAG_CMT, cmt),
                optionalTagWithText(TAG_SRC, src),
                optionalTagWithText(TAG_TYPE, type),
                segments.concatMap { it.writeOperations }
        )

    class Builder {
        var name: String? = null
        var segments: List<TrackSegment>? = null
        var desc: String? = null
        var cmt: String? = null
        var src: String? = null
        var number: Int? = null
        var link: String? = null
        var type: String? = null


        fun build(): Track {
            return Track(
                    name = name,
                    desc = desc,
                    cmt = cmt,
                    src = src,
                    number = number,
                    link = link,
                    type = type,
                    segments = Observable.fromIterable(segments)
            )
        }
    }

    companion object {
        const val TAG_TRACK = "trk"
        const val TAG_NUMBER = "number"
        const val TAG_NAME = "name"
        const val TAG_CMT = "cmt"
        const val TAG_SRC = "src"
        const val TAG_TYPE = "type"
        const val TAG_DESC = "desc"
    }
}