package com.gia.parentcontrol.ui.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gia.parentcontrol.databinding.ItemManagedAppBinding
import com.gia.parentcontrol.model.ManagedApp

class AppListAdapter(
    private val apps: List<ManagedApp>,
    private val onToggleBlock: (ManagedApp, Boolean) -> Unit,
    private val onToggleHide: (ManagedApp, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    inner class VH(val binding: ItemManagedAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemManagedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        val pm = holder.binding.root.context.packageManager

        try {
            holder.binding.ivAppIcon.setImageDrawable(pm.getApplicationIcon(app.packageName))
        } catch (_: Exception) {
            holder.binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        holder.binding.tvAppName.text = app.appName
        holder.binding.tvPackageName.text = app.packageName

        // Block switch — remove listener first to avoid triggering on bind
        holder.binding.switchBlock.setOnCheckedChangeListener(null)
        holder.binding.switchBlock.isChecked = app.isBlocked
        holder.binding.switchBlock.setOnCheckedChangeListener { _, checked ->
            if (checked != app.isBlocked) onToggleBlock(app, checked)
        }

        // Hide switch
        holder.binding.switchHide.setOnCheckedChangeListener(null)
        holder.binding.switchHide.isChecked = app.isHidden
        holder.binding.switchHide.setOnCheckedChangeListener { _, checked ->
            if (checked != app.isHidden) onToggleHide(app, checked)
        }
    }

    override fun getItemCount() = apps.size
}
