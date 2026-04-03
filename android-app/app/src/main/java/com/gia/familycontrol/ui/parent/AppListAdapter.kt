package com.gia.familycontrol.ui.parent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gia.familycontrol.databinding.ItemAppBinding
import com.gia.familycontrol.model.InstalledApp

class AppListAdapter(
    private val apps: List<InstalledApp>,
    private val onToggle: (InstalledApp, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvAppName.text = app.appName
        holder.binding.tvPackageName.text = app.packageName
        holder.binding.switchBlock.isChecked = app.isBlocked
        holder.binding.switchBlock.setOnCheckedChangeListener { _, checked ->
            onToggle(app, checked)
        }
    }

    override fun getItemCount() = apps.size
}
