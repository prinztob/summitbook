package de.drtobiasprinz.summitbook.ui.dialog

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.fragments.BookmarkViewFragment
import de.drtobiasprinz.summitbook.models.BookmarkEntry
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.ui.utils.InputFilterMinMax
import java.text.ParseException
import java.util.*

class AddBookmarkDialog : DialogFragment() {
    var isUpdate = false
    private lateinit var helper: SummitBookDatabaseHelper
    private lateinit var database: SQLiteDatabase
    private var sportTypeAdapter: ArrayAdapter<SportType>? = null
    private var currentBookmark: BookmarkEntry? = null
    private lateinit var bookmarkName: EditText
    private lateinit var commentText: EditText
    private lateinit var heightMeterText: EditText
    private lateinit var kmText: EditText
    private lateinit var saveEntryButton: Button
    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            saveEntryButton.isEnabled = !(isEmpty(bookmarkName) || isEmpty(heightMeterText) || isEmpty(kmText))
        }

        private fun isEmpty(editText: EditText): Boolean {
            return TextUtils.isEmpty(editText.text.toString().trim { it <= ' ' })
        }
    }
    private var sportTypeSpinner: Spinner? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_add_bookmark, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        helper = SummitBookDatabaseHelper(view.context)
        database = helper.writableDatabase
        //input elements
        saveEntryButton = view.findViewById(R.id.add_bookmark_save)
        saveEntryButton.isEnabled = false
        val closeDialogButton = view.findViewById<Button>(R.id.add_bookmark_cancel)
        bookmarkName = view.findViewById(R.id.bookmark_name)
        bookmarkName.addTextChangedListener(watcher)
        sportTypeSpinner = view.findViewById(R.id.activities)
        sportTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
                SportType.values())
        sportTypeSpinner?.adapter = sportTypeAdapter
        commentText = view.findViewById(R.id.comments)
        heightMeterText = view.findViewById(R.id.height_meter)
        heightMeterText.addTextChangedListener(watcher)
        heightMeterText.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 9999))
        kmText = view.findViewById(R.id.kilometers)
        kmText.addTextChangedListener(watcher)
        kmText.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 999))
        if (isUpdate) {
            saveEntryButton.setText(R.string.updateButtonText)
            updateDialogFields(currentBookmark, true)
        }

        //save the summit
        saveEntryButton.setOnClickListener {
            val sportType = SportType.valueOf(sportTypeSpinner?.selectedItem.toString())
            parseSummitEntry(sportType)
            database = helper.writableDatabase
            val adapter = BookmarkViewFragment.adapter
            val bookmark = currentBookmark
            if (bookmark != null) {
                if (isUpdate) {
                    helper.updateBookmark(database, bookmark)
                } else {
                    bookmark._id = helper.insertBookmark(database, bookmark).toInt()
                    adapter?.bookmarks?.add(bookmark)
                }
                adapter?.notifyDataSetChanged()
                Objects.requireNonNull(dialog)?.cancel()
            }
        }
        closeDialogButton.setOnClickListener { v: View ->
            Objects.requireNonNull(dialog)?.cancel()
            val text = if (currentBookmark != null) "Updating an existing summit was canceled." else "Adding a new summit was canceled."
            Toast.makeText(v.context, text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseSummitEntry(sportType: SportType) {
        try {
            val bookmark = currentBookmark
            if (bookmark != null) {
                bookmark.name = bookmarkName.text.toString()
                bookmark.sportType = sportType
                bookmark.comments = commentText.text.toString()
                bookmark.heightMeter = heightMeterText.text.toString().toInt()
                bookmark.kilometers = getTextWithDefault(kmText, 0.0)
            } else {
                currentBookmark = BookmarkEntry(
                        bookmarkName.text.toString(),
                        sportType,
                        commentText.text.toString(), heightMeterText.text.toString().toInt(),
                        getTextWithDefault(kmText, 0.0)
                )
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun updateDialogFields(entry: BookmarkEntry?, updateSpinner: Boolean) {
        if (updateSpinner) {
            val sportTypeString = SportType.valueOf(entry?.sportType.toString())
            val spinnerPosition = sportTypeAdapter?.getPosition(sportTypeString)
            spinnerPosition?.let { sportTypeSpinner?.setSelection(it) }
        }
        if (entry != null) {
            setTextIfNotAlreadySet(bookmarkName, entry.name)
            setTextIfNotAlreadySet(commentText, entry.comments)
            setTextIfNotAlreadySet(heightMeterText, entry.heightMeter.toString())
            setTextIfNotAlreadySet(kmText, entry.kilometers.toString())
        }
    }

    private fun getTextWithDefault(editText: EditText, defaultValue: Double): Double {
        return try {
            editText.text.toString().toDouble()
        } catch (e: Exception) {
            defaultValue
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        database.close()
        helper.close()
    }

    companion object {
        @JvmStatic
        fun updateInstance(entry: BookmarkEntry?): AddBookmarkDialog {
            val add = AddBookmarkDialog()
            add.isUpdate = true
            add.currentBookmark = entry
            return add
        }

        private fun setTextIfNotAlreadySet(editText: EditText, setValue: String) {
            if (editText.text.toString() == "") {
                editText.setText(setValue)
            }
        }
    }

}
