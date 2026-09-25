package de.drtobiasprinz.summitbook.ui.view

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.Poster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullscreenImageViewer(
    private val context: Context,
    private val resources: Resources
) {
    private var dialog: Dialog? = null
    private var viewPager: ViewPager2? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var isDialogShown = false
    var currentPosition: Int = 0
        private set

    fun show(
        images: MutableList<Poster>,
        startPosition: Int,
        sortFilterSummits: List<Summit>?,
        onPositionChanged: (Int) -> Unit = {}
    ) {
        if (isDialogShown) {
            return
        }

        dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog?.setContentView(R.layout.dialog_fullscreen_image_viewer)

        val viewPager = dialog?.findViewById<ViewPager2>(R.id.imageViewPager)
        this.viewPager = viewPager
        val descriptionText = dialog?.findViewById<TextView>(R.id.imageDescription)

        val adapter = PosterImageAdapter(images)
        viewPager?.adapter = adapter
        viewPager?.setCurrentItem(startPosition, false)

        // Update description and position on page change
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                images.getOrNull(position)?.let { descriptionText?.text = it.description }
                onPositionChanged(position)

                // Update images if list changed; rebuild off the main thread and
                // apply the result back on it
                val sizeBefore = images.size
                scope.launch {
                    val updatedImages = withContext(Dispatchers.Default) {
                        getAllImages(sortFilterSummits)
                    }
                    if (isDialogShown && updatedImages.size != sizeBefore) {
                        images.clear()
                        images.addAll(updatedImages)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        viewPager?.registerOnPageChangeCallback(callback)
        pageChangeCallback = callback

        // Keep the description overlay above the navigation bar
        val overlayContainer = dialog?.findViewById<View>(R.id.overlayContainer)
        overlayContainer?.let { overlay ->
            val basePaddingBottom = overlay.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(overlay) { v, insets ->
                val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                v.setPadding(
                    v.paddingLeft,
                    v.paddingTop,
                    v.paddingRight,
                    basePaddingBottom + navigationBars.bottom
                )
                insets
            }
        }

        // Set initial description
        images.getOrNull(startPosition)?.let { descriptionText?.text = it.description }

        dialog?.setOnDismissListener {
            cleanup()
        }

        isDialogShown = true
        dialog?.show()
    }

    fun dismiss() {
        if (dialog != null) {
            dialog?.dismiss()
        } else {
            cleanup()
        }
    }

    private fun cleanup() {
        pageChangeCallback?.let { viewPager?.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        viewPager = null
        dialog = null
        isDialogShown = false
        scope.cancel()
    }

    fun isShowing(): Boolean = isDialogShown

    private fun getAllImages(summits: List<Summit>?): MutableList<Poster> {
        return summits
            ?.flatMap { entry ->
                entry.imageIds.mapIndexed { i, imageId ->
                    Poster(
                        entry.getImageUrl(imageId), entry.getImageDescription(resources, i)
                    )
                }
            }
            ?.toMutableList()
            ?: mutableListOf()
    }

    private inner class PosterImageAdapter(
        private val posters: MutableList<Poster>
    ) : RecyclerView.Adapter<PosterImageAdapter.PosterViewHolder>() {

        inner class PosterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val zoomableImageView: ZoomableImageView = itemView.findViewById(R.id.zoomableImageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_fullscreen_image, parent, false)
            return PosterViewHolder(view)
        }

        override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
            holder.zoomableImageView.onSingleTap = { dismiss() }
            Glide.with(holder.zoomableImageView.context)
                .load(posters[position].url)
                .fitCenter()
                .override(2048)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.zoomableImageView)
        }

        override fun onViewRecycled(holder: PosterViewHolder) {
            holder.zoomableImageView.onSingleTap = null
            super.onViewRecycled(holder)
        }

        override fun getItemCount(): Int = posters.size
    }
}
