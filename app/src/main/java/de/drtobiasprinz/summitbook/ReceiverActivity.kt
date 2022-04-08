package de.drtobiasprinz.summitbook

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import de.drtobiasprinz.summitbook.fragments.BookmarkViewFragment
import de.drtobiasprinz.summitbook.ui.dialog.AddBookmarkDialog

class ReceiverActivity : AppCompatActivity() {
    private var isDialogShow: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

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


        val intent = intent
        val action = intent.action

        if (Intent.ACTION_VIEW == action) {
            val gpxTrackUri = intent.data
            Log.i("ReceiverActivity", "intent was: $action , received url ${gpxTrackUri.toString()}")
            val ft = supportFragmentManager.beginTransaction()
            val fragment = BookmarkViewFragment.getInstance(gpxTrackUri)
            ft.add(R.id.content_frame, fragment)
            ft.commit()
            isDialogShow = true
        } else {
            Log.i("ReceiverActivity", "intent was something else: $action")
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            if (!isDialogShow) {
                val gpxTrackUri = intent.data
                if (gpxTrackUri != null) {
                    val addSummit = AddBookmarkDialog(gpxTrackUri)
                    supportFragmentManager.let { addSummit.show(it, getString(R.string.add_new_bookmark)) }
                }
            }
            isDialogShow = false
        }
    }

}
