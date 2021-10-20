package de.drtobiasprinz.summitbook.ui.utils

import android.app.Activity
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.Summit
import java.io.File

class ImagePickerListener {

    fun setListener(addImageButton: ImageButton, summit: Summit, adapter: SummitViewAdapter, database: AppDatabase) {
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
                                    file.copyTo(summit.getNextImagePath(true).toFile(), overwrite = true)
                                    database.summitDao()?.updateImageIds(summit.id, summit.imageIds)
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