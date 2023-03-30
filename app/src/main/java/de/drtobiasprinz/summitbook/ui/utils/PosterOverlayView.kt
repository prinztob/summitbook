package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Poster

class PosterOverlayView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_poster_overlay, this)
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun update(poster: Poster) {
        findViewById<TextView>(R.id.posterOverlayDescriptionText).text = poster.description
    }
}
