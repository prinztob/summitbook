package de.drtobiasprinz.summitbook

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import de.drtobiasprinz.summitbook.utils.Utils

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        Utils.fixEdgeToEdge(findViewById(R.id.layout))
        setSupportActionBar(toolbar)
        val supportActionBarLocal = supportActionBar
        if (supportActionBarLocal != null) {
            supportActionBarLocal.setDisplayHomeAsUpEnabled(true)
            supportActionBarLocal.setDisplayShowHomeEnabled(true)
        }
        val actionBar = this.supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}