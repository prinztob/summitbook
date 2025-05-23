package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.TrackBoundingBox
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.storage
import de.drtobiasprinz.summitbook.ui.MapCustomInfoBubble
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.MapHelper
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
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

    @JvmStatic
    var selectedItem = MapProvider.OPENTOPO
    private const val TAG = "OpenStreetMapUtils"
    private var osmdroidBasePathDefault: File? = null

    @JvmStatic
    fun addTrackAndMarker(
        summitEntry: Summit,
        osMap: MapView,
        context: Context,
        forceAddTrack: Boolean,
        selectedCustomizeTrackItem: TrackColor,
        alwaysShowTrackOnMap: Boolean,
        rootView: View? = null,
        calculateBondingBox: Boolean = true
    ): Marker? {
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
        drawTrack(
            summitEntry,
            forceAddTrack,
            osMap,
            selectedCustomizeTrackItem,
            calculateBondingBox,
            mGeoPoints,
            rootView = rootView
        )
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
        summitEntry: Summit,
        forceAddTrack: Boolean,
        osMap: MapView,
        selectedCustomizeTrackItem: TrackColor,
        calculateBondingBox: Boolean = false,
        mGeoPoints: ArrayList<GeoPoint> = arrayListOf(),
        color: Int = Color.BLUE,
        rootView: View? = null
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

    private fun getTrackPointsFrom(gpsTrack: GpsTrack): List<GeoPoint> {
        val mGeoPoints: MutableList<GeoPoint> = mutableListOf()
        val positions = gpsTrack.getTrackPositions()
        for (entry in positions) {
            if (entry != null && entry.latitude != 0.0 && entry.longitude != 0.0) {
                mGeoPoints.add(GeoPoint(entry.latitude, entry.longitude))
            }
        }
        return mGeoPoints
    }

    fun getOsmdroidTilesFolder(): File {
        val folders = PreferencesHelper.loadOnDeviceMapsFolder().split("%3A")
        val guessOsmdroidFolder = File(
            Environment.getExternalStorageDirectory(),
            folders.subList(1, folders.size).joinToString("/")
        )
        return if (guessOsmdroidFolder.exists()) {
            guessOsmdroidFolder
        } else {
            File(storage, "osmdroid")
        }
    }

    @JvmStatic
    fun setOsmConfForTiles(setToDefault: Boolean = false) {
        val osmConf = Configuration.getInstance()
        if (osmdroidBasePathDefault == null) {
            osmdroidBasePathDefault = osmConf.osmdroidBasePath
        }
        val osmdroidBasePath = getOsmdroidTilesFolder()
        osmdroidBasePath.mkdirs()
        osmConf.osmdroidBasePath = if (setToDefault) osmdroidBasePathDefault else osmdroidBasePath
        Log.i(TAG, "set osmdroidBasePath to ${osmConf.osmdroidBasePath}")
        val tileCache = File(MainActivity.cache, "tile")
        tileCache.mkdirs()
        osmConf.osmdroidTileCache = tileCache
    }

    @JvmStatic
    fun addMarker(
        mMapView: MapView,
        context: Context,
        startPoint: GeoPoint?,
        entry: Summit,
        addToOverlay: Boolean = true,
        alwaysShowTrackOnMap: Boolean = false
    ): Marker? {
        try {
            val marker = Marker(mMapView)
            marker.position = startPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = entry.id.toString()
            if (entry.hasGpsTrack()) {
                marker.icon = ResourcesCompat.getDrawable(
                    context.resources,
                    entry.sportType.markerIdWithGpx,
                    null
                )
            } else {
                marker.icon = ResourcesCompat.getDrawable(
                    context.resources,
                    entry.sportType.markerIdWithoutGpx,
                    null
                )
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
    fun addDefaultSettings(
        mMapView: MapView,
        fragmentActivity: FragmentActivity
    ) {

        val mScaleBarOverlay = ScaleBarOverlay(mMapView)
        mScaleBarOverlay.setTextSize(64f)
        mScaleBarOverlay.setAlignBottom(true)
        mScaleBarOverlay.setScaleBarOffset(64, 64)
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
    fun showMapTypeSelectorDialog(context: Context, mapView: MapView, onSelected: () -> Unit = {}) {
        val fDialogTitle = context.getString(R.string.select_map_type)
        val builder = AlertDialog.Builder(context)
        val mapProviders = getMapProviders(context)
        builder.setTitle(fDialogTitle)
        builder.setSingleChoiceItems(
            mapProviders.map { context.getString(it.textId) }.toTypedArray(),
            mapProviders.indexOf(selectedItem)
        ) { dialog: DialogInterface, item: Int ->
            selectedItem = mapProviders[item]
            setTileProvider(mapView, context)
            onSelected()
            dialog.dismiss()
        }

        val fMapTypeDialog = builder.create()
        fMapTypeDialog.setCanceledOnTouchOutside(true)
        fMapTypeDialog.show()
    }

    @JvmStatic
    fun setTileProvider(mapView: MapView, context: Context) {
        val mapFiles: List<DocumentFile> = FileHelper.getOnDeviceMapFiles(context)
        if (selectedItem.isOffline) {
            if (selectedItem == MapProvider.MBTILES) {
                setOsmConfForTiles()
                setOnlineMap(mapView, context)
                mapView.invalidate()
            } else {
                setOsmConfForTiles(true)
            }
            val provider = MapHelper.getOfflineMapProvider(context, mapFiles, selectedItem)
            if (provider != null) {
                Log.i(TAG, "Use offline map")
                mapView.setTileProvider(provider)
                setOsmConfForTiles()
            } else {
                setOnlineMap(mapView, context)
            }
        } else {
            setOnlineMap(mapView, context)
        }
    }

    private fun setOnlineMap(mapView: MapView, context: Context) {
        val tileSourceBase = selectedItem.onlineTileSourceBase
        if (tileSourceBase != null) {
            mapView.tileProvider = MapHelper.getOnlineMapProvider(tileSourceBase, context)
            mapView.setTileSource(tileSourceBase)
        }
    }

    private fun getMapProviders(context: Context): List<MapProvider> {
        return MapProvider.entries.filter { it.exists(context) }
    }

    fun getSportTypeForMapProviders(sportType: SportType, context: Context): MapProvider {
        return MapProvider.entries.find {
            it.relevantSportTypes.contains(sportType)
        } ?: if (PreferencesHelper.loadOnDeviceMaps() &&
            FileHelper.getOnDeviceMapFiles(context).isNotEmpty()
        ) {
            MapProvider.HIKING
        } else if (FileHelper.getOnDeviceMbtilesFiles(context).isNotEmpty()) {
            MapProvider.MBTILES
        } else {
            MapProvider.OPENTOPO
        }
    }
}

enum class MapProvider(
    var textId: Int,
    var onlineTileSourceBase: OnlineTileSourceBase?,
    var offlineStyle: String?,
    var isOffline: Boolean = false,
    var exists: (Context) -> Boolean = { true },
    var relevantSportTypes: List<SportType> = listOf()
) {
    OPENTOPO(
        R.string.open_topo_map_type,
        TileSourceFactory.OpenTopo,
        null
    ),
    MAPNIK(
        R.string.mapnik_map_type,
        TileSourceFactory.MAPNIK,
        null
    ),
    MBTILES(
        R.string.generic_offline_map_type,
        null,
        null,
        true,
        { context -> FileHelper.getOnDeviceMbtilesFiles(context).isNotEmpty() },
    ),
    HIKING(
        R.string.hiking_map_type,
        null,
        "elv-hiking",
        true,
        { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        },
        relevantSportTypes = listOf(
            SportType.Hike,
            SportType.Climb,
            SportType.BikeAndHike,
            SportType.Skitour
        )
    ),
    CITY(
        R.string.city_map_type,
        null,
        "elv-city",
        true,
        { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        },
        relevantSportTypes = listOf(SportType.Other, SportType.IndoorTrainer, SportType.Running)
    ),
    CYCLING(
        R.string.cycling_map_type,
        null,
        "elv-cycling",
        true,
        { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        },
        relevantSportTypes = listOf(SportType.Bicycle, SportType.Racer)
    ),
    MTB(
        R.string.mtb_map_type,
        null,
        "elv-mtb",
        true,
        { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        },
        relevantSportTypes = listOf(SportType.Mountainbike)
    )
}