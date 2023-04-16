package de.drtobiasprinz.summitbook

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView
import com.github.dhaval2404.imagepicker.ImagePicker
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.databinding.ActivityAddImagesBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.io.File
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
class AddImagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddImagesBinding

    @Inject
    lateinit var contactsAdapter: ContactsAdapter
    private val viewModel: DatabaseViewModel by viewModels()

    private var database: AppDatabase? = null
    private var summitEntry: Summit? = null
    private var canImageBeOnFirstPosition: Map<Int, Boolean>? = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = DatabaseModule.provideDatabase(applicationContext)
        val bundle = intent.extras
        if (bundle != null) {
            val summitEntryId = bundle.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
            summitEntry = database?.summitsDao()?.getSummit(summitEntryId)
        }
        val localSummit = summitEntry
        if (localSummit != null) {
            binding.summitName.text = localSummit.name
            binding.back.setOnClickListener {
                finish()
            }
            binding.sportTypeImage.setImageResource(localSummit.sportType.imageIdBlack)
            drawLayout(localSummit, binding.images)

            val data = Intent()
            data.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, localSummit.id)
            setResult(Activity.RESULT_OK, data)

        }
    }

    private fun drawLayout(localSummit: Summit, layout: RelativeLayout) {
        var id = 0
        if (localSummit.hasImagePath()) {
            canImageBeOnFirstPosition = summitEntry?.imageIds?.map {
                val bitmap = BitmapDrawable(resources, localSummit.getImagePath(it).toString())
                it to (bitmap.bitmap.height < bitmap.bitmap.width)
            }?.toMap()
            for ((position, imageId) in localSummit.imageIds.withIndex()) {
                id = addImage(localSummit, imageId, layout, id, position)
            }
        }
        addAdditionalImage(id, layout)
    }

    private fun addImage(
        localSummit: Summit,
        imageId: Int,
        layout: RelativeLayout,
        id: Int,
        position: Int
    ): Int {
        val localSummitImage = PhotoView(this)
        val isVerticalImageOnNextPosition =
            (position == 0 && localSummit.imageIds.size > 1 && canImageBeOnFirstPosition?.get(
                localSummit.imageIds[1]
            ) == false)

        localSummitImage.visibility = View.VISIBLE
        Glide.with(this)
            .load("file://" + localSummit.getImagePath(imageId))
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(localSummitImage)

        val lp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        if (id > 0) {
            lp.addRule(RelativeLayout.BELOW, id)
        } else {
            lp.addRule(RelativeLayout.BELOW, R.id.name_and_type)
        }
        localSummitImage.id = View.generateViewId()
        layout.addView(localSummitImage, lp)

        val removeButton = ImageButton(this)
        removeButton.id = View.generateViewId()
        removeButton.setImageResource(R.drawable.baseline_delete_black_24dp)
        removeButton.setOnClickListener { v: View ->
            @Suppress("DEPRECATION")
            AsyncClearCache(Glide.get(applicationContext)).execute()
            AlertDialog.Builder(v.context)
                .setTitle(v.context.getString(R.string.delete_image, summitEntry?.name))
                .setMessage(getString(R.string.delete_image_text))
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    if (localSummit.getImagePath(imageId).toFile()?.delete() == true) {
                        localSummit.imageIds.remove(imageId)
                        updateAdapterAndDatabase(localSummit)
                        layout.removeAllViewsInLayout()
                        drawLayout(localSummit, layout)
                        Toast.makeText(
                            v.context, getString(R.string.delete_image_done),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(
                    android.R.string.cancel
                ) { _: DialogInterface?, _: Int ->
                    Toast.makeText(
                        v.context, getString(R.string.delete_cancel),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
        layout.addView(removeButton, getLayoutParams(localSummitImage.id, RelativeLayout.ALIGN_TOP))
        if (isVerticalImageOnNextPosition) {
            removeButton.visibility = View.GONE
        }

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
        val isVerticalImageOnSecondPosition =
            (position == 1 && canImageBeOnFirstPosition?.get(imageId) == false)
        val addUpButton =
            !(localSummit.imageIds.first() == imageId || isVerticalImageOnSecondPosition)
        if (addUpButton) {
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

        if (!(localSummit.imageIds.last() == imageId || isVerticalImageOnNextPosition)) {
            layout.addView(
                downButton,
                getLayoutParams(
                    if (addUpButton) upButton.id else removeButton.id,
                    RelativeLayout.BELOW
                )
            )
        }

        return localSummitImage.id
    }

    private fun addAdditionalImage(id: Int, layout: RelativeLayout) {
        val localSummitImage = PhotoView(this)
        localSummitImage.visibility = View.VISIBLE
        val lp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        if (id > 0) {
            lp.addRule(RelativeLayout.BELOW, id)
        } else {
            lp.addRule(RelativeLayout.BELOW, R.id.name_and_type)
        }
        localSummitImage.id = View.generateViewId()
        layout.addView(localSummitImage, lp)
        val addHorizontalImageButton = ImageButton(this)
        addHorizontalImageButton.id = View.generateViewId()
        addHorizontalImageButton.setImageResource(R.drawable.ic_baseline_panorama_horizontal_24)
        addImageOnClickListener(
            layout,
            addHorizontalImageButton,
            localSummitImage,
            16f,
            9f
        )
        if (id > 0) {
            val addVerticalImageButton = ImageButton(this)
            addVerticalImageButton.setImageResource(R.drawable.ic_baseline_panorama_vertical_24)
            addImageOnClickListener(
                layout,
                addVerticalImageButton,
                localSummitImage,
                9f,
                16f,
                addHorizontalImageButton.id
            )
        }
    }

    private fun addImageOnClickListener(
        layout: RelativeLayout, button: ImageButton,
        localSummitImage: PhotoView, cropX: Float, cropY: Float, idOtherButton: Int = 0
    ) {
        button.setOnClickListener {
            ImagePicker.with(this)
                .crop(cropX, cropY)
                .compress(1024)
                .galleryOnly()
                .saveDir(File(MainActivity.cache, "SummitBookImageCache"))
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }
        if (idOtherButton > 0) {
            layout.addView(
                button,
                getLayoutParams(localSummitImage.id, RelativeLayout.ALIGN_TOP, false, idOtherButton)
            )
        } else {
            layout.addView(button, getLayoutParams(localSummitImage.id, RelativeLayout.ALIGN_TOP))
        }
    }

    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data
            val localSummit = summitEntry

            if (resultCode == Activity.RESULT_OK) {
                val fileUri = data?.data
                if (fileUri != null && localSummit != null) {
                    val file = localSummit.getNextImagePath(true).toFile()
                    contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        SelectOnOsMapActivity.copyGpxFileToCache(inputStream, file)
                    }
                    updateAdapterAndDatabase(localSummit)
                    binding.images.removeAllViewsInLayout()
                    drawLayout(localSummit, binding.images)
                }
            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.add_image_canceled), Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private fun updateAdapterAndDatabase(localSummit: Summit) {
        viewModel.saveContact(true, localSummit)
    }

    private fun getLayoutParams(
        id: Int,
        alignment: Int,
        alignParentEnd: Boolean = true,
        idOtherButton: Int = 0
    ): RelativeLayout.LayoutParams {
        val layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(alignment, id)
        if (alignParentEnd) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END)
        }
        if (idOtherButton > 0) {
            layoutParams.addRule(RelativeLayout.START_OF, idOtherButton)
        }
        return layoutParams
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        summitEntry?.id?.let { outState.putLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
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
        internal class AsyncClearCache(private val glide: Glide) : AsyncTask<Uri, Int?, Void?>() {
            override fun doInBackground(vararg p0: Uri?): Void? {
                glide.clearDiskCache()
                return null
            }
        }
    }

}