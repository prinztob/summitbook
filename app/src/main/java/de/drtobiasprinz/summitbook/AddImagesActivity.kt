package de.drtobiasprinz.summitbook

import android.app.Activity
import android.content.DialogInterface
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView
import com.github.dhaval2404.imagepicker.ImagePicker
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.models.Summit
import java.io.File

class AddImagesActivity : AppCompatActivity() {
    private var database: AppDatabase? = null
    private var summitEntry: Summit? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_images)
        database = AppDatabase.getDatabase(applicationContext)
        val bundle = intent.extras
        if (bundle != null) {
            val summitEntryId = bundle.getLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            summitEntry = database?.summitDao()?.getSummit(summitEntryId)
        }
        val localSummit = summitEntry
        if (localSummit != null) {
            val textViewName = findViewById<TextView>(R.id.summit_name)
            textViewName.text = localSummit.name
            val imageViewSportType = findViewById<ImageView>(R.id.sport_type_image)
            imageViewSportType.setImageResource(localSummit.sportType.imageId)
            if (localSummit.hasImagePath()) {
                val layout: RelativeLayout = findViewById(R.id.images)
                drawLayout(localSummit, layout)
            }
        }
    }

    private fun drawLayout(localSummit: Summit, layout: RelativeLayout) {
        var id = 0
        for ((position, imageId) in localSummit.imageIds.withIndex()) {
            id = addImage(localSummit, imageId, layout, id, position)
        }
        addAdditionalImage(id, layout, localSummit)
    }

    private fun addAdditionalImage(id: Int, layout: RelativeLayout, localSummit: Summit) {
        val localSummitImage = PhotoView(this)
        localSummitImage.visibility = View.VISIBLE
        val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        if (id > 0) {
            lp.addRule(RelativeLayout.BELOW, id)
        } else {
            lp.addRule(RelativeLayout.BELOW, R.id.name_and_type)
        }
        localSummitImage.id = View.generateViewId()
        layout.addView(localSummitImage, lp)
        val addImageButton = ImageButton(this)
        addImageButton.setImageResource(R.drawable.ic_add_black_24dp)
        addImageButton.setOnClickListener { _ ->
            ImagePicker.with(this)
                    .crop(16f, 9f)
                    .compress(1024)
                    .galleryOnly()
                    .saveDir(File(MainActivity.cache, "SummitBookImageCache"))
                    .start { resultCode, data ->
                        if (resultCode == Activity.RESULT_OK) {
                            val file: File? = ImagePicker.getFile(data)
                            if (file != null) {
                                file.copyTo(localSummit.getNextImagePath(true).toFile(), overwrite = true)
                                database?.summitDao()?.updateImageIds(localSummit.id, localSummit.imageIds)
                                SummitViewFragment.adapter.notifyDataSetChanged()
                                layout.removeAllViewsInLayout()
                                drawLayout(localSummit, layout)
                            }
                        } else if (resultCode == ImagePicker.RESULT_ERROR) {
                            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
                        }
                    }
        }
        layout.addView(addImageButton, getLayoutParams(localSummitImage.id, RelativeLayout.ALIGN_TOP))
    }

    private fun addImage(localSummit: Summit, imageId: Int, layout: RelativeLayout, id: Int, position: Int): Int {
        val localSummitImage = PhotoView(this)
        localSummitImage.visibility = View.VISIBLE
        Glide.with(this)
                .load("file://" + localSummit.getImagePath(imageId))
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(localSummitImage)

        val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        if (id > 0) {
            lp.addRule(RelativeLayout.BELOW, id)
        } else {
            lp.addRule(RelativeLayout.BELOW, R.id.name_and_type)
        }
        localSummitImage.id = View.generateViewId()
        layout.addView(localSummitImage, lp)

        val removeButton = ImageButton(this)
        removeButton.id = View.generateViewId()
        removeButton.setImageResource(R.drawable.ic_delete_black_24dp)
        removeButton.setOnClickListener { v: View ->
            AlertDialog.Builder(v.context)
                    .setTitle(getString(R.string.delete_image))
                    .setMessage(getString(R.string.delete_image_text))
                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                        if (localSummit.getImagePath(imageId).toFile()?.delete() == true) {
                            localSummit.imageIds.remove(imageId)
                            updateAdapterAndDatabase(localSummit)
                            localSummitImage.visibility = View.GONE
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
        layout.addView(removeButton, getLayoutParams(localSummitImage.id, RelativeLayout.ALIGN_TOP))

        val upButton = ImageButton(this)
        upButton.id = View.generateViewId()
        upButton.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
        upButton.setOnClickListener {
            localSummit.imageIds[position] = localSummit.imageIds[position - 1]
            localSummit.imageIds[position - 1] = imageId
            updateAdapterAndDatabase(localSummit)
            layout.removeAllViewsInLayout()
            drawLayout(localSummit, layout)
        }

        if (localSummit.imageIds.first() != imageId) {
            layout.addView(upButton, getLayoutParams(removeButton.id, RelativeLayout.BELOW))
        }
        val downButton = ImageButton(this)
        downButton.setImageResource(R.drawable.ic_baseline_arrow_downward_24)
        downButton.setOnClickListener {
            localSummit.imageIds[position] = localSummit.imageIds[position + 1]
            localSummit.imageIds[position + 1] = imageId
            updateAdapterAndDatabase(localSummit)
            layout.removeAllViewsInLayout()
            drawLayout(localSummit, layout)
        }

        if (localSummit.imageIds.last() != imageId) {
            layout.addView(downButton, getLayoutParams(if (localSummit.imageIds.first() != imageId) upButton.id else removeButton.id, RelativeLayout.BELOW))
        }

        return localSummitImage.id
    }

    private fun updateAdapterAndDatabase(localSummit: Summit) {
        updateImageIds(localSummit, SummitViewFragment.adapter.summitEntries)
        SummitViewFragment.adapter.summitEntriesFiltered?.let { updateImageIds(localSummit, it) }
        database?.summitDao()?.updateImageIds(localSummit.id, localSummit.imageIds)
        SummitViewFragment.adapter.notifyDataSetChanged()
    }

    private fun updateImageIds(localSummit: Summit, summitEntries: List<Summit>) {
        summitEntries.forEach {
            if (it.id == localSummit.id) {
                it.imageIds = localSummit.imageIds
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
        summitEntry?.id?.let { outState.putLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        database?.close()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    companion object {
        private const val TAG = "SummitAddImagesActivity"
    }


}