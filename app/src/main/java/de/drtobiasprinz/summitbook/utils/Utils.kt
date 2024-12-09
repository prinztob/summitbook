package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

object Utils {
    /** Make this package easy to copy to other apps by avoiding direct "R" references. (It
     * would be better to do this by distributing it along with its resources in an AAR, but
     * Chaquopy doesn't support getting Python code from an AAR yet.) */
    @JvmStatic
    fun resId(context: Context, type: String?, name: String?): Int {
        val resources = context.resources
        return resources.getIdentifier(name, type, context.applicationInfo.packageName)
    }

    @JvmStatic
    fun fixEdgeToEdge(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                topMargin = insets.top
                rightMargin = insets.right
            }
            windowInsets
        }
    }
}
