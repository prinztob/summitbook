package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R

enum class SportType(val sportNameStringId: Int, val abbreviationStringId: Int, val imageId: Int, val markerIdWithGpx: Int, val markerIdWithoutGpx: Int, val color: Int) {
    Bicycle(R.string.bicycle, R.string.bicycle_short, R.drawable.icons8_cycling_50, R.drawable.ic_filled_location_green_48, R.drawable.ic_outline_location_green_48, R.color.green_200),
    Racer(R.string.racer, R.string.racer_short, R.drawable.icons8_cycling_50, R.drawable.ic_filled_location_green_48, R.drawable.ic_outline_location_green_48, R.color.green_600),
    Mountainbike(R.string.mountainbike, R.string.mountainbike_short, R.drawable.icons8_mtb_50, R.drawable.ic_filled_location_red_48, R.drawable.ic_outline_location_red_48, R.color.red_400),
    BikeAndHike(R.string.bikeAndHike, R.string.bikeAndHike_short, R.drawable.icons8_mtb_50, R.drawable.ic_filled_location_black_48, R.drawable.ic_outline_location_black_48, R.color.black),
    Climb(R.string.climb, R.string.climb_short, R.drawable.icons8_climbing, R.drawable.ic_filled_location_darkbrown_48, R.drawable.ic_outline_location_darkbrown_48, R.color.brown_400),
    Hike(R.string.hike, R.string.hike_short, R.drawable.icons8_trekking_50, R.drawable.ic_filled_location_lightbrown_48, R.drawable.ic_outline_location_lightbrown_48, R.color.orange_600),
    Running(R.string.running, R.string.running_short, R.drawable.ic_baseline_directions_run_50, R.drawable.ic_filled_location_lightbrown_48, R.drawable.ic_outline_location_lightbrown_48, R.color.orange_100),
    Skitour(R.string.skitour, R.string.skitour_short, R.drawable.icon_ski_touring, R.drawable.ic_filled_location_blue_48, R.drawable.ic_outline_location_blue_48, R.color.blue_400),
    Other(R.string.other, R.string.other_short, R.drawable.ic_accessibility_black_24dp, R.drawable.ic_filled_location_black_48, R.drawable.ic_outline_location_black_48, R.color.grey_400);

    override fun toString(): String {
        return name
    }

}