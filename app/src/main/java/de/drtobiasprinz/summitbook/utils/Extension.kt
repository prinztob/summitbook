package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.fragment.app.FragmentActivity

fun View.isVisible(isShowLoading: Boolean, container: View) {
    if (isShowLoading) {
        this.visibility = View.VISIBLE
        container.visibility = View.GONE
    } else {
        this.visibility = View.GONE
        container.visibility = View.VISIBLE
    }
}

/**
 * Extension function to find FragmentActivity from Context.
 * This replaces the deprecated internal Hilt API FragmentComponentManager.findActivity()
 */
fun Context?.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}