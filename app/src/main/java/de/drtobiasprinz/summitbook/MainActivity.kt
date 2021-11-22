package de.drtobiasprinz.summitbook

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.navigation.NavigationView
import com.stfalcon.imageviewer.StfalconImageViewer
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.fragments.*
import de.drtobiasprinz.summitbook.models.*
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.ui.dialog.ShowNewSummitsFromGarminDialog
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import de.drtobiasprinz.summitbook.ui.utils.ZipFileReader
import java.io.*
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.round


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var imagePathsOfAllImages: List<String>? = listOf()
    private lateinit var database: AppDatabase
    lateinit var summitViewFragment: SummitViewFragment
    private var searchView: SearchView? = null
    private lateinit var sortFilterHelper: SortFilterHelper
    private var pythonExecutor: GarminPythonExecutor? = null
    private var entriesToExcludeForBoundingboxCalculation: MutableList<Summit> = mutableListOf()

    private var isDialogShown = false
    private var currentPosition: Int = 0
    private var viewer: StfalconImageViewer<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(applicationContext)
        val entries = database.summitDao()?.allSummit
        val viewedFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.content_frame)
        verifyStoragePermissions(this)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val username = sharedPreferences.getString("garmin_username", null) ?: ""
        val password = sharedPreferences.getString("garmin_password", null) ?: ""
        if (username != "" && password != "") {
            pythonExecutor = GarminPythonExecutor(username, password)
        }
        mainActivity = this
        setContentView(R.layout.activity_main)
        cache = applicationContext.cacheDir
        storage = applicationContext.filesDir
        File(storage, Summit.subDirForGpsTracks).mkdirs()
        File(storage, Bookmark.subDirForGpsTracks).mkdirs()
        File(storage, Summit.subDirForImages).mkdirs()

        sortFilterHelper = SortFilterHelper.getInstance(this, entries as ArrayList<Summit>, database, savedInstanceState, sharedPreferences)
        if (viewedFragment != null && viewedFragment is SummationFragment) {
            sortFilterHelper.fragment = viewedFragment
        } else {
            summitViewFragment = SummitViewFragment(sortFilterHelper, pythonExecutor)
            sortFilterHelper.fragment = summitViewFragment
        }

        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.nav_open_drawer,
                R.string.nav_close_drawer)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleAtFixedRate({
            val entriesWithoutBoundingBox = sortFilterHelper.entries.filter {
                it.hasGpsTrack() && it.trackBoundingBox == null && it !in entriesToExcludeForBoundingboxCalculation
            }
            if (entriesWithoutBoundingBox.isNotEmpty()) {
                val entryToCheck = entriesWithoutBoundingBox.first()
                entryToCheck.setBoundingBoxFromTrack()
                if (entryToCheck.trackBoundingBox != null) {
                    database.summitDao()?.updateSummit(entryToCheck)
                    Log.i("ScheduleAtFixedRate", "Updated bounding box for ${entryToCheck.name}, ${entriesWithoutBoundingBox.size} remaining.")
                } else {
                    Log.i("ScheduleAtFixedRate", "Updated bounding box for ${entryToCheck.name} failed, remove it from update list.")
                    entriesToExcludeForBoundingboxCalculation.add(entryToCheck)
                }
            } else {
                Log.i("ScheduleAtFixedRate", "Nothing to do.")
            }

        }, 0, 10, TimeUnit.MINUTES)

        if (viewedFragment == null) {
            extremaValuesAllSummits = ExtremaValuesSummits(sortFilterHelper.entries)
            val ft = supportFragmentManager.beginTransaction()
            ft.add(R.id.content_frame, summitViewFragment)
            ft.commit()
        } else {
            sortFilterHelper.entries.let {
                sortFilterHelper.update(it)
                sortFilterHelper.prepare()
                sortFilterHelper.apply()
                SummitViewFragment.adapter.notifyDataSetChanged()
                sortFilterHelper.allEntriesRequested = true
            }
        }
        SummitViewFragment.updateNewSummits(SummitViewFragment.activitiesDir, sortFilterHelper.entries, mainActivity)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = menu?.findItem(R.id.action_search)?.actionView as SearchView
        searchView?.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView?.maxWidth = Int.MAX_VALUE

        // listening to search query text change
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // filter recycler view when query submitted
                summitViewFragment.getAdapter().filter.filter(query)
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                // filter recycler view when text is changed
                summitViewFragment.getAdapter().filter.filter(query)
                return false
            }
        })
        return true
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val selectedNavigationMenuItemId = item.itemId
        startSelectedNavigationMenuItem(selectedNavigationMenuItemId)
        return true
    }

    private fun startSelectedNavigationMenuItem(selectedNavigationMenuItemId: Int) {
        when (selectedNavigationMenuItemId) {
            R.id.nav_bookmarks -> {
                commitFragment(BookmarkViewFragment())
            }
            R.id.nav_statistics -> {
                commitFragment(StatisticsFragment(sortFilterHelper))
            }
            R.id.nav_diagrams -> {
                commitFragment(LineChartFragment(sortFilterHelper))
            }
            R.id.nav_barChart -> {
                commitFragment(BarChartFragment(sortFilterHelper))
            }
            R.id.nav_osmap -> {
                commitFragment(OpenStreetMapFragment(sortFilterHelper))
            }
            R.id.nav_diashow -> {
                openViewer()
            }
            R.id.import_csv -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                }
                startActivityForResult(intent, PICK_ZIP_FILE)
            }
            R.id.export_csv -> {

                AlertDialog.Builder(this)
                        .setTitle(getString(R.string.export_csv_dialog))
                        .setMessage(getString(R.string.export_csv_dialog_text))
                        .setPositiveButton(R.string.export_csv_dialog_positiv) { _: DialogInterface?, _: Int ->
                            startFileSelectorAndExportSummits(String.format("%s_summitbook_backup_ALL.zip", LocalDate.now()), CREATE_ZIP_FILE_ALL_SUMMITS)
                        }
                        .setNeutralButton(R.string.export_csv_dialog_neutral
                        ) { _: DialogInterface?, _: Int ->
                            startFileSelectorAndExportSummits(String.format("%s_summitbook_backup_FILTERED.zip", LocalDate.now()), CREATE_ZIP_FILE_FILTERED_SUMMITS)
                        }
                        .setNegativeButton(android.R.string.cancel
                        ) { _: DialogInterface?, _: Int ->
                            Toast.makeText(this, getString(R.string.export_csv_dialog_negative_text),
                                    Toast.LENGTH_SHORT).show()
                        }
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            else -> {
                commitFragment(SummitViewFragment(sortFilterHelper, pythonExecutor))
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
    }

    private fun openViewer() {
        setAllImagePaths()
        var usePositionAfterTransition = -1
        if (imagePathsOfAllImages?.size ?: 0 < currentPosition) {
            usePositionAfterTransition = currentPosition
            currentPosition = 0
        }
        viewer = StfalconImageViewer.Builder(this, imagePathsOfAllImages) { view, imageUrl ->
            Glide.with(this)
                    .load(imageUrl)
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(view)
        }
                .withStartPosition(currentPosition)
                .withImageChangeListener {
                    currentPosition = it
                    val sizeBefore = imagePathsOfAllImages?.size
                    setAllImagePaths()
                    val sizeAfter = imagePathsOfAllImages?.size
                    if (sizeAfter != sizeBefore) {
                        viewer?.updateImages(imagePathsOfAllImages)
                        if (usePositionAfterTransition >= 0) {
                            viewer?.setCurrentPosition(usePositionAfterTransition)
                        }
                    }
                }
                .withDismissListener { isDialogShown = false }
                .show(!isDialogShown)
        isDialogShown = true
    }

    private fun setAllImagePaths() {
        imagePathsOfAllImages = sortFilterHelper.filteredEntries.map { entry -> entry.imageIds.map { entry.getImageUrl(it) } }.flatten()
    }

    private fun startFileSelectorAndExportSummits(filename: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_ZIP_FILE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                val progressBarZip = findViewById<ProgressBar>(R.id.progressBarZip)
                progressBarZip.visibility = View.VISIBLE
                AsyncImportZipFile(this).execute(uri)
            }
        }
        if (requestCode == CREATE_ZIP_FILE_ALL_SUMMITS && resultCode == Activity.RESULT_OK) {
            val progressBarZip = findViewById<ProgressBar>(R.id.progressBarZip)
            progressBarZip.visibility = View.VISIBLE
            AsyncExportZipFile(this, progressBarZip, sortFilterHelper.entries, resultData).execute()
        }
        if (requestCode == CREATE_ZIP_FILE_FILTERED_SUMMITS && resultCode == Activity.RESULT_OK) {
            val progressBarZip = findViewById<ProgressBar>(R.id.progressBarZip)
            progressBarZip.visibility = View.VISIBLE
            AsyncExportZipFile(this, progressBarZip, sortFilterHelper.filteredEntries, resultData)
        }
        if (requestCode == SelectOnOsMapActivity.PICK_GPX_FILE && resultCode == Activity.RESULT_OK) {
            BookmarkViewFragment.adapter?.onActivityResult(requestCode, resultCode, resultData)
        }
    }


    private fun commitFragment(fragment: Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.content_frame, fragment)
        ft.commit()
    }

    override fun onBackPressed() {
        if (searchView?.isIconified == false) {
            searchView?.isIconified = true
            return
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun onAddSummit(view: View?) {
        val addSummit = AddSummitDialog(sortFilterHelper, pythonExecutor)
        addSummit.show(this.supportFragmentManager, "Summits")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_search) {
            return true
        }
        if (id == R.id.action_show_new_summits) {
            ShowNewSummitsFromGarminDialog(SummitViewFragment.allEntriesFromGarmin, sortFilterHelper, pythonExecutor, findViewById(R.id.progressBarDownload)).show(supportFragmentManager, "Show new summits from Garmin")
        }
        if (id == R.id.action_sort) {
            if (sortFilterHelper.entries.size > 0) {
                sortFilterHelper.showDialog()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_DIALOG_SHOWN, isDialogShown)
        outState.putInt(KEY_CURRENT_POSITION, currentPosition)
        sortFilterHelper.onSaveInstanceState(outState)
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isDialogShown = savedInstanceState.getBoolean(KEY_IS_DIALOG_SHOWN)
        currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION)

        if (isDialogShown) {
            openViewer()
        }
    }

    override fun onPause() {
        super.onPause()
        viewer?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }

    companion object {
        // Storage Permissions
        private const val REQUEST_EXTERNAL_STORAGE = 1

        const val CREATE_ZIP_FILE_ALL_SUMMITS = 101
        const val CREATE_ZIP_FILE_FILTERED_SUMMITS = 201
        const val PICK_ZIP_FILE = 102
        private const val KEY_IS_DIALOG_SHOWN = "IS_DIALOG_SHOWN"
        private const val KEY_CURRENT_POSITION = "CURRENT_POSITION"
        var extremaValuesAllSummits: ExtremaValuesSummits? = null

        @kotlin.jvm.JvmField
        var CSV_FILE_NAME: String = "de-prinz-summitbook-export.csv"

        @kotlin.jvm.JvmField
        var storage: File? = null

        @kotlin.jvm.JvmField
        var cache: File? = null

        @SuppressLint("StaticFieldLeak")
        @kotlin.jvm.JvmField
        var mainActivity: AppCompatActivity? = null
        private val PERMISSIONS_STORAGE: Array<String> = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION)

        fun verifyStoragePermissions(activity: Activity) {
            // Check if we have write permission
            for (entry in PERMISSIONS_STORAGE) {
                val permission = ActivityCompat.checkSelfPermission(activity, entry)
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // We don't have permission so prompt the user
                    ActivityCompat.requestPermissions(
                            activity,
                            PERMISSIONS_STORAGE,
                            REQUEST_EXTERNAL_STORAGE
                    )
                }
            }
        }

        @SuppressLint("StaticFieldLeak")
        class AsyncImportZipFile(private val mainActivity: MainActivity) : AsyncTask<Uri, Int?, Void?>() {

            private val reader = ZipFileReader(mainActivity.cacheDir, mainActivity.database)
            private val database = mainActivity.database
            override fun doInBackground(vararg uri: Uri): Void? {
                mainActivity.contentResolver.openInputStream(uri[0])?.use { inputStream ->
                    reader.extractZip(inputStream)
                }
                publishProgress(10)
                reader.readFromCache()
                publishProgress(20)
                reader.newSummits.forEachIndexed { index, it ->
                    it.id = database.summitDao()?.addSummit(it) ?: 0L
                    reader.readGpxFile(it)
                    reader.readImageFile(it)
                    publishProgress(round(index.toDouble() / reader.newSummits.size * 80.0).toInt() + 20)
                }
                return null
            }

            override fun onProgressUpdate(vararg values: Int?) {
                super.onProgressUpdate(*values)
                val percent = values[0]
                if (percent != null) {
                    mainActivity.findViewById<ProgressBar>(R.id.progressBarZip).progress = percent
                }
            }

            override fun onPostExecute(param: Void?) {
                mainActivity.findViewById<ProgressBar>(R.id.progressBarZip).visibility = View.GONE
                reader.newSummits.forEach {
                    database.summitDao()?.updateSummit(it)
                }
                val entries = database.summitDao()?.allSummit as ArrayList<Summit>
                mainActivity.sortFilterHelper.update(entries)
                mainActivity.sortFilterHelper.prepare()
                mainActivity.sortFilterHelper.apply()
                SummitViewFragment.adapter.notifyDataSetChanged()
                AlertDialog.Builder(mainActivity)
                        .setTitle(mainActivity.getString(R.string.import_string_titel))
                        .setMessage(mainActivity.getString(R.string.import_string,
                                (reader.successful + reader.unsuccessful + reader.duplicate).toString(), reader.successful.toString(), reader.unsuccessful.toString(), reader.duplicate.toString()))
                        .setPositiveButton(R.string.accept) { _: DialogInterface?, _: Int -> }
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show()
            }
        }

        @SuppressLint("StaticFieldLeak")
        class AsyncExportZipFile(val context: Context, val progressBar: ProgressBar, val entries: List<Summit>, val resultData: Intent?) : AsyncTask<Uri, Int?, Void?>() {
            var entryNumber = 0
            var withImages = 0
            var withGpsFile = 0

            override fun doInBackground(vararg uri: Uri): Void? {
                writeToZipFile(entries, resultData)
                return null
            }

            override fun onProgressUpdate(vararg values: Int?) {
                super.onProgressUpdate(*values)
                val percent = values[0]
                if (percent != null) {
                    progressBar.progress = percent
                }
            }

            override fun onPostExecute(param: Void?) {
                progressBar.visibility = View.GONE
                AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.export_csv_summary_titel))
                        .setMessage(context.getString(R.string.export_csv_summary_text,
                                entries.size.toString(), withGpsFile.toString(), withImages.toString()))
                        .setPositiveButton(R.string.accept) { _: DialogInterface?, _: Int -> }
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show()
            }

            private fun writeToZipFile(entries: List<Summit>, resultData: Intent?) {
                val sb = StringBuilder()

                sb.append(Summit.getCsvHeadline())
                for (entry in entries) {
                    sb.append(entry.toString())
                }
                val dir = cache
                if (dir != null) {
                    val csvFile = writeToFile(dir, sb.toString())
                    resultData?.data?.also { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            ZipOutputStream(BufferedOutputStream(outputStream)).use { out ->
                                addFileToZip(csvFile, csvFile.name, out)
                                for (summit in entries) {
                                    Log.i("MainActivity.writeToZipFile", "Write summit $summit to zipFile")
                                    entryNumber += 1
                                    if (summit.hasGpsTrack()) {
                                        val file = summit.getGpsTrackPath()?.toFile()
                                        addFileToZip(file, summit.getExportTrackPath(), out)
                                        withGpsFile += 1
                                    }
                                    if (summit.hasImagePath()) {
                                        for ((i, imageId) in summit.imageIds.withIndex()) {
                                            val file = summit.getImagePath(imageId).toFile()
                                            addFileToZip(file, summit.getExportImagePath(1001 + i), out)
                                            withImages += 1
                                        }
                                    }
                                    publishProgress(round(entryNumber.toDouble() / entries.size * 100.0).toInt())
                                }
                            }
                        }
                    }
                }
            }

            private fun writeToFile(downloadDirectory: File, data: String): File {
                val file = File(downloadDirectory, CSV_FILE_NAME)
                try {
                    FileOutputStream(file).use { stream -> stream.write(data.toByteArray()) }
                } catch (e: IOException) {
                    Log.e("Exception", "File write failed: $e")
                }
                return file
            }

            private fun addFileToZip(file: File?, fileName: String, out: ZipOutputStream) {
                if (file != null) {
                    FileInputStream(file).use { fi ->
                        BufferedInputStream(fi).use { origin ->
                            val entry = ZipEntry(fileName)
                            out.putNextEntry(entry)
                            origin.copyTo(out, 1024)
                        }
                    }
                }
            }


        }

    }

}