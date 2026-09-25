package de.drtobiasprinz.summitbook.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.core.preferences.PreferencesHelper
import de.drtobiasprinz.summitbook.data.appstate.AppState.cache
import de.drtobiasprinz.summitbook.data.appstate.AppState.sharedPreferences
import de.drtobiasprinz.summitbook.data.db.entities.RoadType
import de.drtobiasprinz.summitbook.data.db.entities.SportType
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.db.entities.Surface
import de.drtobiasprinz.summitbook.data.db.entities.TrackBoundingBox
import de.drtobiasprinz.summitbook.data.maps.FileHelper
import de.drtobiasprinz.summitbook.data.maps.MapProvider
import de.drtobiasprinz.summitbook.data.maps.MapTilesHelper
import de.drtobiasprinz.summitbook.data.maps.OfflineMapAnalyzer
import de.drtobiasprinz.summitbook.data.model.ExtensionFromYaml
import de.drtobiasprinz.summitbook.data.model.GpsTrack
import de.drtobiasprinz.summitbook.data.model.GpsTrack.Companion.getAnimatedPathManager
import de.drtobiasprinz.summitbook.data.model.GpsTrack.Companion.getHalfKilometerManager
import de.drtobiasprinz.summitbook.data.model.GpsTrack.Companion.getKilometerManager
import de.drtobiasprinz.summitbook.data.model.LocationInfo
import de.drtobiasprinz.summitbook.data.model.RoadInfo
import de.drtobiasprinz.summitbook.data.model.TrackColor
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.milestones.MilestoneManager
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceSliceLister
import java.io.File
import kotlin.math.abs

class CustomMapViewToAllowScrolling : MapView {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    var updateBoundingBox = false

    /**
     * Optional host callback for transient user messages (e.g. track taps or
     * road-info status). When set, messages are routed there instead of Toasts.
     */
    var onShowMessage: ((String) -> Unit)? = null

    private var fallbackRoadInfoScope: CoroutineScope? = null

    private val mainScope = CoroutineScope(Dispatchers.Main.immediate)

    private var onDeviceMapFilesCache: Pair<String, List<DocumentFile>>? = null

    var osMapRoute: Polyline? = null

    private var summitMarker: Marker? = null

    private var boundingBoxPolyline: Polyline? = null

    private fun showMessage(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val callback = onShowMessage
        if (callback != null) {
            callback(message)
        } else {
            Toast.makeText(context, message, duration).show()
        }
    }

    /**
     * Adds a marker and optional GPS track to the map for a summit entry.
     *
     * @param summitEntry The summit entry to display
     * @param forceAddTrack Force redrawing the track even if already shown
     * @param selectedCustomizeTrackItem Track color customization option
     * @param alwaysShowTrackOnMap Whether to always show the track on map
     * @param calculateBondingBox Whether to calculate and zoom to bounding box
     * @return The created marker, or null if no location is available
     */
    fun addTrackAndMarker(
        summitEntry: Summit,
        trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        forceAddTrack: Boolean,
        selectedCustomizeTrackItem: TrackColor,
        alwaysShowTrackOnMap: Boolean,
        addInfoWindow: Boolean = true,
        calculateBondingBox: Boolean = true,
        onTrackPointSelected: (Int) -> Unit = {}
    ): Marker? {
        val geoPoints = ArrayList<GeoPoint>()

        // Add marker if location is available
        val marker = summitEntry.latLng?.let { latLng ->
            val point = GeoPoint(latLng.latitude, latLng.longitude)
            geoPoints.add(point)

            val createdMarker =
                addMarker(point, summitEntry, addToOverlay = false, alwaysShowTrackOnMap, addInfoWindow=addInfoWindow)

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
            trackPoints = trackPoints,
            forceAddTrack = forceAddTrack,
            selectedCustomizeTrackItem = selectedCustomizeTrackItem,
            calculateBondingBox = calculateBondingBox,
            mGeoPoints = geoPoints,
            onTrackPointSelected = onTrackPointSelected
        )

        // Add marker to overlays after track to ensure it's drawn on top;
        // replace the previous one so repeated calls do not stack markers
        summitMarker?.let { overlays.remove(it) }
        marker?.let { overlays.add(it) }
        summitMarker = marker

        return marker
    }

