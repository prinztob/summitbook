package de.drtobiasprinz.summitbook.ui

import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ListAdapter
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits


class InstantAutoComplete : androidx.appcompat.widget.AppCompatAutoCompleteTextView {
    constructor(context: Context) : super(context)
    constructor(arg0: Context, arg1: AttributeSet?) : super(arg0, arg1)
    constructor(arg0: Context, arg1: AttributeSet?, arg2: Int) : super(arg0, arg1, arg2)

    override fun enoughToFilter(): Boolean {
        return true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int,
                                previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused && filter != null) {
            performFiltering(text, 0)
        }
        val listAdapter: ListAdapter = getAdapter()
        for (i in 0 until listAdapter.getCount()) {
            val temp: String = listAdapter.getItem(i).toString()
            if (text.toString().compareTo(temp) == 0) {
                return
            }
        }
        text.clear()
    }

    fun onRightDrawableClicked(onClicked: (view: InstantAutoComplete) -> Unit) {
        this.setOnTouchListener { v, event ->
            var hasConsumed = false
            if (v is InstantAutoComplete) {
                if (event.x <= v.totalPaddingLeft) {
                    if (event.action == MotionEvent.ACTION_UP) {
                        onClicked(this)
                    }
                    hasConsumed = true
                }
            }
            hasConsumed
        }
    }

}