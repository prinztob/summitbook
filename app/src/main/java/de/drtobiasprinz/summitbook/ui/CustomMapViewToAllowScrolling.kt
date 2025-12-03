package de.drtobiasprinz.summitbook.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.TrackBoundingBox
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.LocationInfo
import de.drtobiasprinz.summitbook.models.RoadInfo
import de.drtobiasprinz.summitbook.models.RoadType
import de.drtobiasprinz.summitbook.models.Surface
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.cache
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.sharedPreferences
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.storage
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.MapTilesHelper
import de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzer
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class CustomMapViewToAllowScrolling : MapView {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    var updateBoundingBox = false

    /**
     * Adds a marker and optional GPS track to the map for a summit entry.
     *
     * @param summitEntry The summit entry to display
     * @param forceAddTrack Force redrawing the track even if already shown
     * @param selectedCustomizeTrackItem Track color customization option
     * @param alwaysShowTrackOnMap Whether to always show the track on map
     * @param rootView Optional root view for track interaction UI
     * @param calculateBondingBox Whether to calculate and zoom to bounding box
     * @return The created marker, or null if no location is available
     */
    fun addTrackAndMarker(
        summitEntry: Summit,
        forceAddTrack: Boolean,
        selectedCustomizeTrackItem: TrackColor,
        alwaysShowTrackOnMap: Boolean,
        rootView: View? = null,
        calculateBondingBox: Boolean = true
    ): Marker? {
        val geoPoints = ArrayList<GeoPoint>()
        
        // Add marker if location is available
        val marker = summitEntry.latLng?.let { latLng ->
            val point = GeoPoint(latLng.latitude, latLng.longitude)
            geoPoints.add(point)
            
            val createdMarker = addMarker(point, summitEntry, addToOverlay = false, alwaysShowTrackOnMap)
            
            // Center map on marker if no GPS track exists
            if (!summitEntry.hasGpsTrack()) {
                controller.apply {
                    setZoom(10.0)
                    setCenter(point)
                }
            }
            
            createdMarker
        }
        
        // Draw GPS track if available
        drawTrack(
            summitEntry = summitEntry,
            forceAddTrack = forceAddTrack,
            selectedCustomizeTrackItem = selectedCustomizeTrackItem,
            calculateBondingBox = calculateBondingBox,
            mGeoPoints = geoPoints,
            rootView = rootView
        )
        
        // Add marker to overlays after track to ensure it's drawn on top
        marker?.let { overlays.add(it) }
        
        return marker
    }

    fun drawBoundingBox(trackBoundingBox: TrackBoundingBox) {
        val polyline = Polyline(this)
        polyline.setOnClickListener { _, _, _ ->
            true // DO NOTHING
        }
        polyline.outlinePaint?.color = Color.BLACK
        polyline.outlinePaint?.strokeWidth = 6f
        polyline.setPoints(trackBoundingBox.getGeoPoints())
        this.overlayManager?.add(polyline)
    }

    fun drawTrack(
        summitEntry: Summit,
        forceAddTrack: Boolean,
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
                    gpsTrack.addGpsTrack(this, selectedCustomizeTrackItem, color, rootView)
                    gpsTrack.isShownOnMap = true
                }
                mGeoPoints.addAll(getTrackPointsFrom(gpsTrack))
            }
            if (calculateBondingBox) {
                this.post { calculateBoundingBox(mGeoPoints) }
            }
        }
    }

    fun addMarker(
        startPoint: GeoPoint?,
        entry: Summit,
        addToOverlay: Boolean = true,
        alwaysShowTrackOnMap: Boolean = false,
        useIconId: Int? = null
    ): Marker? {
        try {
            val marker = Marker(this)
            marker.position = startPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = entry.id.toString()
            val iconId = useIconId ?: if (entry.hasGpsTrack()) {
                entry.sportType.markerIdWithGpx
            } else {
                entry.sportType.markerIdWithoutGpx
            }
            marker.icon = ResourcesCompat.getDrawable(
                context.resources, iconId, null
            )
            marker.infoWindow = MapCustomInfoBubble(this, entry, context, alwaysShowTrackOnMap)
            marker.setOnMarkerClickListener { marker1, _ ->
                if (!marker1.isInfoWindowShown) {
                    marker1.showInfoWindow()
                } else {
                    marker1.closeInfoWindow()
                }
                false
            }
            if (addToOverlay) {
                this.overlays.add(marker)
            }
            return marker
        } catch (e: NullPointerException) {
            e.printStackTrace()
            return null
        }
    }

    fun calculateBoundingBox(mGeoPoints: List<GeoPoint?>) {
        if (mGeoPoints.size > 1) {
            val boundingBox = BoundingBox.fromGeoPoints(mGeoPoints)
            this.zoomToBoundingBox(boundingBox, false, 30)
        }
    }

    fun calculateBoundingBox(gpsTrack: GpsTrack, point: GeoPoint?) {
        val mGeoPoints = ArrayList<GeoPoint>()
        if (point != null) {
            mGeoPoints.add(point)
        }
        mGeoPoints.addAll(getTrackPointsFrom(gpsTrack))
        calculateBoundingBox(mGeoPoints)
    }

    fun addDefaultSettings() {
        val mScaleBarOverlay = ScaleBarOverlay(this)
        mScaleBarOverlay.setTextSize(64f)
        mScaleBarOverlay.setAlignBottom(true)
        mScaleBarOverlay.setScaleBarOffset(64, 64)
        this.overlays.add(mScaleBarOverlay)

        //support for map rotation
        val mRotationGestureOverlay = RotationGestureOverlay(this)
        mRotationGestureOverlay.isEnabled = true
        this.overlays.add(mRotationGestureOverlay)

        //needed for pinch zooms
        this.setMultiTouchControls(true)

        //scales tiles to the current screen's DPI, helps with readability of labels
        this.isTilesScaledToDpi = true
        val copyrightOverlay = CopyrightOverlay(context)
        copyrightOverlay.setTextSize(10)
        this.overlays.add(copyrightOverlay)
    }

    fun enableRoadInfoOnMapClick(scope: CoroutineScope? = null) {
        val coroutineScope = scope ?: CoroutineScope(Dispatchers.Main)
        val mapEventsReceiver = object : org.osmdroid.events.MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                Log.i(
                    "MAP",
                    "singleTapConfirmedHelper: position: $p, ${this@CustomMapViewToAllowScrolling.zoomLevelDouble}"
                )
                if (p != null) {
                    showRoadInfoAtPosition(context, p, coroutineScope)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                Log.i("MAP", "longPressHelper")
                return false
            }
        }

        val mapEventsOverlay = org.osmdroid.views.overlay.MapEventsOverlay(mapEventsReceiver)
        this.overlays.add(0, mapEventsOverlay)
    }

    fun showMapTypeSelectorDialog(onSelected: () -> Unit = {}) {
        val fDialogTitle = context.getString(R.string.select_map_type)
        val builder = AlertDialog.Builder(context)
        val mapProviders = getMapProviders(context)
        builder.setTitle(fDialogTitle)
        builder.setSingleChoiceItems(
            mapProviders.map { context.getString(it.textId) }.toTypedArray(),
            mapProviders.indexOf(selectedItem)
        ) { dialog: DialogInterface, item: Int ->
            selectedItem = mapProviders[item]
            setTileProvider()
            onSelected()
            dialog.dismiss()
        }

        val fMapTypeDialog = builder.create()
        fMapTypeDialog.setCanceledOnTouchOutside(true)
        fMapTypeDialog.show()
    }

    fun setTileProviderDependingOnSummitSportType(sportType: SportType) {
        if (PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        ) {
            selectedItem = getSportTypeForMapProviders(sportType, context)
        } else if (FileHelper.getOnDeviceMbtilesFiles(context).isNotEmpty()) {
            selectedItem = MapProvider.MBTILES
        }
        setTileProvider()
    }

    fun setTileProvider() {
        enableRoadInfoOnMapClick()
        val mapFiles: List<DocumentFile> = FileHelper.getOnDeviceMapFiles(context)
        if (selectedItem.isOffline) {
            if (selectedItem == MapProvider.MBTILES) {
                Log.i(TAG, "Use MBTILES map")
                setOsmConfForTiles()
                setOnlineMap(context)
                this.invalidate()
            } else {
                setOsmConfForTiles(true)
            }
            val provider = MapTilesHelper.getOfflineMapProviderWithHillShading(
                context,
                mapFiles,
                selectedItem
            )
            if (provider != null) {
                Log.i(TAG, "Use offline map")
                this.setTileProvider(provider)
            } else {
                setOnlineMap(context)
            }
        } else {
            setOnlineMap(context)
        }
    }

    private fun setOnlineMap(context: Context) {
        val tileSourceBase = selectedItem.onlineTileSourceBase
        if (tileSourceBase != null) {
            this.tileProvider = MapTilesHelper.getOnlineMapProvider(tileSourceBase, context)
            this.setTileSource(tileSourceBase)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN ->                 // Disallow ScrollView to intercept touch events.
                this.parent.requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP ->                 // Allow ScrollView to intercept touch events.
                this.parent.requestDisallowInterceptTouchEvent(false)
        }
        if (updateBoundingBox) {
            cleanupAndSaveCurrentStatus()
        }
        return super.onTouchEvent(ev)
    }

    private fun cleanupAndSaveCurrentStatus() {
        Log.d(TAG, "Content: ${sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")}")
        val osMapBoundingBox =
            sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")?.split(";")
                ?: emptyList()
        val showSummits = if (osMapBoundingBox.size == 6) osMapBoundingBox[4] else "0"
        val showBookmarks = if (osMapBoundingBox.size == 6) osMapBoundingBox[5] else "0"
        sharedPreferences.edit {
            putString(
                Keys.PREF_OS_MAP_BOUNDING_BOX,
                "${boundingBox.latNorth};${boundingBox.lonEast};${boundingBox.latSouth};${boundingBox.lonWest};$showSummits;$showBookmarks"
            )
        }
        Log.d(TAG, "Content: ${sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")}")
    }

    companion object {
        const val TAG = "CustomMapViewToAllowScrolling"

        var selectedItem = MapProvider.OPENTOPO

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

        fun setOsmConfForTiles(setToDefault: Boolean = false) {
            val osmConf = Configuration.getInstance()
            val osmdroidBasePath = getOsmdroidTilesFolder()
            osmdroidBasePath.mkdirs()
            osmConf.osmdroidBasePath =
                if (setToDefault) File(cache, "osmdroid") else osmdroidBasePath
            Log.i(TAG, "set osmdroidBasePath to ${osmConf.osmdroidBasePath}")
            val tileCache = File(cache, "tile")
            tileCache.mkdirs()
            osmConf.osmdroidTileCache = tileCache
        }

        fun showRoadInfoAtPosition(
            context: Context, geoPoint: GeoPoint, scope: CoroutineScope
        ) {
            Toast.makeText(context, "Querying road info...", Toast.LENGTH_SHORT).show()
            scope.launch {
                try {
                    var info: Pair<RoadInfo?, LocationInfo?>? = null
                    withContext(Dispatchers.IO) {
                        val analyzer = OfflineMapAnalyzer.from(context)
                        info = analyzer.getInfosForLocation(geoPoint)
                    }

                    // Show result on main thread
                    if (info != null) {
                        withContext(Dispatchers.Main) {
                            showMapPointInfo(context, geoPoint, info)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error querying road info", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fun showMapPointInfo(
            context: Context, geoPoint: GeoPoint, info: Pair<RoadInfo?, LocationInfo?>
        ) {
            val message = buildString {
                append("lat: ${geoPoint.latitude}, long: ${geoPoint.longitude}\n\n")

                info.first?.let { roadInfo ->
                    append("Road Info:\n")
                    append(roadInfo.toString())
                    append("\n\n")
                    append("Mapped Surface: ${Surface.mapFromRoadInfo(roadInfo)}\n")
                    append("Mapped RoadType: ${RoadType.mapFromRoadInfo(roadInfo)}\n\n")
                }

                info.second?.let { locationInfo ->
                    append("Location Info:\n")
                    append(locationInfo.toString())
                }
            }

            AlertDialog.Builder(context)
                .setTitle("Road and location Information")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }

        private fun getMapProviders(context: Context): List<MapProvider> {
            return MapProvider.entries.filter { it.exists(context) }
        }

        fun getSportTypeForMapProviders(sportType: SportType, context: Context): MapProvider {
            return MapProvider.entries.find {
                it.relevantSportTypes.contains(sportType)
            } ?: if (PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                    .isNotEmpty()
            ) {
                MapProvider.HIKING
            } else if (FileHelper.getOnDeviceMbtilesFiles(context).isNotEmpty()) {
                MapProvider.MBTILES
            } else {
                MapProvider.OPENTOPO
            }
        }

        fun Double.round(decimals: Int = 2): String = "%.${decimals}f".format(this)
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
        R.string.open_topo_map_type, TileSourceFactory.OpenTopo, null
    ),
    MAPNIK(
        R.string.mapnik_map_type, TileSourceFactory.MAPNIK, null
    ),
    MBTILES(
        R.string.generic_offline_map_type,
        null,
        null,
        true,
        { context -> FileHelper.getOnDeviceMbtilesFiles(context).isNotEmpty() },
    ),
    HIKING(
        R.string.hiking_map_type, null, "elv-hiking", true, { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        }, relevantSportTypes = listOf(
            SportType.Hike, SportType.Climb, SportType.BikeAndHike, SportType.Skitour
        )
    ),
    CITY(
        R.string.city_map_type, null, "elv-city", true, { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        }, relevantSportTypes = listOf(SportType.Other, SportType.IndoorTrainer, SportType.Running)
    ),
    CYCLING(
        R.string.cycling_map_type, null, "elv-cycling", true, { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        }, relevantSportTypes = listOf(SportType.Bicycle, SportType.Racer)
    ),
    MTB(
        R.string.mtb_map_type, null, "elv-mtb", true, { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        }, relevantSportTypes = listOf(SportType.Mountainbike)
    )
}