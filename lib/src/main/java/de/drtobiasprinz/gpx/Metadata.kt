package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import io.reactivex.Observable

data class Metadata(
    val name: String? = null,
    val description: String? = null,
    val author: String? = null
) : XmlWritable {

    private fun hasMetadata(): Boolean =
        name != null || description != null || author != null

    override val writeOperations: Observable<XmlWrite>
        get() = if (hasMetadata()) newTag(
            TAG_METADATA,
            optionalTagWithText(TAG_NAME, name),
            optionalTagWithText(TAG_DESC, description),
            optionalTagWithText(TAG_AUTHOR, author)
        ) else Observable.empty()

    class Builder {
        var name: String? = null
        var desc: String? = null
        var author: String? = null

        fun build(): Metadata {
            return Metadata(name = name, description = desc, author = author)
        }
    }

    companion object {
        const val TAG_METADATA = "metadata"
        const val TAG_NAME = "name"
        const val TAG_DESC = "desc"
        const val TAG_AUTHOR = "author"
    }
}
