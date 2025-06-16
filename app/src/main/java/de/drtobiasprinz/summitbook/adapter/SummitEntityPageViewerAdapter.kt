package de.drtobiasprinz.summitbook.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import de.drtobiasprinz.summitbook.fragments.SummitEntitiesViewFragment
import de.drtobiasprinz.summitbook.models.SummitEntityType


class SummitEntityPageViewerAdapter(fa: FragmentActivity?) :
    FragmentStateAdapter(fa!!) {

    override fun createFragment(position: Int): Fragment {
        val fragment = SummitEntitiesViewFragment()
        when (position) {
            0 -> {
                fragment.usedSummitEntityType = SummitEntityType.PARTICIPANTS
                return fragment
            }

            1 -> {
                fragment.usedSummitEntityType = SummitEntityType.PLACES_VISITED
                return fragment
            }

            2 -> {
                fragment.usedSummitEntityType = SummitEntityType.COUNTRIES
                return fragment
            }

            3 -> {
                fragment.usedSummitEntityType = SummitEntityType.EQUIPMENTS
                return fragment
            }
        }
        throw RuntimeException("NO WAY")
    }


    override fun getItemCount(): Int {
        return 4
    }

}