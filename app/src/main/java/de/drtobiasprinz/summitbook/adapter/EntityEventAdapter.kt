package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.CardEntityEventBinding
import de.drtobiasprinz.summitbook.db.entities.EntityEvent
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SummitEntitySummary
import de.drtobiasprinz.summitbook.ui.dialog.AddEntityEventDialog
import de.drtobiasprinz.summitbook.utils.findActivity
import java.text.NumberFormat
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class EntityEventAdapter(
    private val summits: List<Summit>,
    private val summitEntitySummary: SummitEntitySummary
) :
    RecyclerView.Adapter<EntityEventAdapter.ViewHolder?>() {

    private lateinit var context: Context
    var onClickDeleteEvent: (EntityEvent) -> Unit = { _ -> }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): ViewHolder {
        context = parent.context
        return ViewHolder(
            CardEntityEventBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = differ.currentList[position]
        addCard((holder.binding as CardEntityEventBinding), entry)
    }

    private fun addCard(
        binding: CardEntityEventBinding,
        event: EntityEvent
    ) {
        val relevantSummits = summits.filter {
            it.date > event.date && event.equipmentName in summitEntitySummary.type.getRelevantValueFromSummit(
                it
            )
        }
        val numberFormat = NumberFormat.getInstance(context.resources.configuration.locales[0])

        binding.date.text = event.getDateAsString()
        binding.description.text = event.description
        binding.km.text = String.format(
            context.getString(R.string.value_with_km),
            numberFormat.format(relevantSummits.sumOf { it.kilometers }.roundToInt())
        )
        binding.hm.text = String.format(
            context.getString(R.string.value_with_hm),
            numberFormat.format(relevantSummits.sumOf { it.elevationData.elevationGain })
        )
        binding.time.text = String.format(
            context.getString(R.string.value_with_h),
            numberFormat.format(
                TimeUnit.SECONDS.toHours(
                    relevantSummits.sumOf { it.duration }.toLong()
                )
            )
        )
        binding.entryEdit.setOnClickListener { view: View? ->
            view?.context?.findActivity()?.supportFragmentManager?.beginTransaction()?.let { transaction ->
                AddEntityEventDialog.getInstance(event, summitEntitySummary).show(transaction, "Update entity event")
            }
        }
        binding.entryDelete.setOnClickListener {
            onClickDeleteEvent(event)
        }
    }

    class ViewHolder internal constructor(val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val differCallback =
        object : DiffUtil.ItemCallback<EntityEvent>() {
            override fun areItemsTheSame(
                oldItem: EntityEvent,
                newItem: EntityEvent
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: EntityEvent,
                newItem: EntityEvent
            ): Boolean {
                return oldItem == newItem
            }

        }
    val differ = AsyncListDiffer(this, differCallback)
}