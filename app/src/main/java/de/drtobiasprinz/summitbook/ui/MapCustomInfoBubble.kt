package de.drtobiasprinz.summitbook.ui

import android.content.Context
import android.content.Intent
import android.widget.TextView
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.models.TrackColor
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class MapCustomInfoBubble(mapView: MapView?, var entry: Summit, var context: Context,
                          private var alwaysShowTrack: Boolean) :
        InfoWindow(R.layout.bonuspack_bubble, mapView) {

    override fun onClose() {
        updateGpxTrack(forceRemove = !alwaysShowTrack)
    }

    fun updateGpxTrack(forceShow: Boolean = false, forceRemove: Boolean = false) {
        if (entry.hasGpsTrack()) {
            if (entry.gpsTrack == null) {
                entry.setGpsTrack()
            }
            val gpsTrack = entry.gpsTrack
            if (gpsTrack?.osMapRoute != null) {
                if (forceRemove) {
                    if (gpsTrack.isShownOnMap) {
                        mMapView.overlayManager.remove(gpsTrack.osMapRoute)
                        gpsTrack.isShownOnMap = false
                    }
                } else if (forceShow) {
                    if (!gpsTrack.isShownOnMap) {
                        mMapView.overlayManager.add(gpsTrack.osMapRoute)
                        gpsTrack.isShownOnMap = true
                    }
                } else if (gpsTrack.isShownOnMap) {
                    if (!alwaysShowTrack) {
                        mMapView.overlayManager.remove(gpsTrack.osMapRoute)
                        gpsTrack.isShownOnMap = false
                    }
                } else {
                    mMapView.overlayManager.add(gpsTrack.osMapRoute)
                    gpsTrack.isShownOnMap = true
                }
            } else if (!forceRemove) {
                if (gpsTrack != null) {
                    if (gpsTrack.hasNoTrackPoints()) {
                        gpsTrack.parseTrack()
                    }
                    gpsTrack.addGpsTrack(mMapView, TrackColor.None, summit = entry)
                    gpsTrack.isShownOnMap = true
                }
            }
        }
    }

    override fun onOpen(item: Any) {
        val description = mView.findViewById<TextView>(R.id.bubble_description)
        description.text = context.getString(entry.sportType.sportNameStringId)
        description.setOnClickListener { startIntent() }
        val moreInfo = mView.findViewById<TextView>(R.id.bubble_title)
        moreInfo.text = entry.name
        moreInfo.setOnClickListener { startIntent() }
        updateGpxTrack(forceShow = true)
    }

    private fun startIntent() {
        try {
            val intent = Intent(context, SummitEntryDetailsActivity::class.java)
            intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, entry.id)
            context.startActivity(intent)
        } catch (e: NullPointerException) {
            // DO NOTHING
        }
    }

}