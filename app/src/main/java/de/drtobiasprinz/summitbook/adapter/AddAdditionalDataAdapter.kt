package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.CardAddAdditionalDataBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.dialog.AddAdditionalDataFromExternalResourcesDialog
import kotlin.math.abs


class AddAdditionalDataAdapter(val summit: Summit) :
    RecyclerView.Adapter<AddAdditionalDataAdapter.ViewHolder?>() {

    private lateinit var context: Context

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): ViewHolder {
        context = parent.context
        return ViewHolder(
            CardAddAdditionalDataBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = differ.currentList[position]
        addCard((holder.binding as CardAddAdditionalDataBinding), entry)
    }

    private fun addCard(
        binding: CardAddAdditionalDataBinding,
        entry: AddAdditionalDataFromExternalResourcesDialog.TableEntry
    ) {
        val defaultBackground = binding.entry.background
        binding.entry.text = context.getString(entry.tableEntry.nameId)
        binding.newEntry.text = String.format(
            context.resources.configuration.locales[0],
            if (entry.tableEntry.isInt) "%.0f %s" else "%.1f %s",
            entry.value * entry.tableEntry.scaleFactorView,
            context.getString(entry.tableEntry.unitId)
        )
        binding.newEntry.setOnClickListener {
            entry.isChecked = true
            binding.oldValue.background = defaultBackground
            binding.newEntry.setBackgroundResource(R.color.green_500)
        }

        val currentValue = entry.tableEntry.getValue(summit)

        val defaultValueAsString = if (abs(currentValue) < 0.05 || abs(entry.value - currentValue) < (if (entry.tableEntry.isInt) 0.51 else 0.05)) {
            "-"
        } else {
            String.format(
                context.resources.configuration.locales[0],
                if (entry.tableEntry.isInt) "%.0f %s" else "%.1f %s",
                currentValue * entry.tableEntry.scaleFactorView,
                context.getString(entry.tableEntry.unitId)
            )

        }
        binding.oldValue.text = defaultValueAsString
        if (!entry.isChecked) {
            binding.oldValue.setBackgroundResource(R.color.green_500)
        }
        binding.oldValue.setOnClickListener {
            entry.isChecked = false
            binding.oldValue.setBackgroundResource(R.color.green_500)
            binding.newEntry.background = defaultBackground
        }
    }

    class ViewHolder internal constructor(val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val differCallback =
        object : DiffUtil.ItemCallback<AddAdditionalDataFromExternalResourcesDialog.TableEntry>() {
            override fun areItemsTheSame(
                oldItem: AddAdditionalDataFromExternalResourcesDialog.TableEntry,
                newItem: AddAdditionalDataFromExternalResourcesDialog.TableEntry
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: AddAdditionalDataFromExternalResourcesDialog.TableEntry,
                newItem: AddAdditionalDataFromExternalResourcesDialog.TableEntry
            ): Boolean {
                return oldItem == newItem
            }

        }
    val differ = AsyncListDiffer(this, differCallback)
}