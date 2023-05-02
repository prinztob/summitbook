package de.drtobiasprinz.summitbook.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.SummitEntryDataFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryImagesFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryPowerFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryTrackFragment


class TabsPagerAdapter(fa: FragmentActivity?, private val summitEntry: Summit) : FragmentStateAdapter(fa!!) {

    override fun createFragment(position: Int): Fragment {
        if (position == 0) {
            return SummitEntryDataFragment()
        }
        if (summitEntry.hasImagePath() && position == 1) {
            return SummitEntryImagesFragment()
        }
        if ((summitEntry.hasGpsTrack() && summitEntry.hasImagePath() && position == 2) || (summitEntry.hasGpsTrack() && !summitEntry.hasImagePath() && position == 1)) {
            return SummitEntryTrackFragment()
        }
        if ((summitEntry.hasGpsTrack() && summitEntry.hasImagePath() && summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true && position == 3) ||
                (summitEntry.hasGpsTrack() && !summitEntry.hasImagePath() && summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true && position == 2) ||
                (!summitEntry.hasGpsTrack() && summitEntry.hasImagePath() && summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true && position == 2) ||
                (!summitEntry.hasGpsTrack() && !summitEntry.hasImagePath() && summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true && position == 1)
        ) {
            return SummitEntryPowerFragment()
        }
        throw RuntimeException("NO WAY")
    }


    override fun getItemCount(): Int {
        var tabCount = 1
        if (summitEntry.hasImagePath()) {
            tabCount += 1
        }
        if (summitEntry.hasGpsTrack()) {
            tabCount += 1
        }
        if (summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true) {
            tabCount += 1
        }
        return tabCount
    }

}