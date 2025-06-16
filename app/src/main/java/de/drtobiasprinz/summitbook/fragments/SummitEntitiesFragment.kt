package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SummitEntityPageViewerAdapter
import de.drtobiasprinz.summitbook.databinding.FragementAdditonalDataBinding
import de.drtobiasprinz.summitbook.viewmodel.PageViewModel

@AndroidEntryPoint
class SummitEntitiesFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private lateinit var binding: FragementAdditonalDataBinding

    private lateinit var viewPager: ViewPager2
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragementAdditonalDataBinding.inflate(layoutInflater, container, false)
        pageViewModel = ViewModelProvider(this)[PageViewModel::class.java]
        val tabsPagerAdapter = SummitEntityPageViewerAdapter(activity)
        viewPager = binding.pager
        viewPager.adapter = tabsPagerAdapter
        val tabs = binding.tabs
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = getPageTitle(position)
        }.attach()
        return binding.root
    }

    private fun getPageTitle(position: Int): CharSequence {
        val tabTitles = mutableListOf(
            R.string.participants,
            R.string.place_hint,
            R.string.country_hint,
            R.string.equipments
        )
        return resources.getString(tabTitles[position])
    }


}