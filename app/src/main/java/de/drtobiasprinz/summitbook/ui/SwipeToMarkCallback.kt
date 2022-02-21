package de.drtobiasprinz.summitbook.ui

import android.content.Context
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter
import de.drtobiasprinz.summitbook.models.Summit
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator


class SwipeToMarkCallback(private val mAdapter: SummitViewAdapter, val context: Context) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    private var summit: Summit? = null
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        val entry = mAdapter.getItem(position)
        summit = entry
        if (entry != null) {
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    mAdapter.updateIsPeak(entry, position)
                }
                ItemTouchHelper.RIGHT -> {
                    mAdapter.updateIsFavorite(entry, position)
                }
            }
        }
    }


    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                .addSwipeLeftBackgroundColor(ContextCompat.getColor(context, R.color.blue_500))
                .addSwipeLeftActionIcon(if (summit?.isPeak == true) R.drawable.icons8_valley_24 else R.drawable.icons8_mountain_24)
                .addSwipeRightBackgroundColor(ContextCompat.getColor(context, R.color.green_500))
                .addSwipeRightActionIcon(if (summit?.isFavorite == true) R.drawable.ic_star_border_black_24dp else R.drawable.ic_star_black_24dp)
                .create()
                .decorate()

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

    }

}