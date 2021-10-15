package de.drtobiasprinz.summitbook.ui.utils

import android.app.Activity
import android.database.sqlite.SQLiteDatabase
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.SummitEntry
import java.io.File

class ImagePickerListener {

    fun setListener(addImageButton: ImageButton, summitEntry: SummitEntry, adapter: SummitViewAdapter, database: SQLiteDatabase, helper: SummitBookDatabaseHelper) {
        val context: AppCompatActivity? = MainActivity.mainActivity
        addImageButton.setOnClickListener { _ ->
            MainActivity.mainActivity?.let {
                ImagePicker.with(it)
                        .crop(16f, 9f)
                        .compress(1024)
                        .galleryOnly()
                        .saveDir(File(MainActivity.cache, "SummitBockImageCache"))
                        .start { resultCode, data ->
                            if (resultCode == Activity.RESULT_OK) {
                                val file: File? = ImagePicker.getFile(data)
                                if (file != null) {
                                    file.copyTo(summitEntry.getNextImagePath(true).toFile(), overwrite = true)
                                    helper.updateImageIdsSummit(database, summitEntry._id, summitEntry.imageIds)
                                    adapter.notifyDataSetChanged()
                                }
                            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                                Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Task Cancelled", Toast.LENGTH_SHORT).show()
                            }
                        }
            }
        }
    }
}