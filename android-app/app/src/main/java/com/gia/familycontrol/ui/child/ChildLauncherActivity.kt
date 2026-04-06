package com.gia.familycontrol.ui.child

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gia.familycontrol.R
import com.gia.familycontrol.auth.LoginActivity
import com.gia.familycontrol.util.AppHideManager
import com.gia.familycontrol.util.SecureAuthManager

class ChildLauncherActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Not logged in yet — send to login, stay in back stack so
        // pressing Home after login returns here (the launcher)
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        if (prefs.getString("jwt_token", null) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        setContentView(R.layout.activity_child_launcher)
        recyclerView = findViewById(R.id.rvLauncherApps)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        loadAllowedApps()

        findViewById<ImageView>(R.id.ivParentSettings)
            .setOnClickListener { showParentAuthDialog() }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        if (prefs.getString("jwt_token", null) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        // Check lock state
        if (getSharedPreferences("gia_lock", MODE_PRIVATE).getBoolean("is_locked", false)) {
            startActivity(Intent(this, LockScreenActivity::class.java))
            return
        }
        loadAllowedApps()
    }

    private fun loadAllowedApps() {
        val pm = packageManager
        val hiddenPkgs = AppHideManager.getHiddenPackages(this)
        val blockedPkgs = getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
            .getStringSet("blocked", emptySet()) ?: emptySet()

        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps: List<ResolveInfo> = pm.queryIntentActivities(launchIntent, 0)

        val allowed = allApps.filter { info ->
            val pkg = info.activityInfo.packageName
            pkg != packageName && pkg !in hiddenPkgs && pkg !in blockedPkgs
        }.sortedBy { it.loadLabel(pm).toString().lowercase() }

        recyclerView.adapter = LauncherAppAdapter(allowed, pm) { info ->
            val launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName) ?: return@LauncherAppAdapter
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* Block — child cannot exit launcher */ }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        loadAllowedApps()
    }

    private fun showParentAuthDialog() {
        if (!SecureAuthManager.hasPin(this)) {
            Toast.makeText(this, "No PIN set. Contact parent.", Toast.LENGTH_SHORT).show()
            return
        }
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter parent PIN"
        }
        AlertDialog.Builder(this)
            .setTitle("Parent Access")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                if (SecureAuthManager.verifyPin(this, input.text.toString())) {
                    startActivity(Intent(this, ChildDashboardActivity::class.java))
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class LauncherAppAdapter(
    private val apps: List<ResolveInfo>,
    private val pm: PackageManager,
    private val onLaunch: (ResolveInfo) -> Unit
) : RecyclerView.Adapter<LauncherAppAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivLauncherIcon)
        val label: TextView = view.findViewById(R.id.tvLauncherLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_launcher_app, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val info = apps[position]
        holder.icon.setImageDrawable(info.loadIcon(pm))
        holder.label.text = info.loadLabel(pm).toString()
        holder.itemView.setOnClickListener { onLaunch(info) }
    }

    override fun getItemCount() = apps.size
}
