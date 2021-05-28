package de.drtobiasprinz.summitbook.adapter

import SummitEntryDataFragment
import SummitEntryImagesFragment
import SummitEntryPowerFragment
import SummitEntryTrackFragment
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.SummitEntry


class TabsPagerAdapter(private val mContext: Context, fm: FragmentManager?, private val summitEntry: SummitEntry) : FragmentPagerAdapter(fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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
        if ((summitEntry.hasGpsTrack() && summitEntry.hasImagePath() && summitEntry.activityData?.power != null && summitEntry.activityData?.power?.hasPowerData() == true && position == 3) ||
                (summitEntry.hasGpsTrack() && !summitEntry.hasImagePath() && summitEntry.activityData?.power != null && summitEntry.activityData?.power?.hasPowerData() == true && position == 2) ||
                (!summitEntry.hasGpsTrack() && summitEntry.hasImagePath() && summitEntry.activityData?.power != null && summitEntry.activityData?.power?.hasPowerData() == true && position == 2) ||
                (!summitEntry.hasGpsTrack() && !summitEntry.hasImagePath() && summitEntry.activityData?.power != null && summitEntry.activityData?.power?.hasPowerData() == true && position == 1)
                ) {
            return SummitEntryPowerFragment.newInstance(summitEntry)
        }
        throw RuntimeException("NO WAY")
    }

    override fun getPageTitle(position: Int): CharSequence {
        val TAB_TITLES = mutableListOf(R.string.tab_text_1)
        if (summitEntry.hasImagePath()) {
            TAB_TITLES.add(R.string.tab_text_2)
        }
        if (summitEntry.hasGpsTrack()) {
            TAB_TITLES.add(R.string.tab_text_3)
        }
        if (summitEntry.activityData?.power != null && summitEntry.activityData?.power?.hasPowerData() == true) {
            TAB_TITLES.add(R.string.tab_text_4)
        }
        return mContext.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        var tabCount = 1
        if (summitEntry.hasImagePath()) {
            tabCount += 1
        }
        if (summitEntry.hasGpsTrack()) {
            tabCount += 1
        }
        if (summitEntry.activityData?.power != null && summitEntry.activityData?.power?.hasPowerData() == true) {
            tabCount += 1
        }
        return tabCount
    }

}