package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import de.drtobiasprinz.summitbook.models.BookmarkEntry
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.models.TrackBoundingBox
import de.drtobiasprinz.summitbook.ui.MapCustomInfoBubble
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

object OpenStreetMapUtils {

    private val MAP_TYPE_ITEMS = arrayOf<CharSequence>("OpenTopo", "MAPNIK", "USGS_TOPO", "HIKEBIKEMAP")

    @JvmStatic
    var selectedItem = 0

    @JvmStatic
    fun addTrackAndMarker(summitEntry: SummitEntry, osMap: MapView, context: Context, forceAddTrack: Boolean, isMilageButtonShown: Boolean, alwaysShowTrackOnMap: Boolean): Marker? {
        val mGeoPoints = ArrayList<GeoPoint>()
        val latLng = summitEntry.latLng
        var marker: Marker? = null
        if (latLng != null) {
            val point = GeoPoint(latLng.latitude, latLng.longitude)
            mGeoPoints.add(point)
            marker = addMarker(osMap, context, point, summitEntry, false, alwaysShowTrackOnMap)
            if (!summitEntry.hasGpsTrack()) {
                val mapController = osMap.controller
                mapController.setZoom(10.0)
                mapController.setCenter(point)
            }
        }
        drawTrack(summitEntry, forceAddTrack, osMap, isMilageButtonShown, true, mGeoPoints)
        if (marker != null) {
            osMap.overlays.add(marker)
        }
        return marker
    }

    @JvmStatic
    fun drawBoundingBox(mMapView: MapView?, trackBoundingBox: TrackBoundingBox) {
        val polyline = Polyline(mMapView)
        polyline.setOnClickListener { _, _, _ ->
            true // DO NOTHING
        }
        polyline.outlinePaint?.color = Color.BLACK
        polyline.outlinePaint?.strokeWidth = 6f
        polyline.setPoints(trackBoundingBox.getGeoPoints())
        mMapView?.overlayManager?.add(polyline)
    }

    @JvmStatic
    fun drawTrack(
            summitEntry: SummitEntry, forceAddTrack: Boolean, osMap: MapView, isMilageButtonShown: Boolean,
            calculateBondingBox: Boolean = false, mGeoPoints: ArrayList<GeoPoint> = arrayListOf(), color: Int = Color.BLUE
    ) {
        if (summitEntry.hasGpsTrack()) {
            if (summitEntry.gpsTrack == null) {
                summitEntry.setGpsTrack()
            }
            val gpsTrack: GpsTrack? = summitEntry.gpsTrack
            if (gpsTrack != null) {
                if (gpsTrack.hasNoTrackPoints()) {
                    gpsTrack.parseTrack()
                }
                if (gpsTrack.osMapRoute == null || forceAddTrack) {
                    gpsTrack.addGpsTrack(osMap, isMilageButtonShown, color)
                    gpsTrack.isShownOnMap = true
                }
                addTrackPoints(gpsTrack, mGeoPoints)
            }
            if (calculateBondingBox) {
                osMap.post { calculateBoundingBox(osMap, mGeoPoints) }
            }
        }
    }

    @JvmStatic
    fun addTrackAndMarker(bookmarkEntry: BookmarkEntry, osMap: MapView, forceAddTrack: Boolean, isMilageButtonShown: Boolean) {
        val mGeoPoints = ArrayList<GeoPoint>()
        if (bookmarkEntry.hasGpsTrack()) {
            bookmarkEntry.setGpsTrack()
            val gpsTrack: GpsTrack? = bookmarkEntry.gpsTrack
            if (gpsTrack != null) {
                if (gpsTrack.hasNoTrackPoints()) {
                    gpsTrack.parseTrack()
                }
                if (gpsTrack.osMapRoute == null || forceAddTrack) {
                    gpsTrack.addGpsTrack(osMap, isMilageButtonShown)
                    gpsTrack.isShownOnMap = true
                }
                addTrackPoints(gpsTrack, mGeoPoints)
            }
        }
        osMap.post { calculateBoundingBox(osMap, mGeoPoints) }
    }

