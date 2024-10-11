package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.CardNewSummitsBinding
import de.drtobiasprinz.summitbook.db.entities.IgnoredActivity
import de.drtobiasprinz.summitbook.db.entities.Summit


class AddNewSummitsAdapter :
    RecyclerView.Adapter<AddNewSummitsAdapter.ViewHolder?>() {

    lateinit var ignoredActivities: List<IgnoredActivity>
    private lateinit var context: Context

    var updateButtons: () -> Unit = { }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): ViewHolder {
        context = parent.context
        return ViewHolder(
            CardNewSummitsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = differ.currentList[position]
        addCard((holder.binding as CardNewSummitsBinding), entry)
    }

    private fun addCard(binding: CardNewSummitsBinding, summit: Summit) {
        val date = (summit.getDateAsString()?: "").replace("-", "<br />")
        val dateString =
            "<a href=\"${summit.garminData?.url ?: "unknown"}\">${date}</a>"
        binding.date.isClickable = true
        binding.date.movementMethod = LinkMovementMethod.getInstance()
        binding.date.text = Html.fromHtml(dateString, Html.FROM_HTML_MODE_COMPACT)

        if (summit.garminData?.activityId in ignoredActivities.map { it.activityId }) {
            binding.sportType.text = Html.fromHtml(
                "${context.getString(summit.sportType.sportNameStringId)} <br>(${
                    context.getString(R.string.ignored)
                })", Html.FROM_HTML_MODE_COMPACT
            )
        } else {
            binding.sportType.text = context.getString(summit.sportType.sportNameStringId)
        }
        val kmString = String.format(
            context.resources.configuration.locales[0], "%.1f", summit.kilometers,
            context.getString(R.string.km)
        )
        binding.km.text = kmString

        val hmString = String.format(
            context.resources.configuration.locales[0],
            "%s %s",
            summit.elevationData.elevationGain,
            context.getString(R.string.hm)
        )
        binding.hm.text = hmString

        val velocityString = String.format(
            context.resources.configuration.locales[0],
            "%.1f %s",
            summit.getAverageVelocity(),
            context.getString(R.string.kmh)
        )
        binding.velocity.text = velocityString

        val vo2MaxString = String.format(
            context.resources.configuration.locales[0], "%.1f", summit.garminData?.vo2max
        )
        binding.vo2Max.text = vo2MaxString

        binding.checkBox.setOnCheckedChangeListener { _, selected ->
            summit.isSelected = selected
            updateButtons()
        }


    }

    class ViewHolder internal constructor(val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val differCallback = object : DiffUtil.ItemCallback<Summit>() {
        override fun areItemsTheSame(oldItem: Summit, newItem: Summit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Summit, newItem: Summit): Boolean {
            return oldItem == newItem
        }

    }
    val differ = AsyncListDiffer(this, differCallback)
}