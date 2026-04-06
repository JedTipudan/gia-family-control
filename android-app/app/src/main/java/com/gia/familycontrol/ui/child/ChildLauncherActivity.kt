package com.gia.familycontrol.ui.child

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
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
import com.gia.familycontrol.util.AppHideManager
import com.gia.familycontrol.util.SecureAuthManager

class ChildLauncherActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First time: ask to become default home app
        if (!isDefaultHome()) {
            requestHomeRole()
            return
        }

        showLauncher()
    }

    private fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            return rm.isRoleHeld(RoleManager.ROLE_HOME)
        }
        val info = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return info?.activityInfo?.packageName == packageName
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            startActivityForResult(
                rm.createRequestRoleIntent(RoleManager.ROLE_HOME),
                REQ_HOME
            )
        } else {
            // Android 9 and below — show chooser via home intent
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
            // After user picks, onResume will call showLauncher
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_HOME) showLauncher()
    }

    override fun onResume() {
        super.onResume()
        // Check lock
        if (getSharedPreferences("gia_lock", MODE_PRIVATE).getBoolean("is_locked", false)) {
            startActivity(Intent(this, LockScreenActivity::class.java))
            return
        }
        if (::recyclerView.isInitialized) loadAllowedApps()
    }

    private fun showLauncher() {
        setContentView(R.layout.activity_child_launcher)
        recyclerView = findViewById(R.id.rvLauncherApps)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        loadAllowedApps()
        findViewById<ImageView>(R.id.ivParentSettings)
            .setOnClickListener { showParentAuthDialog() }
    }

    private fun loadAllowedApps() {
        val pm          = packageManager
        val hiddenPkgs  = AppHideManager.getHiddenPackages(this)
        val blockedPkgs = getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
            .getStringSet("blocked", emptySet()) ?: emptySet()

        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).filter { it.activityInfo.packageName != packageName
                && it.activityInfo.packageName !in hiddenPkgs
                && it.activityInfo.packageName !in blockedPkgs
        }.sortedBy { it.loadLabel(pm).toString().lowercase() }

        recyclerView.adapter = LauncherAppAdapter(apps, pm) { info ->
            pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                ?.let { startActivity(it) }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* Block — child cannot exit */ }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (::recyclerView.isInitialized) loadAllowedApps()
    }

    private fun showParentAuthDialog() {
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val isPaired = prefs.getLong("device_id", -1L) != -1L
        val hasPin  = SecureAuthManager.hasPin(this)

        // Not paired yet — go straight to dashboard to enter pair code
        if (!isPaired) {
            startActivity(Intent(this, ChildDashboardActivity::class.java))
            return
        }

        // Paired but no PIN set yet — still allow access
        if (!hasPin) {
            startActivity(Intent(this, ChildDashboardActivity::class.java))
            return
        }

        // Paired + PIN set — require PIN
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

    companion object {
        private const val REQ_HOME = 1001
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
