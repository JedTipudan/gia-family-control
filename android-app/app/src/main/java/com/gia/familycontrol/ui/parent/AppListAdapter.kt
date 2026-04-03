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
        val pm = holder.binding.root.context.packageManager
        
        try {
            val appIcon = pm.getApplicationIcon(app.packageName)
            holder.binding.ivAppIcon.setImageDrawable(appIcon)
        } catch (e: Exception) {
            holder.binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        
        holder.binding.tvAppName.text = app.appName
        holder.binding.tvPackageName.text = app.packageName
        
        // Remove listener before setting checked state to prevent triggering callback
        holder.binding.switchBlock.setOnCheckedChangeListener(null)
        holder.binding.switchBlock.isChecked = app.isBlocked
        
        // Now set the listener
        holder.binding.switchBlock.setOnCheckedChangeListener { _, checked ->
            if (checked != app.isBlocked) {
                onToggle(app, checked)
            }
        }
    }

    override fun getItemCount() = apps.size
}
