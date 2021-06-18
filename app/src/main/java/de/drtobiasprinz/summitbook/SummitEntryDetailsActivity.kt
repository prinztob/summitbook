package de.drtobiasprinz.summitbook

import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.tabs.TabLayout
import de.drtobiasprinz.summitbook.adapter.TabsPagerAdapter
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.HackyViewPager


class SummitEntryDetailsActivity : AppCompatActivity() {
    private lateinit var summitEntry: SummitEntry
    private lateinit var helper: SummitBookDatabaseHelper
    private lateinit var database: SQLiteDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summit_entry_details)
        helper = SummitBookDatabaseHelper(this)
        database = helper.writableDatabase
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
        val bundle = intent.extras
        if (bundle != null) {
            val summitEntryId = intent.extras?.getInt(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != null) {
                val entry = helper.getSummitsWithId(summitEntryId, database)
                if (entry != null) {
                    summitEntry = entry
                }
            }
            val tabsPagerAdapter = TabsPagerAdapter(this, supportFragmentManager, summitEntry)
            val viewPager = findViewById<HackyViewPager>(R.id.view_pager)
            viewPager.adapter = tabsPagerAdapter
            val tabs = findViewById<TabLayout>(R.id.tabs)
            tabs.setupWithViewPager(viewPager)
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
        database.close()
        helper.close()
    }


}