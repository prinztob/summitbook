package de.drtobiasprinz.summitbook

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.navigation.NavigationView
import com.stfalcon.imageviewer.StfalconImageViewer
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.fragments.*
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.Poster
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.PosterOverlayView
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.ui.dialog.ForecastDialog
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


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, FragmentResultReceiver {
    private var allImages: MutableList<Poster> = mutableListOf()
    private lateinit var database: AppDatabase
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var summitViewFragment: SummitViewFragment
    private var searchView: SearchView? = null
    private lateinit var sortFilterHelper: SortFilterHelper
    private var pythonExecutor: GarminPythonExecutor? = null

    private var overlayView: PosterOverlayView? = null
    private var entriesToExcludeForBoundingBoxCalculation: MutableList<Summit> = mutableListOf()

    private var isDialogShown = false
    private var currentPosition: Int = 0
    private var indoorHeightMeterPercent: Int = 0
    private var viewer: StfalconImageViewer<Poster>? = null

    private var selectedSummitInAdapter: Summit? = null
    private val allEntriesFromGarmin = mutableListOf<Summit>()
    private var summitViewAdapterFromSummitViewFragment: SummitViewAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        pythonInstance = Python.getInstance()
        database = AppDatabase.getDatabase(applicationContext)
        val entries = database.summitDao()?.allSummit
        val viewedFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.content_frame)
        verifyStoragePermissions(this)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        indoorHeightMeterPercent = sharedPreferences.getInt("indoor_height_meter_per_cent", 0)
        setPythonExecutor()
        setContentView(R.layout.activity_main)
        cache = applicationContext.cacheDir
        storage = applicationContext.filesDir
        activitiesDir = File(storage, "activities")
        File(storage, Summit.subDirForGpsTracks).mkdirs()
        File(storage, Summit.subDirForGpsTracksBookmark).mkdirs()
        File(storage, Summit.subDirForImages).mkdirs()

        sortFilterHelper = SortFilterHelper.getInstance(this, entries as ArrayList<Summit>, database, savedInstanceState, sharedPreferences)
        if (viewedFragment != null && viewedFragment is SummationFragment) {
            sortFilterHelper.fragment = viewedFragment
        } else {
            summitViewFragment = SummitViewFragment()
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

        val schedulerBoundingBox = Executors.newSingleThreadScheduledExecutor()
        schedulerBoundingBox.schedule({
            val entriesWithoutBoundingBox = sortFilterHelper.entries.filter {
                it.hasGpsTrack() && it.trackBoundingBox == null && it !in entriesToExcludeForBoundingBoxCalculation
            }
            if (entriesWithoutBoundingBox.isNotEmpty()) {
                val entryToCheck = entriesWithoutBoundingBox.first()
                entryToCheck.setBoundingBoxFromTrack()
                if (entryToCheck.trackBoundingBox != null) {
                    database.summitDao()?.updateSummit(entryToCheck)
                    Log.i("Scheduler", "Updated bounding box for ${entryToCheck.name}, ${entriesWithoutBoundingBox.size} remaining.")
                } else {
                    Log.i("Scheduler", "Updated bounding box for ${entryToCheck.name} failed, remove it from update list.")
                    entriesToExcludeForBoundingBoxCalculation.add(entryToCheck)
                }
            } else {
                Log.i("Scheduler", "No more bounding boxes to calculate.")
            }

        }, 10, TimeUnit.MINUTES)

        val useSimplifiedTracks = sharedPreferences.getBoolean("use_simplified_tracks", true)
        if (useSimplifiedTracks) {
            val entriesWithoutSimplifiedGpxTrack = sortFilterHelper.entries.filter {
                it.hasGpsTrack() && !it.hasGpsTrack(simplified = true)
            }.take(100)
            pythonInstance.let {
                if (it != null) {
                    @Suppress("DEPRECATION")
                    AsyncSimplifyGpsTracks(entriesWithoutSimplifiedGpxTrack, it).execute()
                }
            }
        } else {
            sortFilterHelper.entries.filter {
                it.hasGpsTrack(simplified = true)
            }.forEach {
                val trackFile = it.getGpsTrackPath(simplified = true).toFile()
                if (trackFile.exists()) {
                    trackFile.delete()
                }
                val gpxPyFile = it.getGpxPyPath().toFile()
                if (gpxPyFile.exists()) {
                    gpxPyFile.delete()
                }
                Log.e("useSimplifiedTracks", "Deleted ${it.getDateAsString()}_${it.name} because useSimplifiedTracks was set to false.")
            }
        }

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
                summitViewAdapterFromSummitViewFragment?.notifyDataSetChanged()
                sortFilterHelper.allEntriesRequested = true
            }
        }
        SummitViewFragment.updateNewSummits(getAllActivitiesFromThirdParty(), sortFilterHelper.entries, this)
    }

    private fun setPythonExecutor() {
        if (pythonExecutor == null) {
            val username = sharedPreferences.getString("garmin_username", null) ?: ""
            val password = sharedPreferences.getString("garmin_password", null) ?: ""
            if (username != "" && password != "") {
                pythonExecutor = GarminPythonExecutor(pythonInstance, username, password)
            }
        }
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
                summitViewFragment.getAdapter()?.filter?.filter(query)
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                // filter recycler view when text is changed
                summitViewFragment.getAdapter()?.filter?.filter(query)
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
                commitFragment(StatisticsFragment())
            }
            R.id.nav_diagrams -> {
                commitFragment(LineChartFragment())
            }
            R.id.nav_barChart -> {
                commitFragment(BarChartFragment())
            }
            R.id.nav_osmap -> {
                commitFragment(OpenStreetMapFragment())
            }
            R.id.nav_forecast -> {
                ForecastDialog().show(this.supportFragmentManager, "ForecastDialog")
            }
            R.id.nav_diashow -> {
                openViewer()
            }
            R.id.import_csv -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                }
                resultLauncherForImportZip.launch(intent)
            }
            R.id.export_csv -> {

                AlertDialog.Builder(this)
                        .setTitle(getString(R.string.export_csv_dialog))
                        .setMessage(getString(R.string.export_csv_dialog_text))
                        .setPositiveButton(R.string.export_csv_dialog_positive) { _: DialogInterface?, _: Int ->
                            startFileSelectorAndExportSummits(String.format("%s_summitbook_backup_ALL.zip", LocalDate.now()), false)
                        }
                        .setNeutralButton(R.string.export_csv_dialog_neutral
                        ) { _: DialogInterface?, _: Int ->
                            startFileSelectorAndExportSummits(String.format("%s_summitbook_backup_FILTERED.zip", LocalDate.now()), true)
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
                commitFragment(SummitViewFragment())
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
    }

    private fun openViewer() {
        setAllImages()
        var usePositionAfterTransition = -1
        if (allImages.size < currentPosition) {
            usePositionAfterTransition = currentPosition
            currentPosition = 0
        }
        if (allImages.size > 0) {
            overlayView = PosterOverlayView(this).apply {
                update(allImages[currentPosition])
            }
            viewer = StfalconImageViewer.Builder(this, allImages) { view, poster ->
                Glide.with(this)
                        .load(poster.url)
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(view)
            }
                    .withStartPosition(currentPosition)
                    .withImageChangeListener {
                        currentPosition = it
                        val sizeBefore = allImages.size
                        setAllImages()
                        val sizeAfter = allImages.size
                        if (sizeAfter != sizeBefore) {
                            viewer?.updateImages(allImages)
                            if (usePositionAfterTransition >= 0) {
                                viewer?.setCurrentPosition(usePositionAfterTransition)
                            }
                        }
                        overlayView?.update(allImages[it])
                    }
                    .withOverlayView(overlayView)
                    .withDismissListener { isDialogShown = false }
                    .show(!isDialogShown)
            isDialogShown = true
        } else {
            Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAllImages() {
        allImages = sortFilterHelper.filteredEntries.map { entry -> entry.imageIds.mapIndexed { i, imageId -> Poster(entry.getImageUrl(imageId), entry.getImageDescription(resources, i)) } }.flatten() as MutableList<Poster>
    }

    private fun startFileSelectorAndExportSummits(filename: String, useFilteredSummits: Boolean) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        if (useFilteredSummits) {
            resultLauncherForExportZipFilteredSummits.launch(intent)
        } else {
            resultLauncherForExportZipAllSummits.launch(intent)
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

    fun onAddSummit(@Suppress("UNUSED_PARAMETER") view: View?) {
        val addSummit = AddSummitDialog()
        addSummit.show(this.supportFragmentManager, "Summits")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_search) {
            return true
        }
        if (id == R.id.action_show_new_summits) {
            ShowNewSummitsFromGarminDialog().show(supportFragmentManager, "Show new summits from Garmin")
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
        selectedSummitInAdapter?.id?.let { outState.putLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
        sortFilterHelper.onSaveInstanceState(outState)
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isDialogShown = savedInstanceState.getBoolean(KEY_IS_DIALOG_SHOWN)
        currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION)
        val summitEntryId = savedInstanceState.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
        selectedSummitInAdapter = database.summitDao()?.getSummit(summitEntryId)
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

        private const val KEY_IS_DIALOG_SHOWN = "IS_DIALOG_SHOWN"
        private const val KEY_CURRENT_POSITION = "CURRENT_POSITION"
        var extremaValuesAllSummits: ExtremaValuesSummits? = null

        @kotlin.jvm.JvmField
        var CSV_FILE_NAME: String = "de-prinz-summitbook-export.csv"

        @kotlin.jvm.JvmField
        var storage: File? = null

        @kotlin.jvm.JvmField
        var cache: File? = null

        @kotlin.jvm.JvmField
        var activitiesDir: File? = null

        var pythonInstance: Python? = null

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

        @Suppress("DEPRECATION")
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
                mainActivity.summitViewAdapterFromSummitViewFragment?.notifyDataSetChanged()
                AlertDialog.Builder(mainActivity)
                        .setTitle(mainActivity.getString(R.string.import_string_title))
                        .setMessage(mainActivity.getString(R.string.import_string,
                                (reader.successful + reader.unsuccessful + reader.duplicate).toString(), reader.successful.toString(), reader.unsuccessful.toString(), reader.duplicate.toString()))
                        .setPositiveButton(R.string.accept) { _: DialogInterface?, _: Int -> }
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show()
            }
        }

        @Suppress("DEPRECATION")
        class AsyncSimplifyGpsTracks(private val summitsWithoutSimplifiedTracks: List<Summit>, private val pythonInstance: Python) : AsyncTask<Uri, Int?, Void?>() {

            private var numberSimplifiedGpxTracks = 0
            override fun doInBackground(vararg uri: Uri): Void? {
                if (summitsWithoutSimplifiedTracks.isNotEmpty()) {
                    summitsWithoutSimplifiedTracks.forEach {
                        try {
                            GpxPyExecutor(pythonInstance).createSimplifiedGpxTrack(it.getGpsTrackPath(simplified = false))
                            numberSimplifiedGpxTracks += 1
                            Log.i("AsyncSimplifyGpsTracks", "Simplify track for ${it.getDateAsString()}_${it.name}.")
                        } catch (ex: RuntimeException) {
                            Log.e("AsyncSimplifyGpsTracks", "Error in simplify track for ${it.getDateAsString()}_${it.name}: ${ex.message}")
                        }
                    }
                } else {
                    Log.i("AsyncSimplifyGpsTracks", "No more gpx tracks to simplified.")
                }
                return null
            }

            override fun onPostExecute(param: Void?) {
                Log.i("AsyncSimplifyGpsTracks", "$numberSimplifiedGpxTracks gpx tracks simplified.")
            }
        }


        @Suppress("DEPRECATION")
        @SuppressLint("StaticFieldLeak")
        class AsyncExportZipFile(val context: Context, private val progressBar: ProgressBar,
                                 val entries: List<Summit>, private val resultData: Intent?,
                                 private val exportThirdPartyData: Boolean = true,
                                 private val exportCalculatedData: Boolean = true) : AsyncTask<Uri, Int?, Void?>() {
            private var entryNumber = 0
            private var withImages = 0
            private var withGpsFile = 0

            override fun doInBackground(vararg uri: Uri): Void? {
                writeToZipFile(entries, resultData, context.resources, exportThirdPartyData, exportCalculatedData)
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
                        .setTitle(context.getString(R.string.export_csv_summary_title))
                        .setMessage(context.getString(R.string.export_csv_summary_text,
                                entries.size.toString(), withGpsFile.toString(), withImages.toString()))
                        .setPositiveButton(R.string.accept) { _: DialogInterface?, _: Int -> }
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show()
            }

            private fun writeToZipFile(entries: List<Summit>, resultData: Intent?,
                                       resources: Resources, exportThirdPartyData: Boolean,
                                       exportCalculatedData: Boolean) {
                val sb = StringBuilder()

                sb.append(Summit.getCsvHeadline(resources, exportThirdPartyData))
                sb.append(Summit.getCsvDescription(resources, exportThirdPartyData))
                for (entry in entries) {
                    sb.append(entry.getStringRepresentation(exportThirdPartyData, exportCalculatedData))
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
                                        val file = summit.getGpsTrackPath().toFile()
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

    private fun updateImageIds(localSummit: Summit, summitEntries: List<Summit>?) {
        summitEntries?.forEach {
            if (it.id == localSummit.id) {
                it.imageIds = localSummit.imageIds
            }
        }
    }

    override fun getSortFilterHelper(): SortFilterHelper {
        return sortFilterHelper
    }


    override fun getSharedPreference(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun getPythonExecutor(): GarminPythonExecutor? {
        setPythonExecutor()
        return pythonExecutor
    }

    override fun getAllActivitiesFromThirdParty(): MutableList<Summit> {
        allEntriesFromGarmin.clear()
        allEntriesFromGarmin.addAll(GarminPythonExecutor.getAllDownloadedSummitsFromGarmin(activitiesDir))
        return allEntriesFromGarmin
    }

    override fun getProgressBar(): ProgressBar = findViewById(R.id.progressBarDownload)
    override fun getSummitViewAdapter(): SummitViewAdapter? {
        return summitViewAdapterFromSummitViewFragment
    }

    override fun setSummitViewAdapter(summitViewAdapter: SummitViewAdapter?) {
        summitViewAdapterFromSummitViewFragment = summitViewAdapter
    }

    override fun getResultLauncher(): ActivityResultLauncher<Intent> {
        return resultLauncherForSummitViewAdapterFromSummitViewFragment
    }

    private val resultLauncherForSummitViewAdapterFromSummitViewFragment = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val summitEntryId = result.data?.getLongExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, 0L)
            if (summitEntryId != null) {
                val entry = database.summitDao()?.getSummit(summitEntryId)
                if (entry != null) {
                    updateImageIds(entry, summitViewAdapterFromSummitViewFragment?.summitEntries)
                    summitViewAdapterFromSummitViewFragment?.summitEntriesFiltered?.let { updateImageIds(entry, it) }
                    summitViewAdapterFromSummitViewFragment?.notifyDataSetChanged()
                }
            }
        }
    }

    private val resultLauncherForImportZip = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result?.data?.data.also { uri ->
                val progressBarZip = findViewById<ProgressBar>(R.id.progressBarZip)
                progressBarZip.visibility = View.VISIBLE
                @Suppress("DEPRECATION")
                AsyncImportZipFile(this).execute(uri)
            }
        }
    }

    private val resultLauncherForExportZipAllSummits = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val progressBarZip = findViewById<ProgressBar>(R.id.progressBarZip)
            progressBarZip.visibility = View.VISIBLE
            val exportThirdPartyData = sharedPreferences.getBoolean("export_third_party_data", true)
            val exportCalculatedData = sharedPreferences.getBoolean("export_calculated_data", true)

            @Suppress("DEPRECATION")
            AsyncExportZipFile(this, progressBarZip, sortFilterHelper.entries, result.data, exportThirdPartyData, exportCalculatedData).execute()
        }
    }
    private val resultLauncherForExportZipFilteredSummits = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val progressBarZip = findViewById<ProgressBar>(R.id.progressBarZip)
            progressBarZip.visibility = View.VISIBLE
            val exportThirdPartyData = sharedPreferences.getBoolean("export_third_party_data", true)
            val exportCalculatedData = sharedPreferences.getBoolean("export_calculated_data", true)

            @Suppress("DEPRECATION")
            AsyncExportZipFile(this, progressBarZip, sortFilterHelper.filteredEntries, result.data, exportThirdPartyData, exportCalculatedData).execute()
        }
    }

}