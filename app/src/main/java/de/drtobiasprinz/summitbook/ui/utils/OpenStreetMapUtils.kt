package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.GpsTrack
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.TrackBoundingBox
import de.drtobiasprinz.summitbook.db.entities.TrackColor
import de.drtobiasprinz.summitbook.ui.MapCustomInfoBubble
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.File

object OpenStreetMapUtils {

    private val MAP_TYPE_ITEMS = arrayOf<CharSequence>("OpenTopo", "MAPNIK")

    @JvmStatic
    var selectedItem = 0

    @JvmStatic
    fun addTrackAndMarker(summitEntry: Summit, osMap: MapView, context: Context, forceAddTrack: Boolean, selectedCustomizeTrackItem: TrackColor, alwaysShowTrackOnMap: Boolean, rootView: View? = null): Marker? {
        val mGeoPoints = ArrayList<GeoPoint>()
        val latLng = summitEntry.latLng
        var marker: Marker? = null
        if (latLng != null) {
            val point = GeoPoint(latLng.lat, latLng.lon)
            mGeoPoints.add(point)
            marker = addMarker(osMap, context, point, summitEntry, false, alwaysShowTrackOnMap)
            if (!summitEntry.hasGpsTrack()) {
                val mapController = osMap.controller
                mapController.setZoom(10.0)
                mapController.setCenter(point)
            }
        }
        drawTrack(summitEntry, forceAddTrack, osMap, selectedCustomizeTrackItem, true, mGeoPoints, rootView = rootView)
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
            summitEntry: Summit, forceAddTrack: Boolean, osMap: MapView, selectedCustomizeTrackItem: TrackColor,
            calculateBondingBox: Boolean = false, mGeoPoints: ArrayList<GeoPoint> = arrayListOf(),
            color: Int = Color.BLUE, rootView: View? = null
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
                    gpsTrack.addGpsTrack(osMap, selectedCustomizeTrackItem, color, rootView)
                    gpsTrack.isShownOnMap = true
                }
                mGeoPoints.addAll(getTrackPointsFrom(gpsTrack))
            }
            if (calculateBondingBox) {
                osMap.post { calculateBoundingBox(osMap, mGeoPoints) }
            }
        }
    }

    fun getTrackPointsFrom(gpsTrack: GpsTrack): List<GeoPoint> {
        val mGeoPoints: MutableList<GeoPoint> = mutableListOf()
        val positions = gpsTrack.getTrackPositions()
        for (entry in positions) {
            if (entry != null && entry.lat != 0.0 && entry.lon != 0.0) {
                mGeoPoints.add(GeoPoint(entry.lat, entry.lon))
            }
        }
        return mGeoPoints
    }

    @JvmStatic
    fun setOsmConfForTiles() {
        val osmConf = Configuration.getInstance()
        val osmdroidBasePath = File(MainActivity.storage, "osmdroid")
        osmdroidBasePath.mkdirs()
        osmConf.osmdroidBasePath = osmdroidBasePath
        val tileCache = File(MainActivity.cache, "tile")
        tileCache.mkdirs()
        osmConf.osmdroidTileCache = tileCache
    }

    @JvmStatic
    fun addMarker(mMapView: MapView, context: Context, startPoint: GeoPoint?, entry: Summit, addToOverlay: Boolean = true, alwaysShowTrackOnMap: Boolean = false): Marker? {
        try {

            val marker = Marker(mMapView)
            marker.position = startPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = entry.id.toString()
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
        } catch (e: NullPointerException) {
            e.printStackTrace()
            return null
        }
    }

    @JvmStatic
    fun calculateBoundingBox(mMapView: MapView, mGeoPoints: List<GeoPoint?>) {
        if (mGeoPoints.size > 1) {
            val boundingBox = BoundingBox.fromGeoPoints(mGeoPoints)
            mMapView.zoomToBoundingBox(boundingBox, false, 30)
        }
    }

    @JvmStatic
    fun calculateBoundingBox(mMapView: MapView, gpsTrack: GpsTrack, point: GeoPoint?) {
        val mGeoPoints = ArrayList<GeoPoint>()
        if (point != null) {
            mGeoPoints.add(point)
        }
        mGeoPoints.addAll(getTrackPointsFrom(gpsTrack))
        calculateBoundingBox(mMapView, mGeoPoints)
    }

    @JvmStatic
    fun addDefaultSettings(context: Context, mMapView: MapView, fragmentActivity: FragmentActivity) {

        val dm = context.resources.displayMetrics
        //TODO: fix java.lang.IllegalStateException: register failed, the sensor listeners size has exceeded the maximum limit 128
//        val mCompassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context),
//                mMapView)
//        mCompassOverlay.enableCompass()
//        mMapView.overlays.add(mCompassOverlay)

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
        val fDialogTitle = context.getString(R.string.select_map_type)
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