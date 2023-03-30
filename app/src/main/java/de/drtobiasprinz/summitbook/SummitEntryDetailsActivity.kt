package de.drtobiasprinz.summitbook

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import de.drtobiasprinz.summitbook.adapter.TabsPagerAdapter
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.SportGroup
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.SummitEntryResultReceiver
import de.drtobiasprinz.summitbook.di.DatabaseModule


class SummitEntryDetailsActivity : AppCompatActivity(), SummitEntryResultReceiver {
    lateinit var summitEntry: Summit
    private lateinit var viewPager: ViewPager2
    private var summitToCompare: Summit? = null
    private var summitsToCompare: List<Summit> = emptyList()
    private var database: AppDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summit_entry_details)
        database = DatabaseModule.provideDatabase(applicationContext)

        setActionBar()
        val bundle = intent.extras
        if (bundle != null) {
            val summitEntryId = intent.extras?.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != null) {
                val entry = database?.summitsDao()?.getSummit(summitEntryId)
                if (entry != null) {
                    summitEntry = entry
                    val sportGroup = SportGroup.values().filter { summitEntry.sportType in it.sportTypes }
                    summitsToCompare = if (sportGroup.size == 1) {
                        sportGroup.first().sportTypes.flatMap {
                            database?.summitsDao()?.getAllSummitWithSameSportType(it)
                                ?: emptyList()
                        }
                    } else {
                        database?.summitsDao()?.getAllSummitWithSameSportType(summitEntry.sportType)
                                ?: emptyList()
                    }
                }
            }
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

    private fun getPageTitle(position: Int): CharSequence {
        val tabTitles = mutableListOf(R.string.tab_text_1)
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
        val supportActionBarLocal = supportActionBar
        if (supportActionBarLocal != null) {
            supportActionBarLocal.setDisplayHomeAsUpEnabled(true)
            supportActionBarLocal.setDisplayShowHomeEnabled(true)
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                supportActionBarLocal.hide()
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

    override fun onDestroy() {
        super.onDestroy()
        database?.close()
    }

    override fun getSummit(): Summit {
        return summitEntry
    }

    override fun getSelectedSummitForComparison(): Summit? {
        return summitToCompare
    }

    override fun setSelectedSummitForComparison(summit: Summit?) {
        summitToCompare = summit
    }

    override fun getSummitsForComparison(): List<Summit> {
        return summitsToCompare
    }

    override fun getViewPager(): ViewPager2 {
        return viewPager
    }


}