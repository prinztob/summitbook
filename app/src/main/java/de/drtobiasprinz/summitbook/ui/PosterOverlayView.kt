package de.drtobiasprinz.summitbook.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.Poster
import kotlinx.android.synthetic.main.view_poster_overlay.view.*

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
        posterOverlayDescriptionText.text = poster.description
    }
}
