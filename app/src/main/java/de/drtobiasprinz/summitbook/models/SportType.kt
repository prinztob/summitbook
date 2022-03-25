package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R

enum class SportType(val sportNameStringId: Int, val imageIdBlack: Int, val imageIdWhite: Int, val markerIdWithGpx: Int, val markerIdWithoutGpx: Int, val color: Int) {
    Bicycle(R.string.bicycle, R.drawable.icons8_cycling_50, R.drawable.icons8_cycling_white_50, R.drawable.ic_filled_location_green_48, R.drawable.ic_outline_location_green_48, R.color.green_200),
    Racer(R.string.racer, R.drawable.icons8_racer_50, R.drawable.icons8_racer_white_50, R.drawable.ic_filled_location_green_48, R.drawable.ic_outline_location_green_48, R.color.green_600),
    IndoorTrainer(R.string.indoor_trainer, R.drawable.icons8_spinning_50, R.drawable.icons8_spinning_white_50, R.drawable.ic_filled_location_green_48, R.drawable.ic_outline_location_green_48, R.color.green_800),
    Mountainbike(R.string.mountainbike, R.drawable.icons8_mtb_50, R.drawable.icons8_mtb_white_50, R.drawable.ic_filled_location_red_48, R.drawable.ic_outline_location_red_48, R.color.red_400),
    BikeAndHike(R.string.bikeAndHike, R.drawable.icons8_mtb_50, R.drawable.icons8_mtb_white_50, R.drawable.ic_filled_location_black_48, R.drawable.ic_outline_location_black_48, R.color.black),
    Climb(R.string.climb, R.drawable.icons8_climbing_50, R.drawable.icons8_climbing_white_50, R.drawable.ic_filled_location_darkbrown_48, R.drawable.ic_outline_location_darkbrown_48, R.color.brown_400),
    Hike(R.string.hike, R.drawable.icons8_trekking_50, R.drawable.icons8_trekking_white_50, R.drawable.ic_filled_location_lightbrown_48, R.drawable.ic_outline_location_lightbrown_48, R.color.orange_600),
    Running(R.string.running, R.drawable.icons8_running_50, R.drawable.icons8_running_white_50, R.drawable.ic_filled_location_lightbrown_48, R.drawable.ic_outline_location_lightbrown_48, R.color.orange_100),
    Skitour(R.string.skitour, R.drawable.icon_ski_touring, R.drawable.icon_ski_touring_white, R.drawable.ic_filled_location_blue_48, R.drawable.ic_outline_location_blue_48, R.color.blue_400),
    Other(R.string.other, R.drawable.ic_baseline_accessibility_24_black, R.drawable.ic_baseline_accessibility_24_white, R.drawable.ic_filled_location_black_48, R.drawable.ic_outline_location_black_48, R.color.grey_400);

    override fun toString(): String {
        return name
    }

}

enum class SportGroup(val sportTypes: List<SportType>) {
    Bike(listOf(SportType.Bicycle, SportType.Racer, SportType.IndoorTrainer, SportType.Mountainbike)),
    Foot(listOf(SportType.Climb, SportType.Hike, SportType.Skitour))
}