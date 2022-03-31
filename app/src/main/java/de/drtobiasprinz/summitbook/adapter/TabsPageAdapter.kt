package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.fragments.SummitEntryDataFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryImagesFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryPowerFragment
import de.drtobiasprinz.summitbook.fragments.SummitEntryTrackFragment
import de.drtobiasprinz.summitbook.models.Summit


class TabsPagerAdapter(private val mContext: Context, fm: FragmentManager?, private val summitEntry: Summit) : FragmentPagerAdapter(fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        if (position == 0) {
            return SummitEntryDataFragment.newInstance(summitEntry)
        }
        if (summitEntry.hasImagePath() && position == 1) {
            return SummitEntryImagesFragment.newInstance(summitEntry)
        }
        if ((summitEntry.hasGpsTrack() && summitEntry.hasImagePath() && position == 2) || (summitEntry.hasGpsTrack() && !summitEntry.hasImagePath() && position == 1)) {
            return SummitEntryTrackFragment.newInstance(summitEntry)
        }
        if ((summitEntry.hasGpsTrack() && summitEntry.hasImagePath() && summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true && position == 3) ||
                (summitEntry.hasGpsTrack() && !summitEntry.hasImagePath() && summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true && position == 2) ||
                (!summitEntry.hasGpsTrack() && summitEntry.hasImagePath() && summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true && position == 2) ||
                (!summitEntry.hasGpsTrack() && !summitEntry.hasImagePath() && summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true && position == 1)
                ) {
            return SummitEntryPowerFragment.newInstance(summitEntry)
        }
        throw RuntimeException("NO WAY")
    }

    override fun getPageTitle(position: Int): CharSequence {
        val tabTitles = mutableListOf(R.string.tab_text_1)
        if (summitEntry.hasImagePath()) {
            tabTitles.add(R.string.tab_text_2)
        }
        if (summitEntry.hasGpsTrack()) {
            tabTitles.add(R.string.tab_text_3)
        }
        if (summitEntry.garminData?.power != null && summitEntry.garminData?.power?.hasPowerData() == true) {
            tabTitles.add(R.string.tab_text_4)
        }
        return mContext.resources.getString(tabTitles[position])
    }

    override fun getCount(): Int {
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