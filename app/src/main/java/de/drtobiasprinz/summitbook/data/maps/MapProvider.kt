package de.drtobiasprinz.summitbook.data.maps

import android.content.Context
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.core.preferences.PreferencesHelper
import de.drtobiasprinz.summitbook.data.db.entities.GroupForHeatmap
import de.drtobiasprinz.summitbook.data.db.entities.SportType
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

enum class MapProvider(
    var textId: Int,
    var onlineTileSourceBase: OnlineTileSourceBase?,
    var offlineStyle: String?,
    var isOffline: Boolean = false,
    var exists: (Context) -> Boolean = { true },
    var relevantSportTypes: List<SportType> = listOf(),
    val heatmap: GroupForHeatmap = GroupForHeatmap.All
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
        heatmap = GroupForHeatmap.Winter
    ),
    HIKING(
        R.string.hiking_map_type, null, "elv-hiking", true, { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        }, relevantSportTypes = listOf(
            SportType.Hike, SportType.Climb, SportType.BikeAndHike, SportType.Skitour
        ),
        heatmap = GroupForHeatmap.Walking
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
        }, relevantSportTypes = listOf(SportType.Bicycle, SportType.Racer),
        heatmap = GroupForHeatmap.Bicycle
    ),
    MTB(
        R.string.mtb_map_type, null, "elv-mtb", true, { context ->
            PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(context)
                .isNotEmpty()
        }, relevantSportTypes = listOf(SportType.Mountainbike),
        heatmap = GroupForHeatmap.Bicycle
    ),
}