    private fun addTrackPoints(gpsTrack: GpsTrack, mGeoPoints: ArrayList<GeoPoint>) {
        val positions = gpsTrack.getTrackPositions()
        for (entry in positions) {
            if (entry != null && entry.longitude != 0.0 && entry.latitude != 0.0) {
                mGeoPoints.add(GeoPoint(entry.latitude, entry.longitude))
            }
        }
    }

    @JvmStatic
    fun addMarker(mMapView: MapView, context: Context, startPoint: GeoPoint?, entry: SummitEntry, addToOverlay: Boolean = true, alwaysShowTrackOnMap: Boolean = false): Marker {
        val marker = Marker(mMapView)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = entry._id.toString()
        if (entry.hasGpsTrack()) {
            marker.icon = ResourcesCompat.getDrawable(context.resources, entry.sportType.markerIdWithGpx, null)
        } else {
            marker.icon = ResourcesCompat.getDrawable(context.resources, entry.sportType.markerIdWithoutGpx, null)
        }
        marker.infoWindow = MapCustomInfoBubble(mMapView, entry, context, alwaysShowTrackOnMap)
        marker.setOnMarkerClickListener { marker1, _ ->
            if (!marker1.isInfoWindowShown) {
                marker1.showInfoWindow()
            } else {
                marker1.closeInfoWindow()
            }
            false
        }
        if (addToOverlay) {
            mMapView.overlays.add(marker)
        }
        return marker
    }

    @JvmStatic
    fun calculateBoundingBox(mMapView: MapView, mGeoPoints: List<GeoPoint?>) {
        if (mGeoPoints.size > 1) {
            val boundingBox = BoundingBox.fromGeoPoints(mGeoPoints)
            mMapView.zoomToBoundingBox(boundingBox, false, 30)
        }
    }

    @JvmStatic
    fun calculateBoundingBox(mMapView: MapView, gpsTrack: GpsTrack, point: GeoPoint) {
        val mGeoPoints = ArrayList<GeoPoint>()
        mGeoPoints.add(point)
        addTrackPoints(gpsTrack, mGeoPoints)
        calculateBoundingBox(mMapView, mGeoPoints)
    }

    @JvmStatic
    fun addDefaultSettings(context: Context, mMapView: MapView, fragmentActivity: FragmentActivity) {

        val dm = context.resources.displayMetrics
        val mCompassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context),
                mMapView)
        mCompassOverlay.enableCompass()
        mMapView.overlays.add(mCompassOverlay)

        //map scale
        val mScaleBarOverlay = ScaleBarOverlay(mMapView)
        mScaleBarOverlay.setCentred(true)
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 4, 10)
        mMapView.overlays.add(mScaleBarOverlay)

        //support for map rotation
        val mRotationGestureOverlay = RotationGestureOverlay(mMapView)
        mRotationGestureOverlay.isEnabled = true
        mMapView.overlays.add(mRotationGestureOverlay)

        //needed for pinch zooms
        mMapView.setMultiTouchControls(true)

        //scales tiles to the current screen's DPI, helps with readability of labels
        mMapView.isTilesScaledToDpi = true
        val copyrightOverlay = CopyrightOverlay(fragmentActivity)
        copyrightOverlay.setTextSize(10)
        mMapView.overlays.add(copyrightOverlay)
    }

    @JvmStatic
    fun showMapTypeSelectorDialog(context: Context, mapView: MapView) {
        // Prepare the dialog by setting up a Builder.
        val fDialogTitle = "Select Map Type"
        val builder = AlertDialog.Builder(context)
        builder.setTitle(fDialogTitle)
        builder.setSingleChoiceItems(
                MAP_TYPE_ITEMS,
                selectedItem
        ) { dialog: DialogInterface, item: Int ->
            setTileSource(item, mapView)
            selectedItem = item
            dialog.dismiss()
        }

        val fMapTypeDialog = builder.create()
        fMapTypeDialog.setCanceledOnTouchOutside(true)
        fMapTypeDialog.show()
    }

    @JvmStatic
    fun setTileSource(item: Int, mapView: MapView) {
        val tileSource: ITileSource? = when (item) {
            0 -> TileSourceFactory.OpenTopo
            1 -> TileSourceFactory.MAPNIK
            2 -> TileSourceFactory.USGS_TOPO
            else -> TileSourceFactory.HIKEBIKEMAP
        }
        mapView.setTileSource(tileSource)
    }
}