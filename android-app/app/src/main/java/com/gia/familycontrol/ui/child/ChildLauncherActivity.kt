package com.gia.familycontrol.ui.child

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gia.familycontrol.R
import com.gia.familycontrol.util.AppHideManager
import com.gia.familycontrol.util.SecureAuthManager
import java.util.concurrent.Executors

class ChildLauncherActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var adapter: LauncherAppAdapter
    private val executor    = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var allApps     = listOf<AppItem>()   // full list
    private var currentApps = listOf<AppItem>()   // currently shown (filtered)

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            loadAppsAsync()
        }
    }

    data class AppItem(val packageName: String, val label: String, val icon: Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isDefaultHome()) { requestHomeRole(); return }
        initLauncher()
    }

    private fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
        val info = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return info?.activityInfo?.packageName == packageName
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startActivityForResult(
                getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME),
                REQ_HOME
            )
        else
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_HOME) initLauncher()
    }

    private fun initLauncher() {
        setContentView(R.layout.activity_child_launcher)

        etSearch     = findViewById(R.id.etSearch)
        recyclerView = findViewById(R.id.rvLauncherApps)

        adapter = LauncherAppAdapter { item ->
            packageManager.getLaunchIntentForPackage(item.packageName)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                ?.let { startActivity(it) }
            // Clear search after launching
            etSearch.setText("")
            etSearch.clearFocus()
        }

        recyclerView.apply {
            layoutManager = GridLayoutManager(this@ChildLauncherActivity, 4)
            adapter        = this@ChildLauncherActivity.adapter
            setHasFixedSize(false)
            itemAnimator   = null
        }

        // Search filter
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString() ?: "")
            }
        })

        findViewById<View>(R.id.ivParentSettings).setOnClickListener { showParentAuthDialog() }

        loadAppsAsync()
    }

    override fun onResume() {
        super.onResume()
        if (getSharedPreferences("gia_lock", MODE_PRIVATE).getBoolean("is_locked", false)) {
            startActivity(Intent(this, LockScreenActivity::class.java))
            return
        }
        // Register refresh receiver
        registerReceiver(refreshReceiver, IntentFilter("com.gia.familycontrol.REFRESH_LAUNCHER"))
        if (::recyclerView.isInitialized) loadAppsAsync()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(refreshReceiver) } catch (_: Exception) {}
    }

    private fun loadAppsAsync() {
        executor.execute {
            val pm         = packageManager
            val hiddenPkgs = AppHideManager.getHiddenPackages(this)

            val loaded = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                PackageManager.GET_META_DATA
            ).filter {
                val pkg = it.activityInfo.packageName
                pkg != packageName && pkg !in hiddenPkgs
            }.map {
                AppItem(
                    packageName = it.activityInfo.packageName,
                    label       = it.loadLabel(pm).toString(),
                    icon        = it.loadIcon(pm)
                )
            }.sortedBy { it.label.lowercase() }

            mainHandler.post {
                allApps = loaded
                val query = if (::etSearch.isInitialized) etSearch.text.toString() else ""
                filterApps(query)
            }
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) allApps
        else allApps.filter { it.label.contains(query, ignoreCase = true) }

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = currentApps.size
            override fun getNewListSize() = filtered.size
            override fun areItemsTheSame(o: Int, n: Int) =
                currentApps[o].packageName == filtered[n].packageName
            override fun areContentsTheSame(o: Int, n: Int) =
                currentApps[o].label == filtered[n].label
        })

        currentApps = filtered
        adapter.setApps(filtered)
        diff.dispatchUpdatesTo(adapter)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // If searching, clear search on back — don't exit launcher
        if (::etSearch.isInitialized && etSearch.text.isNotEmpty()) {
            etSearch.setText("")
            etSearch.clearFocus()
        }
        // Never exit launcher
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (::recyclerView.isInitialized) {
            etSearch.setText("")
            loadAppsAsync()
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun showParentAuthDialog() {
        val prefs    = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val isPaired = prefs.getLong("device_id", -1L) != -1L
        val hasPin   = SecureAuthManager.hasPin(this)

        when {
            !isPaired || !hasPin -> startActivity(Intent(this, ChildDashboardActivity::class.java))
            else -> {
                val input = android.widget.EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                            android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    hint = "Enter parent PIN"
                }
                AlertDialog.Builder(this)
                    .setTitle("Parent Access")
                    .setView(input)
                    .setPositiveButton("Unlock") { _, _ ->
                        if (SecureAuthManager.verifyPin(this, input.text.toString()))
                            startActivity(Intent(this, ChildDashboardActivity::class.java))
                        else
                            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    companion object { private const val REQ_HOME = 1001 }
}

class LauncherAppAdapter(
    private val onLaunch: (ChildLauncherActivity.AppItem) -> Unit
) : RecyclerView.Adapter<LauncherAppAdapter.VH>() {

    private var apps = listOf<ChildLauncherActivity.AppItem>()

    fun setApps(newApps: List<ChildLauncherActivity.AppItem>) { apps = newApps }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon:  ImageView = view.findViewById(R.id.ivLauncherIcon)
        val label: TextView  = view.findViewById(R.id.tvLauncherLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_launcher_app, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = apps[position]
        holder.icon.setImageDrawable(item.icon)
        holder.label.text = item.label
        holder.itemView.setOnClickListener { onLaunch(item) }
    }

    override fun getItemCount() = apps.size
}
