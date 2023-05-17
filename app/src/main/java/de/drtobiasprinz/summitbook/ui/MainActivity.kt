package de.drtobiasprinz.summitbook.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import androidx.lifecycle.lifecycleScope
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
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.*
import de.drtobiasprinz.summitbook.models.Poster
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.ui.dialog.ForecastDialog
import de.drtobiasprinz.summitbook.ui.dialog.ShowNewSummitsFromGarminDialog
import de.drtobiasprinz.summitbook.ui.utils.*
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    lateinit var binding: ActivityMainBinding
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
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

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
        viewModel.solarIntensitiesList.observe(this) { solarListDataStatus ->
            solarListDataStatus.data.let { solarIntensities ->
                binding.navView.menu.findItem(R.id.nav_solar).isVisible =
                    !solarIntensities.isNullOrEmpty()
            }
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
                        val executor = pythonExecutor
                        if (executor != null) {
                            binding.loading.visibility = View.VISIBLE
                            viewModel.summitsList.observeOnce(this@MainActivity) { summitsListDataStatus ->
                                summitsListDataStatus.data.let { summits ->
                                    viewModel.solarIntensitiesList.observeOnce(this@MainActivity) { solarListDataStatus ->
                                        solarListDataStatus.data.let { solarIntensities ->
                                            val updater = GarminDataUpdater(
                                                sharedPreferences,
                                                executor,
                                                summits,
                                                solarIntensities,
                                                viewModel
                                            )

                                            lifecycleScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    updater.update()
                                                }
                                                updater.onFinish(
                                                    binding.loading,
                                                    this@MainActivity
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.set_user_pwd), Toast.LENGTH_LONG
                            ).show()
                        }
                        return@setOnMenuItemClickListener true
                    }
                    (R.id.action_show_new_summits) -> {
                        val dialog = ShowNewSummitsFromGarminDialog()
                        dialog.save = { summits, isMerge ->
                            binding.loading.visibility = View.VISIBLE
                            binding.loading.tooltipText =
                                getString(R.string.tool_tip_progress_new_garmin_activities,
                                    summits.joinToString(", ") { it.name })
                            if (isMerge) {
                                executeDownload(summits)
                            } else {
                                summits.forEach {
                                    executeDownload(listOf(it))
                                }
                            }
                        }
                        dialog.show(
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
                    setOverviewText(sortFilterValues.apply(summits, sharedPreferences))
                }
            }
        }
        addMissingBoundingBox()
    }

    private fun executeDownload(summits: List<Summit>) {
        val downloader = GarminTrackAndDataDownloader(
            summits,
            pythonExecutor,
            sharedPreferences.getBoolean("download_tcx", false)
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                downloader.downloadTracks()
            }
            downloader.extractFinalSummit()
            downloader.composeFinalTrack()
            downloader.updateFinalEntry(viewModel)
            binding.loading.visibility = View.GONE
            binding.loading.tooltipText = ""
        }
    }

    private fun addMissingBoundingBox() {
        viewModel.summitsList.observeOnce(this@MainActivity) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val entriesWithoutBoundingBox = it.data?.filter { summit ->
                        summit.hasGpsTrack() && summit.trackBoundingBox == null && summit !in entriesToExcludeForBoundingBoxCalculation
                    } as MutableList<Summit>?
                    if (entriesWithoutBoundingBox != null && entriesWithoutBoundingBox.isNotEmpty()) {
                        val entriesToCheck = entriesWithoutBoundingBox.take(100)
                        entriesToCheck.forEach { entryToCheck ->
                            entriesWithoutBoundingBox.remove(entryToCheck)
                            entryToCheck.setBoundingBoxFromTrack()
                            if (entryToCheck.trackBoundingBox != null) {
                                viewModel.saveSummit(true, entryToCheck)
                                Log.i(
                                    "MainActivity Background",
                                    "Updated bounding box for ${entryToCheck.name}, ${entriesWithoutBoundingBox.size} remaining."
                                )
                            } else {
                                Log.i(
                                    "Scheduler",
                                    "Updated bounding box for ${entryToCheck.name} failed, remove it from update list."
                                )
                                entriesToExcludeForBoundingBoxCalculation.add(entryToCheck)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setOverviewText(summits: List<Summit>) {
        val numberFormat = NumberFormat.getInstance(resources.configuration.locales[0])
        numberFormat.maximumFractionDigits = 0
        val indoorHeightMeterPercent = sharedPreferences.getInt("indoor_height_meter_per_cent", 0)
        val statisticEntry = StatisticEntry(summits, indoorHeightMeterPercent)
        statisticEntry.calculate()
        val peaks = summits.filter { it.isPeak }
        binding.overview.text = getString(
            R.string.base_info_activities,
            numberFormat.format(summits.size),
            numberFormat.format(statisticEntry.totalKm),
            numberFormat.format(statisticEntry.totalHm)
        )
        binding.overviewSummits.text = getString(
            R.string.base_info_summits,
            numberFormat.format(peaks.size),
            numberFormat.format(peaks.sumOf { it.kilometers }),
            numberFormat.format(peaks.sumOf { it.elevationData.elevationGain })
        )
    }

    private fun filter() {
        val sortAndFilterFragment = SortAndFilterFragment()
        sortAndFilterFragment.apply = {
            viewModel.refresh()
        }
        sortAndFilterFragment.show(
            supportFragmentManager, SortAndFilterFragment().tag
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        val search = menu.findItem(R.id.actionSearch)
        val searchView = search.actionView as SearchView
        searchView.queryHint = getString(R.string.search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    summitViewFragment.viewModel?.getSearchSummit(newText)
                }
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
        private const val KEY_IS_DIALOG_SHOWN = "IS_DIALOG_SHOWN"
        private const val KEY_CURRENT_POSITION = "CURRENT_POSITION"
        var CSV_FILE_NAME_SUMMITS: String = "de-prinz-summitbook-export.csv"
        var CSV_FILE_NAME_SEGMENTS: String = "de-prinz-summitbook-export-segments.csv"
        var CSV_FILE_NAME_FORECASTS: String = "de-prinz-summitbook-export-forecasts.csv"

        var entriesToExcludeForBoundingBoxCalculation: MutableList<Summit> = mutableListOf()
        var entriesToExcludeForSimplifyGpxTrack: MutableList<Summit> = mutableListOf()
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
            .setPositiveButton(R.string.export_csv_dialog_positive) { _: DialogInterface?, _: Int ->
                useFilteredSummits = false
                startFileSelectorAndExportSummits(
                    String.format(
                        "%s_summit-book_backup_ALL.zip",
                        LocalDate.now()
                    )
                )
            }
            .setNeutralButton(
                R.string.export_csv_dialog_neutral
            ) { _: DialogInterface?, _: Int ->
                useFilteredSummits = true
                startFileSelectorAndExportSummits(
                    String.format(
                        "%s_summit-book_backup_FILTERED.zip",
                        LocalDate.now()
                    )
                )
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
                val exportThirdPartyData =
                    sharedPreferences.getBoolean("export_third_party_data", true)
                val exportCalculatedData =
                    sharedPreferences.getBoolean("export_calculated_data", true)
                viewModel.summitsList.observeOnce(this@MainActivity) { summitsListDataStatus ->
                    summitsListDataStatus.data.let { summits ->
                        viewModel.forecastList.observeOnce(this@MainActivity) { forecastsListDataStatus ->
                            forecastsListDataStatus.data.let { forecasts ->
                                viewModel.segmentsList.observeOnce(this@MainActivity) { segmentsListDataStatus ->
                                    segmentsListDataStatus.data.let { segments ->
                                        if (summits != null) {
                                            asyncExportZipFile(
                                                if (useFilteredSummits) sortFilterValues.apply(
                                                    summits, sharedPreferences
                                                ) else summits,
                                                result.data,
                                                segments,
                                                forecasts,
                                                exportThirdPartyData,
                                                exportCalculatedData
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    private val resultLauncherForImportZip =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result?.data?.data.also { uri ->
                    if (uri != null) {
                        asyncImportZipFile(uri)
                    }
                }
            }
        }

    private fun asyncImportZipFile(uri: Uri) {
        viewModel.summitsList.observeOnce(this@MainActivity) { summitsListDataStatus ->
            summitsListDataStatus.data.let { summits ->
                viewModel.forecastList.observeOnce(this@MainActivity) { forecastsListDataStatus ->
                    forecastsListDataStatus.data.let { forecasts ->
                        viewModel.segmentsList.observeOnce(this@MainActivity) { segmentsListDataStatus ->
                            segmentsListDataStatus.data.let { segments ->
                                lifecycleScope.launch {
                                    binding.loading.visibility = View.VISIBLE
                                    val reader = ZipFileReader(
                                        File(cacheDir, "ZipFileReader_${Date().time}"),
                                        summits as MutableList<Summit>,
                                        forecasts as MutableList<Forecast>,
                                        segments as MutableList<Segment>
                                    )
                                    withContext(Dispatchers.IO) {
                                        reader.saveSummit = { isEdit, summit ->
                                            viewModel.saveSummit(isEdit, summit)
                                        }
                                        reader.saveForecast = { forecast ->
                                            viewModel.saveForecast(false, forecast)
                                        }
                                        reader.saveSegmentDetails = { details ->
                                            viewModel.saveSegmentDetails(false, details)
                                        }
                                        reader.saveSegmentEntry = { entry ->
                                            viewModel.saveSegmentEntry(false, entry)
                                        }
                                        contentResolver.openInputStream(uri)?.use { inputStream ->
                                            reader.extractAndImport(inputStream)
                                        }
                                        reader.cleanUp()

                                    }
                                    AlertDialog.Builder(this@MainActivity)
                                        .setTitle(getString(R.string.import_string_title))
                                        .setMessage(
                                            getString(
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
                                    binding.loading.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun asyncExportZipFile(
        entries: List<Summit>, resultData: Intent?,
        segments: List<Segment>?, forecasts: List<Forecast>?,
        exportThirdPartyData: Boolean = true,
        exportCalculatedData: Boolean = true
    ) {

        lifecycleScope.launch {
            binding.loading.visibility = View.VISIBLE
            val writer = ZipFileWriter(
                entries,
                segments,
                forecasts,
                this@MainActivity,
                exportThirdPartyData,
                exportCalculatedData
            )
            withContext(Dispatchers.IO) {
                resultData?.data?.also { resultDataUri ->
                    contentResolver.openOutputStream(resultDataUri)
                        ?.let {
                            writer.writeToZipFile(it)
                            it.close()
                        }

                }
            }
            binding.loading.visibility = View.GONE
            AlertDialog.Builder(this@MainActivity)
                .setTitle(getString(R.string.export_csv_summary_title))
                .setMessage(
                    getString(
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

    private fun getAllImages(summits: List<Summit>?): MutableList<Poster> {
        return summits?.map { entry ->
            entry.imageIds.mapIndexed { i, imageId ->
                Poster(
                    entry.getImageUrl(imageId),
                    entry.getImageDescription(resources, i)
                )
            }
        }?.flatten() as MutableList<Poster>
    }

    private fun openViewer() {
        viewModel.summitsList.observeOnce(this) { dataStatusSummits ->
            dataStatusSummits.data.let { summits ->
                val sortFilterSummits =
                    summits?.let { it1 -> sortFilterValues.apply(it1, sharedPreferences) }
                var allImages = getAllImages(sortFilterSummits)
                var usePositionAfterTransition = -1
                if (allImages.size < currentPosition) {
                    usePositionAfterTransition = currentPosition
                    currentPosition = 0
                }
                if (allImages.size > 0) {
                    overlayView = PosterOverlayView(this@MainActivity).apply {
                        update(allImages[currentPosition])
                    }
                    viewer = StfalconImageViewer.Builder(
                        this@MainActivity,
                        allImages
                    ) { view, poster ->
                        Glide.with(this@MainActivity)
                            .load(poster.url)
                            .fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(view)
                    }
                        .withStartPosition(currentPosition)
                        .withImageChangeListener { position ->
                            currentPosition = position
                            val sizeBefore = allImages.size
                            allImages = getAllImages(sortFilterSummits)
                            val sizeAfter = allImages.size
                            if (sizeAfter != sizeBefore) {
                                viewer?.updateImages(allImages)
                                if (usePositionAfterTransition >= 0) {
                                    viewer?.setCurrentPosition(usePositionAfterTransition)
                                }
                            }
                            overlayView?.update(allImages[position])
                        }
                        .withOverlayView(overlayView)
                        .withDismissListener { isDialogShown = false }
                        .show(!isDialogShown)
                    isDialogShown = true
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.no_image_selected),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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
            viewModel.summitsList.observeOnce(this@MainActivity) { summitsListDataStatus ->
                summitsListDataStatus.data.let { summits ->
                    if (summits != null) {
                        sortFilterValues.setInitialValues(summits, sharedPreferences)
                    }
                }

                viewModel.refresh()
            }
        }
        if (key == "garmin_username" || key == "garmin_password") {
            updatePythonExecutor()
        }
    }
}

fun <T> LiveData<T>.observeOnce(
    lifecycleOwner: LifecycleOwner,
    observer: androidx.lifecycle.Observer<T>
) {
    observe(lifecycleOwner, object : androidx.lifecycle.Observer<T> {
        override fun onChanged(value: T) {
            observer.onChanged(value)
            removeObserver(this)
        }
    })
}