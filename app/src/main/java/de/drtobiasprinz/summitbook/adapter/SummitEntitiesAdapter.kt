package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.internal.managers.FragmentComponentManager
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.CardSummitEntitiesBinding
import de.drtobiasprinz.summitbook.db.entities.EntityEvent
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SummitEntitySummary
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.dialog.AddEntityEventDialog
import java.util.Locale
import javax.inject.Singleton
import kotlin.math.round


@Singleton
class SummitEntitiesAdapter :
    RecyclerView.Adapter<SummitEntitiesAdapter.ViewHolder>() {
    var summits: List<Summit> = emptyList()
    lateinit var context: Context
    var entityEvents: List<EntityEvent> = emptyList()
    var recyclerViewVisible: Boolean = false
    var onClickUpdate: (SummitEntitySummary, String) -> Unit = { _, _ -> }
    var onClickDeleteEvent: (EntityEvent) -> Unit = { _ -> }
    var drawableIdDefault: Int? = null
    var drawableIdActive: Int? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding =
            CardSummitEntitiesBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(differ.currentList[position], entityEvents)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    inner class ViewHolder(var binding: CardSummitEntitiesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(entity: SummitEntitySummary, entityEvents: List<EntityEvent>) {
            binding.apply {
                val isActive = entity.name in MainActivity.peaks.map { it.name }
                entityNameEdit.setText(entity.name)
                val drawableIdActiveLocal = drawableIdActive
                val drawableIdDefaultLocal = drawableIdDefault
                if (isActive && drawableIdActiveLocal != null) {
                    image.setImageResource(drawableIdActiveLocal)
                } else if (drawableIdDefaultLocal != null) {
                    image.setImageResource(drawableIdDefaultLocal)
                }
                entityName.text = entity.name
                numberActivities.text = String.format(Locale.getDefault(), "# %s", entity.count)
                distance.text = String.format(
                    Locale.getDefault(),
                    "%s %s",
                    round(entity.distance),
                    context.getString(R.string.km)
                )
                heightMeter.text = String.format(
                    Locale.getDefault(),
                    "%s %s",
                    entity.heightMeters,
                    context.getString(R.string.hm)
                )
                addEvent.setOnClickListener {
                    AddEntityEventDialog.getInstance(
                        null, entity
                    ).show(
                        (FragmentComponentManager.findActivity(binding.root.context) as FragmentActivity).supportFragmentManager.beginTransaction(),
                        "Add Event"
                    )
                }
                entryEdit.setOnClickListener {
                    entityName.visibility = View.GONE
                    entityNameEdit.visibility = View.VISIBLE
                    save.visibility = View.VISIBLE
                    cancel.visibility = View.VISIBLE
                }
                save.setOnClickListener {
                    entityName.visibility = View.VISIBLE
                    entityNameEdit.visibility = View.GONE
                    save.visibility = View.GONE
                    cancel.visibility = View.GONE
                    onClickUpdate(
                        entity,
                        entityNameEdit.text.toString()
                    )
                }
                cancel.setOnClickListener {
                    entityName.visibility = View.VISIBLE
                    entityNameEdit.visibility = View.GONE
                    save.visibility = View.GONE
                    cancel.visibility = View.GONE
                }
            }
            val result = entityEvents
                .filter { it.equipmentName == entity.name }
                .sortedByDescending { it.date }
            val entityEventAdapter = EntityEventAdapter(summits, entity)
            entityEventAdapter.differ.submitList(result)
            entityEventAdapter.onClickDeleteEvent = onClickDeleteEvent
            binding.recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = entityEventAdapter
            }
            if (result.isEmpty()) {
                binding.dropDownRecyclerView.visibility = View.GONE
            } else {
                binding.dropDownRecyclerView.visibility = View.VISIBLE
                setDropDown(binding, true)
            }
            binding.dropDownRecyclerView.setOnClickListener {
                setDropDown(binding, recyclerViewVisible)
                if (!recyclerViewVisible) {
                    binding.recyclerView.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.GONE
                }
                recyclerViewVisible = !recyclerViewVisible
            }
        }
    }

    private fun setDropDown(binding: CardSummitEntitiesBinding, showDownDrawable: Boolean) {
        binding.dropDownRecyclerView.setImageResource(
            if (showDownDrawable) R.drawable.baseline_arrow_drop_down_24 else R.drawable.baseline_arrow_drop_up_black_24dp
        )
    }

    private val differCallback = object : DiffUtil.ItemCallback<SummitEntitySummary>() {
        override fun areItemsTheSame(oldItem: SummitEntitySummary, newItem: SummitEntitySummary): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: SummitEntitySummary, newItem: SummitEntitySummary): Boolean {
            return oldItem.name == newItem.name
        }

    }
    val differ = AsyncListDiffer(this, differCallback)

}