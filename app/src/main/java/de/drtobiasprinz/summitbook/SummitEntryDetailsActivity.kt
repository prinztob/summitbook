package de.drtobiasprinz.summitbook

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.chaquo.python.Python
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.adapter.TabsPagerAdapter
import de.drtobiasprinz.summitbook.db.entities.SportGroup
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.viewmodel.PageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SummitEntryDetailsActivity : AppCompatActivity() {
    var pageViewModel: PageViewModel? = null

    lateinit var summitEntry: Summit
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this)[PageViewModel::class.java]
        setContentView(R.layout.activity_summit_entry_details)
        setActionBar()
        OpenStreetMapUtils.setOsmConfForTiles()
        val bundle = intent.extras
        if (bundle != null) {
            val summitEntryId = intent.extras?.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != null) {
                pageViewModel?.getSummitToView(summitEntryId)
                pageViewModel?.summitToView?.observe(this) { itData ->
                    itData.data?.let { entry ->
                        summitEntry = entry
                        pythonInstance?.let { simplifyTrack(it, entry) }
                        val tabsPagerAdapter = TabsPagerAdapter(this, summitEntry)
                        viewPager = findViewById(R.id.pager)
                        viewPager.adapter = tabsPagerAdapter
                        viewPager.offscreenPageLimit = 4
                        val tabs = findViewById<TabLayout>(R.id.tabs)
                        TabLayoutMediator(tabs, viewPager) { tab, position ->
                            tab.text = getPageTitle(position)
                        }.attach()
                    }
                }
            }
        }
    }

    private fun simplifyTrack(
        pythonInstance: Python,
        summit: Summit
    ) {
        val useSimplifiedTracks =
            MainActivity.sharedPreferences.getBoolean(Keys.PREF_USE_SIMPLIFIED_TRACKS, true)
        if (
            !summit.ignoreSimplifyingTrack &&
            useSimplifiedTracks &&
            summit.hasGpsTrack() &&
            !summit.hasGpsTrack(simplified = true) &&
            summit.sportType != SportType.IndoorTrainer
        ) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        GpxPyExecutor(pythonInstance).createSimplifiedGpxTrack(
                            summit.getGpsTrackPath(
                                simplified = false
                            )
                        )
                        Log.i(
                            "AsyncSimplifyGpsTracks",
                            "Simplified track for ${summit.getDateAsString()}_${summit.name}."
                        )
                    } catch (ex: RuntimeException) {
                        Log.e(
                            "AsyncSimplifyGpsTracks",
                            "Error in simplify track for ${summit.getDateAsString()}_${summit.name}: ${ex.message}"
                        )
                    }
                }
            }
        }
    }

    private fun getPageTitle(position: Int): CharSequence {
        val tabTitles = mutableListOf(R.string.tab_text_0)
        if (summitEntry.garminData != null) {
            tabTitles.add(R.string.tab_text_1)
        }
        if (summitEntry.hasImagePath()) {
            tabTitles.add(R.string.tab_text_2)
        }
        if (summitEntry.hasGpsTrack()) {
            tabTitles.add(R.string.tab_text_3)
        }
        if (summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true) {
            tabTitles.add(R.string.tab_text_4)
        }
        return resources.getString(tabTitles[position])
    }

    private fun setActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            supportActionBar?.hide()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, summitEntry.id)
    }

    companion object {

        fun getSummitsToCompare(
            itData: DataStatus<List<Summit>>,
            summitEntry: Summit,
            onlyWithGpxTrack: Boolean = false,
            onlyWithPowerData: Boolean = false,
        ): List<Summit> {
            itData.data.let { summits ->
                val sportGroup =
                    SportGroup.entries
                        .filter { summitEntry.sportType in it.sportTypes }
                return if (sportGroup.size == 1) {
                    summits?.filter {
                        it.id != summitEntry.id && it.hasGpsTrack() &&
                                it.sportType in sportGroup.first().sportTypes
                    } ?: emptyList()
                } else {
                    summits?.filter {
                        it.id != summitEntry.id &&
                                (if (onlyWithGpxTrack) it.hasGpsTrack() else true) &&
                                (if (onlyWithPowerData) it.garminData?.power != null else true) &&
                                it.sportType == summitEntry.sportType
                    } ?: emptyList()
                }
            }
        }

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

}