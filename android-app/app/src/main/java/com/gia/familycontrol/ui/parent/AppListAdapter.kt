package com.gia.familycontrol.ui.parent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gia.familycontrol.databinding.ItemAppBinding
import com.gia.familycontrol.model.InstalledApp

class AppListAdapter(
    private val apps: List<InstalledApp>,
    private val onToggleBlock: (InstalledApp, Boolean) -> Unit,
    private val onToggleHide: (InstalledApp, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        val pm = holder.binding.root.context.packageManager

        try {
            holder.binding.ivAppIcon.setImageDrawable(pm.getApplicationIcon(app.packageName))
        } catch (e: Exception) {
            holder.binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        holder.binding.tvAppName.text = app.appName
        holder.binding.tvPackageName.text = app.packageName

        // Block switch
        holder.binding.switchBlock.setOnCheckedChangeListener(null)
        holder.binding.switchBlock.isChecked = app.isBlocked
        holder.binding.switchBlock.setOnCheckedChangeListener { _, checked ->
            if (checked != app.isBlocked) onToggleBlock(app, checked)
        }

        // Hide switch — only shown if view exists in layout
        try {
            val switchHide = holder.binding.root.findViewById<android.widget.Switch>(
                holder.binding.root.resources.getIdentifier("switchHide", "id", holder.binding.root.context.packageName)
            )
            switchHide?.setOnCheckedChangeListener(null)
            switchHide?.isChecked = app.isHidden
            switchHide?.setOnCheckedChangeListener { _, checked ->
                if (checked != app.isHidden) onToggleHide(app, checked)
            }
        } catch (_: Exception) {}
    }

    override fun getItemCount() = apps.size
}
