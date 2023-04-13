package de.drtobiasprinz.summitbook.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.navigation.NavigationView
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SettingsActivity
import de.drtobiasprinz.summitbook.databinding.ActivityMainBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.*
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.fragments.*
import de.drtobiasprinz.summitbook.ui.dialog.ForecastDialog
import de.drtobiasprinz.summitbook.ui.dialog.ShowNewSummitsFromGarminDialog
import de.drtobiasprinz.summitbook.ui.utils.*
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.io.File
import java.time.LocalDate
import java.util.*
import javax.inject.Inject
import kotlin.math.round
import kotlin.math.roundToLong


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var database: AppDatabase
    private lateinit var binding: ActivityMainBinding
    private lateinit var summitViewFragment: SummitViewFragment

    private val viewModel: DatabaseViewModel by viewModels()

    @Inject
    lateinit var sortFilterValues: SortFilterValues
    private var currentPosition: Int = 0
    private var overlayView: PosterOverlayView? = null
    private var viewer: StfalconImageViewer<Poster>? = null
    private var isDialogShown = false
    private lateinit var sharedPreferences: SharedPreferences

    private var useFilteredSummits: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = DatabaseModule.provideDatabase(this)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        sortFilterValues.setInitialValues(database, sharedPreferences)
        updatePythonExecutor()
        pythonInstance = Python.getInstance()
        cache = applicationContext.cacheDir
        storage = applicationContext.filesDir
        activitiesDir = File(storage, "activities")
        val viewedFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.content_frame)
        setSupportActionBar(binding.toolbarInclude.toolbar)
        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbarInclude.toolbar,
            R.string.nav_open_drawer,
            R.string.nav_close_drawer
        )
        binding.drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        if (DatabaseModule.provideDatabase(this).solarIntensityDao()?.getAll().isNullOrEmpty()) {
            binding.navView.menu.findItem(R.id.nav_solar).isVisible = false
        }
        binding.navView.setNavigationItemSelectedListener(this)
        summitViewFragment = SummitViewFragment()
        if (viewedFragment == null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.add(R.id.content_frame, summitViewFragment)
            ft.commit()
        }
        binding.apply {
            toolbarInclude.toolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.actionSort -> {
                        filter()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_update -> {
                        binding.loading.visibility = View.VISIBLE
                        @Suppress("DEPRECATION")
                        ((pythonExecutor)?.let {
                            AsyncUpdateGarminData(
                                sharedPreferences,
                                it,
                                database,
                                summitViewFragment.contactsAdapter.differ.currentList,
                                this@MainActivity,
                                binding.loading
                            ).execute()
                        })
                        return@setOnMenuItemClickListener true
                    }
                    (R.id.action_show_new_summits) -> {
                        ShowNewSummitsFromGarminDialog().show(
                            supportFragmentManager,
                            "Show new summits from Garmin"
                        )
                        return@setOnMenuItemClickListener true
                    }
                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }
            }
            viewModel.summitsList.observe(this@MainActivity) { itData ->
                itData.data?.let { summits ->
                    setOverviewText(summits)
                }
            }
        }
    }

    private fun setOverviewText(summits: List<Summit>) {
        val indoorHeightMeterPercent = sharedPreferences.getInt("indoor_height_meter_per_cent", 0)
        val statisticEntry = StatisticEntry(summits, indoorHeightMeterPercent)
        statisticEntry.calculate()
        val peaks = summits.filter { it.isPeak }
        binding.overview.text = getString(
            R.string.base_info_activities,
            summits.size.toString(),
            statisticEntry.totalKm.roundToLong().toInt().toString(),
            statisticEntry.totalHm.toFloat().roundToLong().toString()
        )
        binding.overviewSummits.text = getString(
            R.string.base_info_summits,
            peaks.size.toString(),
            peaks.sumOf { it.kilometers }.toInt().toString(),
            peaks.sumOf { it.elevationData.elevationGain }.toString()
        )
    }

    private fun filter() {
        val sortAndFilterFragment = SortAndFilterFragment()
        sortAndFilterFragment.show(
            supportFragmentManager, SortAndFilterFragment().tag
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        val search = menu.findItem(R.id.actionSearch)
        val searchView = search.actionView as SearchView
        searchView.queryHint = "Search..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                summitViewFragment.viewModel?.getSearchContacts(newText!!)
                return true
            }

        })
        return super.onCreateOptionsMenu(menu)
    }

    private fun updatePythonExecutor() {
        if (pythonExecutor == null || pythonExecutor?.username == "" || pythonExecutor?.password == "") {
            val username = sharedPreferences.getString("garmin_username", null) ?: ""
            val password = sharedPreferences.getString("garmin_password", null) ?: ""
            if (username != "" && password != "") {
                pythonExecutor = GarminPythonExecutor(username, password)
            }
        }
    }


    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private const val KEY_IS_DIALOG_SHOWN = "IS_DIALOG_SHOWN"
        private const val KEY_CURRENT_POSITION = "CURRENT_POSITION"
        var CSV_FILE_NAME_SUMMITS: String = "de-prinz-summitbook-export.csv"
        var CSV_FILE_NAME_SEGMENTS: String = "de-prinz-summitbook-export-segments.csv"
        var CSV_FILE_NAME_FORECASTS: String = "de-prinz-summitbook-export-forecasts.csv"

        var entriesToExcludeForBoundingBoxCalculation: MutableList<Summit> = mutableListOf()
        var extremaValuesAllSummits: ExtremaValuesSummits? = null
        var storage: File? = null
        var cache: File? = null
        var activitiesDir: File? = null
        var pythonInstance: Python? = null
        var pythonExecutor: GarminPythonExecutor? = null
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val selectedNavigationMenuItemId = item.itemId
        startSelectedNavigationMenuItem(selectedNavigationMenuItemId)
        return true
    }


    private fun startSelectedNavigationMenuItem(selectedNavigationMenuItemId: Int) {
        when (selectedNavigationMenuItemId) {
            R.id.nav_bookmarks -> {
                val fragment = SummitViewFragment()
                fragment.showBookmarksOnly = true
                commitFragment(fragment)
            }
            R.id.nav_routes -> {
                commitFragment(SegmentsViewFragment())
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
                showExportCsvDialog()
            }
            R.id.nav_solar -> {
                commitFragment(LineChartSolarFragment())
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            else -> {
                commitFragment(SummitViewFragment())
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun showExportCsvDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.export_csv_dialog))
            .setMessage(getString(R.string.export_csv_dialog_text))
            .setPositiveButton(R.string.export_csv_dialog_neutral) { _: DialogInterface?, _: Int ->
                useFilteredSummits = false
                startFileSelectorAndExportSummits(
                    String.format(
                        "%s_summitbook_backup_ALL.zip",
                        LocalDate.now()
                    ))
            }
            .setNegativeButton(
                android.R.string.cancel
            ) { _: DialogInterface?, _: Int ->
                Toast.makeText(
                    this, getString(R.string.export_csv_dialog_negative_text),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun commitFragment(fragment: Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.content_frame, fragment)
        ft.commit()
    }

    private fun startFileSelectorAndExportSummits(filename: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        resultLauncherForExportZipSummits.launch(intent)
    }

    private val resultLauncherForExportZipSummits =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                binding.loading.visibility = View.VISIBLE
                val exportThirdPartyData =
                    sharedPreferences.getBoolean("export_third_party_data", true)
                val exportCalculatedData =
                    sharedPreferences.getBoolean("export_calculated_data", true)
                val segments = DatabaseModule.provideDatabase(this).segmentsDao()?.getAllSegments()
                val forecasts = DatabaseModule.provideDatabase(this).forecastDao()?.allForecasts
                viewModel.summitsList.observe(this, object: androidx.lifecycle.Observer<DataStatus<List<Summit>>> {
                    override fun onChanged(t: DataStatus<List<Summit>>?) {
                        viewModel.summitsList.removeObserver(this)
                        if (t?.data != null) {
                            @Suppress("DEPRECATION")
                            AsyncExportZipFile(
                                this@MainActivity,
                                binding.loading,
                                t.data,
                                result.data,
                                segments,
                                forecasts,
                                exportThirdPartyData,
                                exportCalculatedData
                            ).execute()
                        }
                    }
                })
            }
        }
    private val resultLauncherForImportZip =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result?.data?.data.also { uri ->
                    binding.loading.visibility = View.VISIBLE
                    @Suppress("DEPRECATION")
                    AsyncImportZipFile(this).execute(uri)
                }
            }
        }

    @Suppress("DEPRECATION")
    @SuppressLint("StaticFieldLeak")
    class AsyncImportZipFile(private val mainActivity: MainActivity) :
        AsyncTask<Uri, Int?, Void?>() {

        private val reader = ZipFileReader(File(mainActivity.cacheDir, "ZipFileReader_${Date().time}"), mainActivity.database)
        private val database = mainActivity.database
        override fun doInBackground(vararg uri: Uri): Void? {

            mainActivity.contentResolver.openInputStream(uri[0])?.use { inputStream ->
                reader.extractZip(inputStream)
            }
            publishProgress(10)
            reader.readFromCache()
            publishProgress(20)
            reader.newSummits.forEachIndexed { index, it ->
                it.id = database.summitsDao().addSummit(it)
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
                mainActivity.binding.loading.progress = percent
            }
        }

        override fun onPostExecute(param: Void?) {
            mainActivity.binding.loading.visibility = View.GONE
            reader.newSummits.forEach {
                mainActivity.viewModel.saveContact(true, it)
            }
            reader.cleanUp()
            AlertDialog.Builder(mainActivity)
                .setTitle(mainActivity.getString(R.string.import_string_title))
                .setMessage(
                    mainActivity.getString(
                        R.string.import_string,
                        (reader.successful + reader.unsuccessful + reader.duplicate).toString(),
                        reader.successful.toString(),
                        reader.unsuccessful.toString(),
                        reader.duplicate.toString()
                    )
                )
                .setPositiveButton(R.string.accept) { _: DialogInterface?, _: Int -> }
                .setIcon(android.R.drawable.ic_dialog_info)
                .show()
        }
    }


    @Suppress("DEPRECATION")
    class AsyncSimplifyGpsTracks(
        private val summitsWithoutSimplifiedTracks: List<Summit>,
        private val pythonInstance: Python
    ) : AsyncTask<Uri, Int?, Void?>() {

        private var numberSimplifiedGpxTracks = 0
        override fun doInBackground(vararg uri: Uri): Void? {
            if (summitsWithoutSimplifiedTracks.isNotEmpty()) {
                summitsWithoutSimplifiedTracks.forEach {
                    try {
                        GpxPyExecutor(pythonInstance).createSimplifiedGpxTrack(
                            it.getGpsTrackPath(
                                simplified = false
                            )
                        )
                        numberSimplifiedGpxTracks += 1
                        Log.i(
                            "AsyncSimplifyGpsTracks",
                            "Simplify track for ${it.getDateAsString()}_${it.name}."
                        )
                    } catch (ex: RuntimeException) {
                        Log.e(
                            "AsyncSimplifyGpsTracks",
                            "Error in simplify track for ${it.getDateAsString()}_${it.name}: ${ex.message}"
                        )
                    }
                }
            } else {
                Log.i("AsyncSimplifyGpsTracks", "No more gpx tracks to simplify.")
            }
            return null
        }

        override fun onPostExecute(param: Void?) {
            Log.i("AsyncSimplifyGpsTracks", "$numberSimplifiedGpxTracks gpx tracks simplified.")
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StaticFieldLeak")
    class AsyncExportZipFile(
        val context: Context, private val progressBar: ProgressBar,
        val entries: List<Summit>, private val resultData: Intent?,
        val segments: List<Segment>?, val forecasts: List<Forecast>?,
        private val exportThirdPartyData: Boolean = true,
        private val exportCalculatedData: Boolean = true
    ) : AsyncTask<Uri, Int?, Void?>() {
        private lateinit var writer: ZipFileWriter

        override fun doInBackground(vararg uri: Uri): Void? {
            resultData?.data?.also { resultDataUri ->
                writer = ZipFileWriter(
                    entries,
                    segments,
                    forecasts,
                    context,
                    exportThirdPartyData,
                    exportCalculatedData
                )
                context.contentResolver.openOutputStream(resultDataUri)
                    ?.let { writer.writeToZipFile(it) }
            }
            return null
        }

        override fun onPostExecute(param: Void?) {
            progressBar.visibility = View.GONE
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.export_csv_summary_title))
                .setMessage(
                    context.getString(
                        R.string.export_csv_summary_text,
                        entries.size.toString(),
                        writer.withGpsFile.toString(),
                        writer.withImages.toString()
                    )
                )
                .setPositiveButton(R.string.accept) { _: DialogInterface?, _: Int -> }
                .setIcon(android.R.drawable.ic_dialog_info)
                .show()
        }
    }

    private fun getAllImages(): MutableList<Poster> {
        return summitViewFragment.contactsAdapter.differ.currentList.map { entry ->
            entry.imageIds.mapIndexed { i, imageId ->
                Poster(
                    entry.getImageUrl(imageId),
                    entry.getImageDescription(resources, i)
                )
            }
        }.flatten() as MutableList<Poster>
    }

    private fun openViewer() {
        var allImages = getAllImages()
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
                    allImages = getAllImages()
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


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_DIALOG_SHOWN, isDialogShown)
        outState.putInt(KEY_CURRENT_POSITION, currentPosition)
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isDialogShown = savedInstanceState.getBoolean(KEY_IS_DIALOG_SHOWN)
        currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION)
        if (isDialogShown) {
            openViewer()
        }
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (key == "current_year_switch") {
            sortFilterValues.setInitialValues(database, sharedPreferences)
            summitViewFragment.contactsAdapter.viewModel?.getSortedAndFilteredSummits(sortFilterValues)
        }
    }
}