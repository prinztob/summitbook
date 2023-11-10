package de.drtobiasprinz.summitbook.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.SummitEntryDataFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryImagesFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryPowerFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryThirdPartyFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryTrackFragment


class TabsPagerAdapter(fa: FragmentActivity?, private val summitEntry: Summit) :
    FragmentStateAdapter(fa!!) {

    override fun createFragment(position: Int): Fragment {
        if (position == getPositionForSummitEntryDataFragment()) {
            return SummitEntryDataFragment()
        }
        if (summitEntry.garminData != null && position == getPositionForSummitEntryThirdPartyFragment()) {
            return SummitEntryThirdPartyFragment()
        }
        if (summitEntry.hasImagePath() && position == getPositionForSummitEntryImagesFragment()) {
            return SummitEntryImagesFragment()
        }
        if (summitEntry.hasGpsTrack() && position == getPositionForSummitEntryTrackFragment()) {
            return SummitEntryTrackFragment()
        }
        if (summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true && position == getPositionForSummitEntryPowerFragment()) {
            return SummitEntryPowerFragment()
        }
        throw RuntimeException("NO WAY")
    }


    override fun getItemCount(): Int {
        var tabCount = 1
        if (summitEntry.garminData != null) {
            tabCount += 1
        }
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

    private fun getPositionForSummitEntryDataFragment(): Int {
        return 0
    }

    private fun getPositionForSummitEntryThirdPartyFragment(): Int {
        return if (summitEntry.garminData != null) {
            getPositionForSummitEntryDataFragment() + 1
        } else {
            getPositionForSummitEntryDataFragment()
        }
    }

    private fun getPositionForSummitEntryImagesFragment(): Int {
        return if (summitEntry.hasImagePath()) {
            getPositionForSummitEntryThirdPartyFragment() + 1
        } else {
            getPositionForSummitEntryThirdPartyFragment()
        }
    }

    private fun getPositionForSummitEntryTrackFragment(): Int {
        return if (summitEntry.hasGpsTrack()) {
            getPositionForSummitEntryImagesFragment() + 1
        } else {
            getPositionForSummitEntryImagesFragment()
        }
    }

    private fun getPositionForSummitEntryPowerFragment(): Int {
        return if (summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true) {
            getPositionForSummitEntryTrackFragment() + 1
        } else {
            getPositionForSummitEntryTrackFragment()
        }
    }
}