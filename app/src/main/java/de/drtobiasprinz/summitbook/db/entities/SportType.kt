package de.drtobiasprinz.summitbook.db.entities

import de.drtobiasprinz.summitbook.R

enum class SportType(
    val sportNameStringId: Int,
    val imageIdBlack: Int,
    val imageIdWhite: Int,
    val markerIdWithGpx: Int,
    val markerIdWithoutGpx: Int,
    val color: Int,
    val garminTypeIds: List<Int>
) {
    Bicycle(
        R.string.bicycle,
        R.drawable.icons8_cycling_50,
        R.drawable.icons8_cycling_white_50,
        R.drawable.ic_filled_location_green_48,
        R.drawable.ic_outline_location_green_48,
        R.color.green_200,
        listOf(2, 10, 21, 22)
    ),
    Racer(
        R.string.racer,
        R.drawable.icons8_racer_50,
        R.drawable.icons8_racer_white_50,
        R.drawable.ic_filled_location_green_48,
        R.drawable.ic_outline_location_green_48,
        R.color.green_600,
        listOf(19)
    ),
    IndoorTrainer(
        R.string.indoor_trainer,
        R.drawable.icons8_spinning_50,
        R.drawable.icons8_spinning_white_50,
        R.drawable.ic_filled_location_green_48,
        R.drawable.ic_outline_location_green_48,
        R.color.green_800,
        listOf(25)
    ),
    Mountainbike(
        R.string.mountainbike,
        R.drawable.icons8_mtb_50,
        R.drawable.icons8_mtb_white_50,
        R.drawable.ic_filled_location_red_48,
        R.drawable.ic_outline_location_red_48,
        R.color.red_400,
        listOf(5, 20, 143)
    ),
    BikeAndHike(
        R.string.bikeAndHike,
        R.drawable.icons8_mtb_50,
        R.drawable.icons8_mtb_white_50,
        R.drawable.ic_filled_location_black_48,
        R.drawable.ic_outline_location_black_48,
        R.color.black,
        listOf(89)
    ),
    Climb(
        R.string.climb,
        R.drawable.icons8_climbing_50,
        R.drawable.icons8_climbing_white_50,
        R.drawable.ic_filled_location_darkbrown_48,
        R.drawable.ic_outline_location_darkbrown_48,
        R.color.brown_400,
        listOf(37)
    ),
    Hike(
        R.string.hike,
        R.drawable.icons8_trekking_50,
        R.drawable.icons8_trekking_white_50,
        R.drawable.ic_filled_location_lightbrown_48,
        R.drawable.ic_outline_location_lightbrown_48,
        R.color.orange_600,
        listOf(3)
    ),
    Running(
        R.string.running,
        R.drawable.icons8_running_50,
        R.drawable.icons8_running_white_50,
        R.drawable.ic_filled_location_lightbrown_48,
        R.drawable.ic_outline_location_lightbrown_48,
        R.color.orange_100,
        listOf(1, 6, 7, 8, 9)
    ),
    Skitour(
        R.string.skitour,
        R.drawable.icon_ski_touring,
        R.drawable.icon_ski_touring_white,
        R.drawable.ic_filled_location_blue_48,
        R.drawable.ic_outline_location_blue_48,
        R.color.blue_400,
        listOf(165, 169, 203)
    ),
    Other(
        R.string.other,
        R.drawable.ic_baseline_accessibility_24_black,
        R.drawable.ic_baseline_accessibility_24_white,
        R.drawable.ic_filled_location_black_48,
        R.drawable.ic_outline_location_black_48,
        R.color.grey_400,
        listOf(4)
    );

    override fun toString(): String {
        return name
    }

    companion object {
        fun getSportTypeFromGarminId(garminId: Int): SportType {
            entries.forEach {
                if (garminId in it.garminTypeIds) {
                    return it
                }
            }
            return Other
        }
    }
}

enum class SportGroup(
    val sportTypes: List<SportType>,
    val sportNameStringId: Int,
    val color: Int
) {
    OnABicycle(
        listOf(
            SportType.Bicycle,
            SportType.Racer,
            SportType.Mountainbike
        ),
        R.string.onBicycle,
        R.color.green_500,
    ),
    OnFoot(
        listOf(SportType.Climb, SportType.Hike, SportType.Skitour, SportType.Running),
        R.string.onFoot,
        R.color.brown_700,
    ),
    Indoor(
        listOf(SportType.IndoorTrainer),
        R.string.indoor,
        R.color.green_800,
    ),
    Other(
        listOf(SportType.BikeAndHike, SportType.IndoorTrainer, SportType.Other),
        R.string.other,
        R.color.grey_500,
    )
}