    fun drawBoundingBox(trackBoundingBox: TrackBoundingBox) {
        val polyline = Polyline(this)
        polyline.setInfoWindow(null) // Prevent default BasicInfoWindow crash
        polyline.setOnClickListener { _, _, _ ->
            true // DO NOTHING
        }
        polyline.outlinePaint?.color = Color.BLACK
        polyline.outlinePaint?.strokeWidth = 6f
        polyline.setPoints(trackBoundingBox.getGeoPoints())
        boundingBoxPolyline?.let { overlayManager?.remove(it) }
        boundingBoxPolyline = polyline
        this.overlayManager?.add(polyline)
    }

    fun drawTrack(
        trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        forceAddTrack: Boolean,
        selectedCustomizeTrackItem: TrackColor,
        calculateBondingBox: Boolean = false,
        mGeoPoints: ArrayList<GeoPoint> = arrayListOf(),
        color: Int = Color.BLUE,
        onTrackPointSelected: (Int) -> Unit = {}
    ) {
        if (osMapRoute == null || forceAddTrack) {
            addGpsTrack(
                this,
                trackPoints,
                selectedCustomizeTrackItem,
                color,
                onTrackPointSelected = onTrackPointSelected
            )
        }
        mGeoPoints.addAll(getTrackPointsFrom(trackPoints))
        if (calculateBondingBox) {
            this.post { calculateBoundingBox(mGeoPoints) }
        }
    }

    fun addGpsTrack(
        mMapView: MapView?,
        trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        selectedCustomizeTrackItem: TrackColor = TrackColor.None,
        color: Int = COLOR_POLYLINE_STATIC,
        summit: Summit? = null,
        onTrackPointSelected: (Int) -> Unit = {}
    ) {
        var usedTrackPoints = trackPoints
        try {
            if (osMapRoute != null) {
                mMapView?.overlays?.remove(osMapRoute)
            }
            osMapRoute = Polyline(mMapView)
            osMapRoute?.setInfoWindow(null) // Prevent default BasicInfoWindow crash

            osMapRoute?.setOnClickListener { _, _, eventPos ->
                if (mMapView != null) {
                    if (summit != null) {
                        showMessage(
                            mMapView.context,
                            "${summit.getDateAsString()} ${summit.name}",
                            Toast.LENGTH_LONG
                        )
                    }

                    val closestPoint = trackPoints.withIndex().minByOrNull { (_, trackPoint) ->
                        val trackPointGeo =
                            GeoPoint(trackPoint.first.latitude, trackPoint.first.longitude)
                        abs(trackPointGeo.distanceToAsDouble(eventPos))
                    }?.index
                    if (closestPoint != null) {
                        onTrackPointSelected(closestPoint)
                    }
                }
                return@setOnClickListener true
            }
            osMapRoute?.outlinePaint?.color = color
            osMapRoute?.outlinePaint?.strokeWidth = LINE_WIDTH_BIG
            osMapRoute?.outlinePaint?.strokeCap = Paint.Cap.ROUND
            val paintBorder = Paint()
            paintBorder.strokeWidth = 20f
            when (selectedCustomizeTrackItem) {
                TrackColor.Mileage -> {
                    val managers: MutableList<MilestoneManager> = ArrayList()
                    val slicerForPath = MilestoneMeterDistanceSliceLister()
                    managers.add(getAnimatedPathManager(slicerForPath))
                    managers.add(getHalfKilometerManager())
                    managers.add(getKilometerManager())
                    osMapRoute?.setMilestoneManagers(managers)
                }

                TrackColor.None -> {

                    Log.i(
                        TAG,
                        "Nothing to do, selectedCustomizeTrackItem is set to 0"
                    )
                }

                else -> {
                    usedTrackPoints = getUsedPoints(trackPoints, selectedCustomizeTrackItem.f)
                    GpsTrack.addColorToTrack(
                        trackPoints,
                        osMapRoute,
                        paintBorder,
                        selectedCustomizeTrackItem
                    )
                }
            }
            osMapRoute?.setPoints(usedTrackPoints.map {
                GeoPoint(
                    it.first.latitude,
                    it.first.longitude,
                    it.first.elevation ?: 0.0
                )
            })
            mMapView?.overlayManager?.add(osMapRoute)

        } catch (e: NullPointerException) {
            Log.e(TAG, "Failed to add GPS track", e)
        }
    }

