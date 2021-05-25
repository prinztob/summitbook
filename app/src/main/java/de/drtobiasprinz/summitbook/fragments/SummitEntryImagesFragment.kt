import android.content.DialogInterface
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.PageViewModel


class SummitEntryImagesFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private lateinit var helper: SummitBookDatabaseHelper
    private lateinit var database: SQLiteDatabase
    private var summitEntry: SummitEntry? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.fragment_summit_entry_images, container, false)
        helper = SummitBookDatabaseHelper(requireContext())
        database = helper.writableDatabase
        if (summitEntry == null && savedInstanceState != null) {
            val summitEntryId = savedInstanceState.getInt(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != 0) {
                summitEntry = helper.getSummitsWithId(summitEntryId, database)
            }
        }
        val localSummitEntry = summitEntry
        if (localSummitEntry != null) {
            val textViewName = root.findViewById<TextView>(R.id.summit_name)
            textViewName.text = localSummitEntry.name
            val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
            imageViewSportType.setImageResource(localSummitEntry.sportType.imageId)
            if (localSummitEntry.hasImagePath()) {
                val layout: RelativeLayout = root.findViewById(R.id.images)
                drawLayout(localSummitEntry, layout)
            }
        }
        return root
    }

    private fun drawLayout(localSummitEntry: SummitEntry, layout: RelativeLayout) {
        var id = 0
        for ((position, imageId) in localSummitEntry.imageIds.withIndex()) {
            id = addImage(localSummitEntry, imageId, layout, id, position)
        }
    }

    private fun addImage(localSummitEntry: SummitEntry, imageId: Int, layout: RelativeLayout, id: Int, position: Int): Int {
        val localSummitEntryImage = PhotoView(requireContext())
        localSummitEntryImage.visibility = View.VISIBLE
        Glide.with(this)
                .load("file://" + localSummitEntry.getImagePath(imageId))
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(localSummitEntryImage)

        val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        if (id > 0) {
            lp.addRule(RelativeLayout.BELOW, id)
        } else {
            lp.addRule(RelativeLayout.BELOW, R.id.name_and_type)
        }
        localSummitEntryImage.id = View.generateViewId()
        layout.addView(localSummitEntryImage, lp)

        val removeButton = ImageButton(requireContext())
        removeButton.id = View.generateViewId()
        removeButton.setImageResource(R.drawable.ic_delete_black_24dp)
        removeButton.setOnClickListener { v: View ->
            AlertDialog.Builder(v.context)
                    .setTitle(getString(R.string.delete_image))
                    .setMessage(getString(R.string.delete_image_text))
                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                        if (localSummitEntry.getImagePath(imageId).toFile()?.delete() == true) {
                            localSummitEntry.imageIds.remove(imageId)
                            updateAdapterAndDatabase(localSummitEntry)
                            localSummitEntryImage.visibility = View.GONE
                            Toast.makeText(v.context, getString(R.string.delete_image_done),
                                    Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(android.R.string.no
                    ) { _: DialogInterface?, _: Int ->
                        Toast.makeText(v.context, getString(R.string.delete_cancel),
                                Toast.LENGTH_SHORT).show()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
        }
        layout.addView(removeButton, getLayoutParams(localSummitEntryImage.id, RelativeLayout.ALIGN_TOP))

        val upButton = ImageButton(requireContext())
        upButton.id = View.generateViewId()
        upButton.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
        upButton.setOnClickListener {
            localSummitEntry.imageIds[position] = localSummitEntry.imageIds[position - 1]
            localSummitEntry.imageIds[position - 1] = imageId
            updateAdapterAndDatabase(localSummitEntry)
            layout.removeAllViewsInLayout()
            drawLayout(localSummitEntry, layout)
        }

        if (localSummitEntry.imageIds.first() != imageId) {
            layout.addView(upButton, getLayoutParams(removeButton.id, RelativeLayout.BELOW))
        }
        val downButton = ImageButton(requireContext())
        downButton.setImageResource(R.drawable.ic_baseline_arrow_downward_24)
        downButton.setOnClickListener {
            localSummitEntry.imageIds[position] = localSummitEntry.imageIds[position + 1]
            localSummitEntry.imageIds[position + 1] = imageId
            updateAdapterAndDatabase(localSummitEntry)
            layout.removeAllViewsInLayout()
            drawLayout(localSummitEntry, layout)
        }

        if (localSummitEntry.imageIds.last() != imageId) {
            layout.addView(downButton, getLayoutParams(if (localSummitEntry.imageIds.first() != imageId) upButton.id else removeButton.id, RelativeLayout.BELOW))
        }

        return localSummitEntryImage.id
    }

    private fun updateAdapterAndDatabase(localSummitEntry: SummitEntry) {
        updateImageIds(localSummitEntry, SummitViewFragment.adapter.summitEntries)
        SummitViewFragment.adapter.summitEntriesFiltered?.let { updateImageIds(localSummitEntry, it) }
        helper.updateImageIdsSummit(database, localSummitEntry._id, localSummitEntry.imageIds)
        SummitViewFragment.adapter.notifyDataSetChanged()
    }

    private fun updateImageIds(localSummitEntry: SummitEntry, summitEntries: ArrayList<SummitEntry>) {
        summitEntries.forEach {
            if (it._id == localSummitEntry._id) {
                it.imageIds = localSummitEntry.imageIds
            }
        }
    }

    private fun getLayoutParams(id: Int, alignment: Int): RelativeLayout.LayoutParams {
        val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.addRule(alignment, id)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END)
        return layoutParams
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        summitEntry?._id?.let { outState.putInt(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
        helper.close()
    }

    companion object {
        private const val TAG = "SummitEntryImagesFragment"
        fun newInstance(summitEntry: SummitEntry): SummitEntryImagesFragment {
            val fragment = SummitEntryImagesFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }
}