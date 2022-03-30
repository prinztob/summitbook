 package de.drtobiasprinz.summitbook.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.MapCustomInfoBubble
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addMarker
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.calculateBoundingBox
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.setTileSource
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.showMapTypeSelectorDialog
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class OpenStreetMapFragment() : Fragment(), SummationFragment {
    private var mGeoPoints: MutableList<GeoPoint?> = ArrayList()
    private var mMarkers: MutableList<Marker?> = ArrayList()
    private var mMarkersShown: MutableList<Marker?> = ArrayList()
    private var gotoLocationDialog: AlertDialog? = null
    private var summitEntries: List<Summit>? = null
    private var filteredEntries: List<Summit>? = null
    private var mMapView: MapView? = null
    private lateinit var root: View
    private var maxPointsToShow: Int = 10000
    private lateinit var resultreceiver: FragmentResultReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultreceiver = context as FragmentResultReceiver
        setHasOptionsMenu(true)
        val context = requireContext()
        maxPointsToShow = (resultreceiver.getSharedPreference().getString("max_number_points", maxPointsToShow.toString())?: maxPointsToShow.toString()).toInt()
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        root = inflater.inflate(R.layout.fragment_open_street_map, container, false)
        setHasOptionsMenu(true)
        resultreceiver.getSortFilterHelper().fragment = this
        summitEntries = resultreceiver.getSortFilterHelper().entries
        filteredEntries = resultreceiver.getSortFilterHelper().filteredEntries.filter { it.sportType != SportType.IndoorTrainer }
        setMap()
        Log.d(TAG, "onCreateView")
        return root
    }

    private fun setMap() {
        val mMapView = root.findViewById<MapView>(R.id.osmap)
        this.mMapView = mMapView
        val changeMapTypeButton: ImageButton = root.findViewById(R.id.change_map)
        changeMapTypeButton.setOnClickListener { showMapTypeSelectorDialog(requireContext(), mMapView) }

        val showAllTracksButton: ImageButton = root.findViewById(R.id.show_all_tracks)
        showAllTracksButton.setOnClickListener { _: View? ->
            var pointsShown = mMarkersShown.sumBy { (it?.infoWindow as MapCustomInfoBubble).entry.gpsTrack?.trackPoints?.size?:0 }
            val markersInBoundingBox = mMarkers.filter {
                val mapCustomInfoBubble: MapCustomInfoBubble = it?.infoWindow as MapCustomInfoBubble
                val shouldBeShown = mMapView?.boundingBox?.let { it1 -> mapCustomInfoBubble.entry.isInBoundingBox(it1) }
                if (shouldBeShown == false && it in mMarkersShown) {
                    pointsShown -= mapCustomInfoBubble.entry.gpsTrack?.trackPoints?.size?: 0
                    mapCustomInfoBubble.updateGpxTrack(forceRemove = true)
                    mMarkersShown.remove(it)

                }
                shouldBeShown == true
            }
            markersInBoundingBox.forEach {
                if (it != null) {
                    val infoWindow: MapCustomInfoBubble = it.infoWindow as MapCustomInfoBubble
                    if (it !in mMarkersShown) {
                        if (pointsShown < maxPointsToShow) {
                            infoWindow.updateGpxTrack(forceShow = true)
                            pointsShown += infoWindow.entry.gpsTrack?.trackPoints?.size?: 0
                            mMarkersShown.add(it)
                        }
                    }
                }
            }
            if (pointsShown > maxPointsToShow) {
                if (context != null) {
                    Toast.makeText(context, String.format(requireContext().resources.getString(R.string.summits_shown, mMarkersShown.size.toString(), markersInBoundingBox.size.toString())), Toast.LENGTH_LONG).show()
                }
            }
        }

        requireActivity().findViewById<View>(R.id.add_new_summit).visibility = View.INVISIBLE

        mMapView?.setOnGenericMotionListener { _: View?, event: MotionEvent ->
            if (0 != event.source and InputDevice.SOURCE_CLASS_POINTER) {
                if (event.action == MotionEvent.ACTION_SCROLL) {
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        mMapView.controller?.zoomOut()
                    } else {
                        mMapView.controller?.zoomIn()
                    }
                    return@setOnGenericMotionListener true
                }
            }
            false
        }
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val localMapView = mMapView
        if (localMapView != null) {
            setTileSource(selectedItem, localMapView)
            addOverlays()
            val context: Context? = this.activity
            val mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), localMapView)
            mLocationOverlay.enableMyLocation()
            localMapView.overlays.add(mLocationOverlay)
            addDefaultSettings(requireContext(), localMapView, requireActivity())
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDetach")
        mMapView?.onDetach()
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        gotoLocationDialog?.dismiss()
        requireActivity().findViewById<View>(R.id.add_new_summit).visibility = View.VISIBLE
    }

    private fun addOverlays() {
        addAllMarkers()
    }

    private fun addAllMarkers() {
        val localMapView = mMapView
        if (localMapView != null) {
            localMapView.overlays?.clear()
            mGeoPoints = ArrayList()
            mMarkers = ArrayList()
            val filteredEntriesLocal = filteredEntries
            if (filteredEntriesLocal != null) {
                for (entry in filteredEntriesLocal) {
                    val latLng = entry.latLng
                    if (latLng != null && mMapView != null) {
                        val point = GeoPoint(latLng.lat, latLng.lon)
                        mGeoPoints.add(point)
                        mMarkers.add(addMarker(localMapView, requireContext(), point, entry))
                    }
                }
            }
            val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    return false
                }

                override fun longPressHelper(arg0: GeoPoint): Boolean {
                    Log.d("debug", "LongPressHelper")
                    mMarkers.forEach {
                        if (it?.isInfoWindowShown == true) {
                            it.infoWindow.close()
                        }
                    }
                    //your onLongPress logic here
                    return false
                }
            }

            val eventsOverlay = MapEventsOverlay(mReceive)
            mMapView?.overlays?.add(eventsOverlay)
            mMapView?.post { calculateBoundingBox(localMapView, mGeoPoints) }
        }
    }

    override fun update(filteredSummitEntries: List<Summit>?) {
        if (mMapView != null) {
            filteredEntries = filteredSummitEntries?.filter { it.sportType != SportType.IndoorTrainer }
            addAllMarkers()
        }
    }

    companion object {
        const val TAG = "osmBaseFrag"
    }

}