package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.CardSummitEntitiesBinding
import de.drtobiasprinz.summitbook.models.SummitEntities
import java.util.Locale
import javax.inject.Singleton
import kotlin.math.round


@Singleton
class SummitEntitiesAdapter :
    RecyclerView.Adapter<SummitEntitiesAdapter.ViewHolder>() {

    lateinit var context: Context
    var onClickUpdate: (SummitEntities, String) -> Unit = { _, _ -> }


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
        holder.setData(differ.currentList[position])
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    inner class ViewHolder(var binding: CardSummitEntitiesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(entity: SummitEntities) {
            binding.apply {
                entityNameEdit.setText(entity.name)
                entityName.text = entity.name
                tourDate.text = String.format(Locale.getDefault(), "# %s", entity.count)
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
                    onClickUpdate(entity, entityNameEdit.text.toString())
                }
                cancel.setOnClickListener {
                    entityName.visibility = View.VISIBLE
                    entityNameEdit.visibility = View.GONE
                    save.visibility = View.GONE
                    cancel.visibility = View.GONE
                }
            }
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<SummitEntities>() {
        override fun areItemsTheSame(oldItem: SummitEntities, newItem: SummitEntities): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: SummitEntities, newItem: SummitEntities): Boolean {
            return oldItem.name == newItem.name
        }

    }
    val differ = AsyncListDiffer(this, differCallback)

}