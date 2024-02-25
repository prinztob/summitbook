package de.drtobiasprinz.summitbook.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.navigation.NavigationView
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.PythonActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SettingsActivity
import de.drtobiasprinz.summitbook.databinding.ActivityMainBinding
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.BarChartFragment
import de.drtobiasprinz.summitbook.fragments.LineChartDailyReportData
import de.drtobiasprinz.summitbook.fragments.LineChartFragment
import de.drtobiasprinz.summitbook.fragments.OpenStreetMapFragment
import de.drtobiasprinz.summitbook.fragments.SegmentsViewFragment
import de.drtobiasprinz.summitbook.fragments.SortAndFilterFragment
import de.drtobiasprinz.summitbook.fragments.StatisticsFragment
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.models.Poster
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.ui.dialog.ForecastDialog
import de.drtobiasprinz.summitbook.ui.dialog.ShowNewSummitsFromGarminDialog
import de.drtobiasprinz.summitbook.ui.utils.CustomLineChartWithMarker
import de.drtobiasprinz.summitbook.ui.utils.GarminDataUpdater
import de.drtobiasprinz.summitbook.ui.utils.GarminTrackAndDataDownloader
import de.drtobiasprinz.summitbook.ui.utils.MyFillFormatter
import de.drtobiasprinz.summitbook.ui.utils.MyLineLegendRenderer
import de.drtobiasprinz.summitbook.ui.utils.PosterOverlayView
import de.drtobiasprinz.summitbook.ui.utils.ZipFileReader
import de.drtobiasprinz.summitbook.ui.utils.ZipFileWriter
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    lateinit var binding: ActivityMainBinding
    private lateinit var summitViewFragment: SummitViewFragment
    private lateinit var numberFormat: NumberFormat

    private lateinit var performanceGraphProvider: PerformanceGraphProvider
    private val viewModel: DatabaseViewModel by viewModels()

    @Inject
    lateinit var sortFilterValues: SortFilterValues
    private var currentPosition: Int = 0
    private var overlayView: PosterOverlayView? = null
    private var viewer: StfalconImageViewer<Poster>? = null
    private var isDialogShown = false
    private var selectedGraphType = GraphType.ElevationGain

    private var graphIsVisible: Boolean = false
    private var useFilteredSummits: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        numberFormat = NumberFormat.getInstance(resources.configuration.locales[0])
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cache = applicationContext.cacheDir
        storage = applicationContext.filesDir
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updatePythonExecutor()
        pythonInstance = Python.getInstance()
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
        viewModel.dailyReportDataList.observe(this) { dailyReportDataStatus ->
            dailyReportDataStatus.data.let { dailyReportData ->
                binding.navView.menu.findItem(R.id.nav_daily_data).isVisible =
                    !dailyReportData.isNullOrEmpty()
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
                                    viewModel.dailyReportDataList.observeOnce(this@MainActivity) { dailyReportDataStatus ->
                                        dailyReportDataStatus.data.let { dailyReportData ->
                                            val updater = GarminDataUpdater(
                                                sharedPreferences,
                                                executor,
                                                summits,
                                                dailyReportData,
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
                    setOverviewText(
                        sortFilterValues.apply(summits, sharedPreferences)
                    )
                    viewModel.forecastList.observe(this@MainActivity) { itDataForeCasts ->
                        itDataForeCasts.data?.let { forecasts ->
                            performanceGraphProvider = PerformanceGraphProvider(summits, forecasts)
                            drawPerformanceGraph(selectedGraphType)
                            binding.overviewLayout.setOnClickListener {
                                if (!graphIsVisible) {
                                    binding.chartLayout.visibility = View.VISIBLE
                                    binding.groupProperty.addOnButtonCheckedListener { _, checkedId, isChecked ->
                                        binding.lineChartMonth.clear()
                                        binding.lineChartYear.clear()
                                        if (isChecked) {
                                            selectedGraphType = when (checkedId) {
                                                binding.buttonKilometers.id -> {
                                                    GraphType.Kilometer
                                                }

                                                binding.buttonActivity.id -> {
                                                    GraphType.Count
                                                }

                                                binding.buttonVo2max.id -> {
                                                    GraphType.Vo2Max
                                                }

                                                binding.buttonPower.id -> {
                                                    GraphType.Power
                                                }

                                                else -> {
                                                    GraphType.ElevationGain
                                                }
                                            }
                                            drawPerformanceGraph(
                                                selectedGraphType
                                            )
                                        }
                                    }
                                } else {
                                    binding.chartLayout.visibility = View.GONE
                                }
                                graphIsVisible = !graphIsVisible
                            }
                        }
                    }
                }
            }
        }
        addMissingBoundingBox()
    }

    private fun drawPerformanceGraph(
        graphType: GraphType
    ) {
        val currentYear = Calendar.getInstance()[Calendar.YEAR].toString()
        val currentMonth = Calendar.getInstance()[Calendar.MONTH] + 1
        val selectedYear =
            if (sortFilterValues.getSelectedYear() != "") sortFilterValues.getSelectedYear() else currentYear
        binding.textYear.text = selectedYear
        binding.textMonth.text = DateFormatSymbols().months[currentMonth - 1]
        drawChart(binding.lineChartYear, graphType, selectedYear)
        if (currentYear == selectedYear) {
            binding.lineChartMonth.visibility = View.VISIBLE
            binding.textMonth.visibility = View.VISIBLE
            drawChart(
                binding.lineChartMonth,
                graphType,
                selectedYear,
                if (currentMonth < 10) "0${currentMonth}" else currentMonth.toString()
            )
        } else {
            binding.lineChartMonth.visibility = View.GONE
            binding.textMonth.visibility = View.GONE
        }
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
                downloader.extractFinalSummit()
                downloader.composeFinalTrack()
            }
            downloader.updateFinalEntry(viewModel)
            binding.loading.visibility = View.GONE
            binding.loading.tooltipText = ""
            Toast.makeText(
                this@MainActivity, getString(R.string.add_new_summit_successful),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun addMissingBoundingBox() {
        val scheduler = Executors.newScheduledThreadPool(5)
        scheduler.scheduleWithFixedDelay({
            val summits = summitViewFragment.summitsAdapter.differ.currentList
            val executor = pythonExecutor
            if (summits.isNotEmpty()) {
                val entriesWithoutBoundingBox = summits.filter {
                    it.hasGpsTrack() && it.trackBoundingBox == null && it !in entriesToExcludeForBoundingBoxCalculation
                }
                if (entriesWithoutBoundingBox.isNotEmpty()) {
                    updateBoundingBox(entriesWithoutBoundingBox)
                } else {
                    Log.i(
                        "Scheduler",
                        "No more bounding boxes to update. Starting to simplify GPX tracks"
                    )
                    updateSimplifiedTracks(summits)
                }
                updateDailyReportData(executor, summits)
            }

        }, 1, 5, TimeUnit.MINUTES)
    }

    private fun updateDailyReportData(
        executor: GarminPythonExecutor?,
        summits: List<Summit>?
    ) {
        if (sharedPreferences.getBoolean(
                "startup_auto_update_switch",
                false
            ) && executor != null
        ) {
            binding.loading.visibility = View.VISIBLE
            viewModel.dailyReportDataList.observeOnce(this) { dailyReportDataStatus ->
                dailyReportDataStatus.data.let { dailyReportData ->
                    val updater = GarminDataUpdater(
                        sharedPreferences,
                        executor,
                        summits,
                        dailyReportData,
                        viewModel
                    )
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            updater.update()
                        }
                        updater.onFinish(binding.loading, this@MainActivity)
                    }
                }
            }
        }
    }

    private fun updateSimplifiedTracks(summits: List<Summit>) {
        val useSimplifiedTracks =
            sharedPreferences.getBoolean("use_simplified_tracks", true)
        if (useSimplifiedTracks) {
            val entriesWithoutSimplifiedGpxTrack = summits.filter {
                it !in entriesToExcludeForSimplifyGpxTrack &&
                        it.hasGpsTrack() &&
                        !it.hasGpsTrack(simplified = true) &&
                        it.sportType != SportType.IndoorTrainer
            }.sortedByDescending { it.date }
            if (entriesWithoutSimplifiedGpxTrack.isEmpty()) {
                Log.i(
                    "Scheduler",
                    "No more tracks to simplify."
                )
            }
            pythonInstance.let {
                if (it != null) {
                    asyncSimplifyGpsTracks(
                        entriesWithoutSimplifiedGpxTrack.take(5),
                        it
                    )
                }
            }
        } else {
            summits.filter {
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
                Log.e(
                    "useSimplifiedTracks",
                    "Deleted ${it.getDateAsString()}_${it.name} because useSimplifiedTracks was set to false."
                )
            }
        }
    }

    private fun updateBoundingBox(entriesWithoutBoundingBox: List<Summit>) {
        val entriesToCheck = entriesWithoutBoundingBox.take(50)
        entriesToCheck.forEachIndexed { index, entryToCheck ->
            lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    entryToCheck.setBoundingBoxFromTrack()
                }
                if (entryToCheck.trackBoundingBox != null) {
                    viewModel.saveSummit(true, entryToCheck)
                    Log.i(
                        "Scheduler",
                        "Updated bounding box for ${entryToCheck.getDateAsString()}_${entryToCheck.name}, " +
                                "${entriesWithoutBoundingBox.size - index} remaining."
                    )
                } else {
                    Log.i(
                        "Scheduler",
                        "Updated bounding box for ${entryToCheck.getDateAsString()}_${entryToCheck.name} failed, remove it from update list."
                    )
                    entriesToExcludeForBoundingBoxCalculation.add(entryToCheck)
                }
            }
        }
    }

    private fun asyncSimplifyGpsTracks(
        summitsWithoutSimplifiedTracks: List<Summit>,
        pythonInstance: Python
    ) {
        var numberSimplifiedGpxTracks = 0
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
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
                                "Simplified track for ${it.getDateAsString()}_${it.name}."
                            )
                        } catch (ex: RuntimeException) {
                            Log.e(
                                "AsyncSimplifyGpsTracks",
                                "Error in simplify track for ${it.getDateAsString()}_${it.name}: ${ex.message}"
                            )
                            entriesToExcludeForSimplifyGpxTrack.add(it)
                        }
                    }
                } else {
                    Log.i("AsyncSimplifyGpsTracks", "No more gpx tracks to simplify.")
                }
            }
        }
    }

    private fun drawChart(
        lineChart: CustomLineChartWithMarker,
        graphType: GraphType,
        year: String,
        month: String? = null
    ) {
        var chartEntries: List<Entry>
        var chartEntriesForecast: List<Entry>
        var minMax: Pair<List<Entry>, List<Entry>>
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                chartEntries =
                    performanceGraphProvider.getActualGraphForSummits(graphType, year, month)
                chartEntriesForecast =
                    performanceGraphProvider.getForecastGraphForSummits(graphType, year, month)
                minMax =
                    performanceGraphProvider.getActualGraphMinMaxForSummits(graphType, year, month)
            }
            lineChart.invalidate()
            lineChart.axisRight.setDrawLabels(false)
            setYAxis(lineChart.axisLeft, graphType)
            setXAxis(lineChart.xAxis, year, month)
            if (graphType.cumulative) {
                lineChart.axisLeft.axisMinimum = 0f
                lineChart.axisRight.axisMinimum = 0f
            } else {
                lineChart.axisLeft.axisMinimum = (minMax.second + chartEntries).filter { it.y > 0 }.minOf { it.y }
                lineChart.axisRight.axisMinimum = (minMax.second + chartEntries).filter { it.y > 0 }.minOf { it.y }
            }

            val params = lineChart.layoutParams
            params.height = (Resources.getSystem().displayMetrics.heightPixels * 0.22).toInt()
            lineChart.layoutParams = params

            val dataSets: MutableList<ILineDataSet?> = ArrayList()
            val dataSet = LineDataSet(
                if (graphType.filterZeroValues) chartEntries.filter { it.y > 0f } else chartEntries,
                getString(R.string.actually)
            )
            setGraphView(dataSet, false, color = Color.rgb(0, 128, 0))

            if (minMax.first.isNotEmpty() && minMax.second.isNotEmpty()) {
                lineChart.axisLeft.axisMaximum = (minMax.second + chartEntries).maxOf { it.y }
                lineChart.axisRight.axisMaximum = (minMax.second + chartEntries).maxOf { it.y }
                val dataSetMaximalValues =
                    LineDataSet(
                        if (graphType.filterZeroValues) minMax.second.filter { it.y > 0f } else minMax.second,
                        getString(R.string.max_5_yrs)
                    )
                if (graphType.cumulative) {
                    val dataSetMinimalValues =
                        LineDataSet(
                            minMax.first,
                            getString(R.string.min_5_yrs)
                        )
                    setGraphView(dataSetMinimalValues)
                    dataSets.add(dataSetMinimalValues)
                    dataSetMaximalValues.fillFormatter = MyFillFormatter(dataSetMinimalValues)
                    lineChart.renderer = MyLineLegendRenderer(
                        lineChart,
                        lineChart.animator,
                        lineChart.viewPortHandler
                    )
                }
                setGraphView(dataSetMaximalValues)
                dataSets.add(dataSetMaximalValues)
            }
            if (graphType.hasForecast) {
                val dataSetForecast =
                    LineDataSet(chartEntriesForecast, getString(R.string.forecast))
                setGraphView(dataSetForecast, false, color = Color.rgb(255, 0, 0))
                dataSets.add(dataSetForecast)
            }
            dataSets.add(dataSet)

            when (resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    lineChart.xAxis.textColor = Color.WHITE
                    lineChart.axisRight.textColor = Color.WHITE
                    lineChart.axisLeft.textColor = Color.WHITE
                    lineChart.legend?.textColor = Color.WHITE
                }

                Configuration.UI_MODE_NIGHT_NO -> {
                    lineChart.xAxis.textColor = Color.BLACK
                    lineChart.axisRight.textColor = Color.BLACK
                    lineChart.axisLeft.textColor = Color.BLACK
                    lineChart.legend?.textColor = Color.BLACK
                }

                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    lineChart.xAxis.textColor = Color.WHITE
                    lineChart.axisRight.textColor = Color.WHITE
                    lineChart.axisLeft.textColor = Color.WHITE
                    lineChart.legend?.textColor = Color.WHITE
                }
            }

            lineChart.setTouchEnabled(true)
            lineChart.data = LineData(dataSets)
        }
    }

    private fun setYAxis(yAxis: YAxis?, graphType: GraphType) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val format = "${numberFormat.format(value.toDouble())} ${graphType.unit}"
                return String.format(
                    resources.configuration.locales[0],
                    format,
                    value,
                    graphType.unit
                )
            }
        }
    }

    private fun setXAxis(xAxis: XAxis, year: String? = null, month: String? = null) {
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                val cal = Calendar.getInstance()
                cal.time =
                    PerformanceGraphProvider.parseDate(String.format("${year}-${month ?: "01"}-01 00:00:00"))
                if (month == null) {
                    cal.set(Calendar.DAY_OF_YEAR, value.toInt())
                } else {
                    cal.set(Calendar.DAY_OF_MONTH, value.toInt())
                }
                return SimpleDateFormat(
                    "dd MMM",
                    resources.configuration.locales[0]
                )
                    .format(cal.time)
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

    private fun setGraphView(set1: LineDataSet?, filled: Boolean = true, color: Int = Color.BLUE) {
        set1?.mode = LineDataSet.Mode.LINEAR
        set1?.setDrawValues(false)
        set1?.setDrawCircles(false)
        if (filled) {
            set1?.cubicIntensity = 20f
            set1?.color = color
            set1?.highLightColor = Color.rgb(244, 117, 117)
            set1?.setDrawFilled(true)
            set1?.fillColor = color
            set1?.fillAlpha = 100
        } else {
            set1?.lineWidth = 2f
            set1?.setCircleColor(Color.BLACK)
            set1?.color = color
            set1?.setDrawFilled(false)
        }
        set1?.setDrawHorizontalHighlightIndicator(true)
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

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    sortFilterValues.searchString = query
                    summitViewFragment.viewModel?.refresh()
                }
                return true
            }

        })

        return super.onCreateOptionsMenu(menu)
    }

    private fun updatePythonExecutor() {
        if (pythonExecutor == null || pythonExecutor?.username == "" || pythonExecutor?.password == "") {
            val username = sharedPreferences.getString("garmin_username", "") ?: ""
            val password = sharedPreferences.getString("garmin_password", "") ?: ""
            val garminMfaSwitch = sharedPreferences.getBoolean("garmin_mfa", false)
            val oauthPath = File(storage?.absolutePath, ".garminconnect")
            if (oauthPath.exists()) {
                pythonExecutor = GarminPythonExecutor(username, password)
            } else if (username != "" && password != "" && garminMfaSwitch) {
                val intent = Intent(this, PythonActivity::class.java)
                startActivity(intent)
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
        lateinit var sharedPreferences: SharedPreferences
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

            R.id.nav_daily_data -> {
                commitFragment(LineChartDailyReportData())
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
            sortFilterValues.updateCurrentYearSwitch(
                sharedPreferences.getBoolean("current_year_switch", false)
            )
            viewModel.refresh()
        }
        if (key == "garmin_username" || key == "garmin_password" || key == "garmin_mfa") {
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