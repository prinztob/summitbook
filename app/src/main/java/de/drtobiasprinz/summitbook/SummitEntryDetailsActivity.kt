package de.drtobiasprinz.summitbook

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.tabs.TabLayout
import de.drtobiasprinz.summitbook.adapter.TabsPagerAdapter
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.MyResultReceiver
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.HackyViewPager


class SummitEntryDetailsActivity : AppCompatActivity(), MyResultReceiver {
    lateinit var summitEntry: Summit
    private lateinit var viewPager: HackyViewPager
    private var summitToCompare: Summit? = null
    var summitsToCompare: List<Summit> = emptyList()
    private var database: AppDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summit_entry_details)
        database = AppDatabase.getDatabase(applicationContext)

        setActionBar()
        val bundle = intent.extras
        if (bundle != null) {
            val summitEntryId = intent.extras?.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != null) {
                val entry = database?.summitDao()?.getSummit(summitEntryId)
                if (entry != null) {
                    summitEntry = entry
                    summitsToCompare = database?.summitDao()?.getAllSummitWithSameSportType(summitEntry.sportType)
                            ?: emptyList()
                }
            }
            val tabsPagerAdapter = TabsPagerAdapter(this, supportFragmentManager, summitEntry)
            viewPager = findViewById(R.id.view_pager)
            viewPager.adapter = tabsPagerAdapter
            val tabs = findViewById<TabLayout>(R.id.tabs)
            tabs.setupWithViewPager(viewPager)
        }
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

    override fun getViewPager(): HackyViewPager {
        return viewPager
    }


}