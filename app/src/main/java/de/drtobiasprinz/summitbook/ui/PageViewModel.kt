package de.drtobiasprinz.summitbook.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel


class PageViewModel : ViewModel() {
    private val mTitle = MutableLiveData<String>()
    val text = Transformations.map(mTitle) { input -> "Contact not available in $input" }

    fun setIndex(index: String) {
        mTitle.value = index
    }
}