    fun addAdditionalGpsTrack(
        trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        color: Int = COLOR_POLYLINE_STATIC,
        lineWidth: Float = LINE_WIDTH_BIG
    ): Polyline? {
        return try {
            val additionalRoute = Polyline(this)
            additionalRoute.setInfoWindow(null) // Prevent default BasicInfoWindow crash
            additionalRoute.outlinePaint?.color = color
            additionalRoute.outlinePaint?.strokeWidth = lineWidth
            additionalRoute.outlinePaint?.strokeCap = Paint.Cap.ROUND
            
            additionalRoute.setPoints(trackPoints.map {
                GeoPoint(
                    it.first.latitude,
                    it.first.longitude,
                    it.first.elevation ?: 0.0
                )
            })
            overlayManager?.add(additionalRoute)
            additionalRoute
        } catch (e: NullPointerException) {
            Log.e(TAG, "Failed to add additional GPS track", e)
            null
        }
    }

    private fun getUsedPoints(
        trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        f: (Pair<TrackPoint, ExtensionFromYaml>) -> Double?
    ): List<Pair<TrackPoint, ExtensionFromYaml>> {
        return trackPoints.filter { f(it) != null }.toMutableList()
    }

    fun addMarker(
        startPoint: GeoPoint?,
        entry: Summit,
        addToOverlay: Boolean = true,
        alwaysShowTrackOnMap: Boolean = false,
        useIconId: Int? = null,
        addInfoWindow: Boolean = true
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
            if (addInfoWindow) {
                marker.infoWindow = MapCustomInfoBubble(this, entry, context, alwaysShowTrackOnMap)
                marker.setOnMarkerClickListener { marker1, _ ->
                    if (!marker1.isInfoWindowShown) {
                        marker1.showInfoWindow()
                    } else {
                        marker1.closeInfoWindow()
                    }
                    false
                }
            } else {
                // Disable default info window behavior when addInfoWindow is false
                marker.infoWindow = null // Explicitly set to null to prevent default info window
                marker.setOnMarkerClickListener { _, _ ->
                    true // Consume the click event without showing info window
                }
            }
            if (addToOverlay) {
                this.overlays.add(marker)
            }
            return marker
        } catch (e: NullPointerException) {
            Log.e(TAG, "Failed to add marker", e)
            return null
        }
    }

    fun calculateBoundingBox(mGeoPoints: List<GeoPoint?>) {
        if (mGeoPoints.size > 1) {
            val boundingBox = BoundingBox.fromGeoPoints(mGeoPoints)
            this.zoomToBoundingBox(boundingBox, false, 30)
        }
    }

    fun calculateBoundingBox(
        trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        point: GeoPoint?
    ) {
        val mGeoPoints = ArrayList<GeoPoint>()
        if (point != null) {
            mGeoPoints.add(point)
        }
        mGeoPoints.addAll(getTrackPointsFrom(trackPoints))
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
        // Check if a MapEventsOverlay already exists to avoid adding multiple overlays
        val existingOverlay = overlays.firstOrNull { it is org.osmdroid.views.overlay.MapEventsOverlay }
        if (existingOverlay != null) {
            return // Already exists, don't add another one
        }
        
        val coroutineScope = if (scope != null) {
            scope
        } else {
            fallbackRoadInfoScope?.cancel()
            CoroutineScope(Dispatchers.Main).also { fallbackRoadInfoScope = it }
        }
        val mapEventsReceiver = object : org.osmdroid.events.MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                Log.i(
                    "MAP",
                    "singleTapConfirmedHelper: position: $p, ${this@CustomMapViewToAllowScrolling.zoomLevelDouble}"
                )
                if (p != null) {
                    showRoadInfoAtPosition(context, p, coroutineScope, onShowMessage)
                }
                return false // Don't consume the event, allow it to pass through to other overlays
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
        // getMapProviders() queries the maps folder via DocumentFile; keep that
        // storage access off the main thread (StrictMode DiskReadViolation)
        mainScope.launch {
            val mapProviders = withContext(Dispatchers.IO) { getMapProviders(context) }
            if (!isAttachedToWindow) return@launch
            val builder = AlertDialog.Builder(context)
            builder.setTitle(fDialogTitle)
            builder.setSingleChoiceItems(
                mapProviders.map { context.getString(it.textId) }.toTypedArray(),
                mapProviders.indexOf(selectedItem)
            ) { dialog: DialogInterface, item: Int ->
                selectedItem = mapProviders[item]
                persistSelectedMapProvider()
                setTileProvider()
                onSelected()
                dialog.dismiss()
            }

            val fMapTypeDialog = builder.create()
            fMapTypeDialog.setCanceledOnTouchOutside(true)
            fMapTypeDialog.show()
        }
    }

    /**
     * Memoized [FileHelper.getOnDeviceMapFiles]: repeated calls (e.g. from
     * Compose update lambdas) must not re-query storage. Cached per maps
     * folder URI so a newly selected folder invalidates the cache.
     */
    private fun getOnDeviceMapFilesCached(): List<DocumentFile> {
        val folderUri = PreferencesHelper.loadOnDeviceMapsFolder()
        val cache = onDeviceMapFilesCache
        if (cache != null && cache.first == folderUri) {
            return cache.second
        }
        val mapFiles = FileHelper.getOnDeviceMapFiles(context)
        onDeviceMapFilesCache = folderUri to mapFiles
        return mapFiles
    }

    fun setTileProviderDependingOnSummitSportType(sportType: SportType) {
        if (PreferencesHelper.loadOnDeviceMaps() && getOnDeviceMapFilesCached().isNotEmpty()) {
            selectedItem = getSportTypeForMapProviders(sportType, context)
        } else if (FileHelper.getOnDeviceMbtilesFiles(context).isNotEmpty()) {
            selectedItem = MapProvider.MBTILES
        }
        setTileProvider()
    }

    fun setTileProvider() {
        enableRoadInfoOnMapClick()
        val mapFiles: List<DocumentFile> = getOnDeviceMapFilesCached()
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
                selectedItem,
                plainTheme = usePlainMapTheme
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

    override fun onDetach() {
        fallbackRoadInfoScope?.cancel()
        fallbackRoadInfoScope = null
        mainScope.cancel()
        super.onDetach()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN ->                 // Disallow ScrollView to intercept touch events.
                this.parent.requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->                 // Allow ScrollView to intercept touch events again.
                this.parent.requestDisallowInterceptTouchEvent(false)
        }
        if (updateBoundingBox &&
            (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL)
        ) {
            // Persist the bounding box once, when the gesture ends
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
        const val TAG = "CustomMap"
        const val LINE_WIDTH_BIG = 16f
        const val COLOR_POLYLINE_STATIC = Color.BLUE
        // Restored from preferences so an explicitly selected map type survives
        // process death. Programmatic (auto-detected) writes must not call
        // [persistSelectedMapProvider].
        var selectedItem: MapProvider = loadPersistedMapProvider()

        /**
         * Persists the explicitly user-selected map type. Only call this for
         * explicit user choices (map type dialog), never for programmatic
         * auto-detection by sport type or offline-map availability.
         */
        fun persistSelectedMapProvider() {
            sharedPreferences.edit { putString(Keys.PREF_MAP_PROVIDER, selectedItem.name) }
        }

        private fun loadPersistedMapProvider(): MapProvider {
            return runCatching {
                val stored = sharedPreferences.getString(Keys.PREF_MAP_PROVIDER, null)
                MapProvider.entries.firstOrNull { it.name == stored } ?: MapProvider.OPENTOPO
            }.getOrDefault(MapProvider.OPENTOPO)
        }

        /**
         * When true, offline MapsForge maps are rendered with the plain built-in
         * theme (no Elevate render theme, no hill shading) so that an overlaid
         * heatmap is clearly visible. Set by the map screen whenever the heatmap
         * overlay is toggled.
         */
        var usePlainMapTheme = false

        private fun getTrackPointsFrom(trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>): List<GeoPoint> {
            return trackPoints.filter { it.first.latitude != 0.0 && it.first.longitude != 0.0 }
                .map { GeoPoint(it.first.latitude, it.first.longitude) }
        }

        fun setOsmConfForTiles(setToDefault: Boolean = false) {
            val osmConf = Configuration.getInstance()
            val osmdroidBasePath = MapTilesHelper.getOsmdroidTilesFolder()
            osmdroidBasePath.mkdirs()
            osmConf.osmdroidBasePath =
                if (setToDefault) File(cache, "osmdroid") else osmdroidBasePath
            Log.i(TAG, "set osmdroidBasePath to ${osmConf.osmdroidBasePath}")
            val tileCache = File(cache, "tile")
            tileCache.mkdirs()
            osmConf.osmdroidTileCache = tileCache
        }

        fun showRoadInfoAtPosition(
            context: Context, geoPoint: GeoPoint, scope: CoroutineScope,
            onShowMessage: ((String) -> Unit)? = null
        ) {
            val queryingMessage = context.getString(R.string.querying_road_info)
            if (onShowMessage != null) {
                onShowMessage(queryingMessage)
            } else {
                Toast.makeText(context, queryingMessage, Toast.LENGTH_SHORT).show()
            }
            scope.launch {
                try {
                    val info = withContext(Dispatchers.IO) {
                        OfflineMapAnalyzer.from(context).use { analyzer ->
                            analyzer.getInfosForLocation(geoPoint)
                        }
                    }

                    // Show result on main thread
                    withContext(Dispatchers.Main) {
                        showMapPointInfo(context, geoPoint, info)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error querying road info", e)
                    withContext(Dispatchers.Main) {
                        val errorMessage =
                            context.getString(R.string.road_info_error, e.message ?: "")
                        if (onShowMessage != null) {
                            onShowMessage(errorMessage)
                        } else {
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        fun showMapPointInfo(
            context: Context, geoPoint: GeoPoint, info: Pair<RoadInfo?, LocationInfo?>
        ) {
            val message = buildString {
                append(context.getString(R.string.road_info_lat, geoPoint.latitude.toString()))
                append(", ")
                append(context.getString(R.string.road_info_long, geoPoint.longitude.toString()))
                append("\n\n")

                info.first?.let { roadInfo ->
                    append(context.getString(R.string.road_info_heading))
                    append("\n")
                    append(roadInfo.toString())
                    append("\n\n")
                    append(context.getString(R.string.road_info_surface, Surface.mapFromRoadInfo(roadInfo)))
                    append("\n")
                    append(context.getString(R.string.road_info_road_type, RoadType.mapFromRoadInfo(roadInfo)))
                    append("\n\n")
                }

                info.second?.let { locationInfo ->
                    append(context.getString(R.string.road_info_location_heading))
                    append("\n")
                    append(locationInfo.toString())
                }
            }

            AlertDialog.Builder(context)
                .setTitle(R.string.road_info_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
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
