package com.gia.parentcontrol.ui.dashboard

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.parentcontrol.R
import com.gia.parentcontrol.model.ScheduledLock
import com.gia.parentcontrol.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduleLockActivity : AppCompatActivity() {

    private val api by lazy { RetrofitClient.create(this) }
    private var childDeviceId: Long = -1L
    private val schedules = mutableListOf<ScheduledLock>()
    private lateinit var listView: ListView
    private lateinit var emptyView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childDeviceId = getSharedPreferences("parent_prefs", MODE_PRIVATE).getLong("child_device_id", -1L)

        supportActionBar?.title = "⏰ Scheduled Lock"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Build layout programmatically — no extra XML needed
        val root = android.widget.FrameLayout(this)

        listView = ListView(this).apply {
            divider = null
            setPadding(24, 24, 24, 24)
            clipToPadding = false
        }

        emptyView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 0, 48, 0)
            addView(TextView(context).apply {
                text = "⏰"
                textSize = 52f
                gravity = android.view.Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = "No schedules yet"
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFF202124.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 8)
            })
            addView(TextView(context).apply {
                text = "Tap + to automatically lock the device at bedtime."
                textSize = 13f
                setTextColor(0xFF5F6368.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 32)
            })
        }

        root.addView(listView)
        root.addView(emptyView)
        setContentView(root)

        // FAB
        val fab = com.google.android.material.floatingactionbutton.FloatingActionButton(this).apply {
            setImageResource(android.R.drawable.ic_input_add)
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF5E5CE6.toInt())
            val lp = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                setMargins(0, 0, 40, 40)
            }
            layoutParams = lp
            setOnClickListener { showDialog(null) }
        }
        root.addView(fab)

        listView.adapter = ScheduleAdapter()
        fetchAndLoad()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun fetchAndLoad() {
        // Also refresh child device id
        lifecycleScope.launch {
            try {
                val res = api.getChildDevices()
                if (res.isSuccessful) {
                    res.body()?.firstOrNull()?.let { childDeviceId = it.id }
                }
            } catch (_: Exception) {}
            loadSchedules()
        }
    }

    private fun loadSchedules() {
        lifecycleScope.launch {
            try {
                val res = api.getSchedules()
                if (res.isSuccessful) {
                    schedules.clear()
                    schedules.addAll(res.body() ?: emptyList())
                    (listView.adapter as ScheduleAdapter).notifyDataSetChanged()
                    emptyView.visibility = if (schedules.isEmpty()) View.VISIBLE else View.GONE
                    listView.visibility  = if (schedules.isEmpty()) View.GONE  else View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(this@ScheduleLockActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDialog(existing: ScheduledLock?) {
        if (childDeviceId == -1L) {
            AlertDialog.Builder(this)
                .setTitle("No Device Paired")
                .setMessage("Please pair with a child device first before creating a schedule. Go back to the dashboard and tap QR to pair.")
                .setPositiveButton("Got it", null)
                .show()
            return
        }

        val ctx = this
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        // Label
        layout.addView(TextView(ctx).apply { text = "Label"; textSize = 12f; setTextColor(0xFF5F6368.toInt()) })
        val etLabel = EditText(ctx).apply {
            setText(existing?.label ?: "Bedtime")
            textSize = 14f
            setPadding(0, 8, 0, 16)
        }
        layout.addView(etLabel)

        // Lock time
        var lockTime = existing?.lockTime ?: "20:00"
        var unlockTime = existing?.unlockTime ?: "06:00"

        layout.addView(TextView(ctx).apply { text = "🔒 Lock Time"; textSize = 12f; setTextColor(0xFF5F6368.toInt()) })
        val tvLock = TextView(ctx).apply {
            text = fmt12(lockTime)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFFC5221F.toInt())
            setPadding(0, 4, 0, 4)
        }
        layout.addView(tvLock)
        val btnPickLock = com.google.android.material.button.MaterialButton(ctx).apply {
            text = "Pick Lock Time"
            isAllCaps = false
            textSize = 13f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 16
            layoutParams = lp
            setOnClickListener {
                val c = Calendar.getInstance()
                TimePickerDialog(ctx, { _, h, m ->
                    lockTime = "%02d:%02d".format(h, m)
                    tvLock.text = fmt12(lockTime)
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
            }
        }
        layout.addView(btnPickLock)

        // Unlock time
        layout.addView(TextView(ctx).apply { text = "🔓 Unlock Time"; textSize = 12f; setTextColor(0xFF5F6368.toInt()) })
        val tvUnlock = TextView(ctx).apply {
            text = fmt12(unlockTime)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF137333.toInt())
            setPadding(0, 4, 0, 4)
        }
        layout.addView(tvUnlock)
        val btnPickUnlock = com.google.android.material.button.MaterialButton(ctx).apply {
            text = "Pick Unlock Time"
            isAllCaps = false
            textSize = 13f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 16
            layoutParams = lp
            setOnClickListener {
                val c = Calendar.getInstance()
                TimePickerDialog(ctx, { _, h, m ->
                    unlockTime = "%02d:%02d".format(h, m)
                    tvUnlock.text = fmt12(unlockTime)
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
            }
        }
        layout.addView(btnPickUnlock)

        // Days
        layout.addView(TextView(ctx).apply { text = "Days (empty = every day)"; textSize = 12f; setTextColor(0xFF5F6368.toInt()); setPadding(0,0,0,8) })
        val dayKeys = listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
        val dayLabels = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
        val selectedDays = (existing?.days?.split(",")?.filter { it.isNotBlank() } ?: emptyList()).toMutableList()
        val dayRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val toggles = dayKeys.mapIndexed { i, key ->
            ToggleButton(ctx).apply {
                textOn = dayLabels[i]; textOff = dayLabels[i]
                textSize = 10f; isChecked = selectedDays.contains(key)
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = lp
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedDays.add(key) else selectedDays.remove(key)
                }
            }
        }
        toggles.forEach { dayRow.addView(it) }
        layout.addView(dayRow)

        // Active switch
        val switchRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 0)
        }
        switchRow.addView(TextView(ctx).apply {
            text = "Active"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val sw = Switch(ctx).apply { isChecked = existing?.isActive ?: true }
        switchRow.addView(sw)
        layout.addView(switchRow)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "New Schedule" else "Edit Schedule")
            .setView(layout)
            .setPositiveButton(if (existing == null) "Create" else "Save") { _, _ ->
                val body = ScheduledLock(
                    id = existing?.id ?: 0,
                    deviceId = if (childDeviceId != -1L) childDeviceId else 0,
                    label = etLabel.text.toString().ifBlank { "Bedtime" },
                    lockTime = lockTime,
                    unlockTime = unlockTime,
                    days = selectedDays.joinToString(","),
                    isActive = sw.isChecked
                )
                lifecycleScope.launch {
                    try {
                        if (existing == null) api.createSchedule(body)
                        else api.updateSchedule(body.id, body)
                        Toast.makeText(this@ScheduleLockActivity, "✅ Saved", Toast.LENGTH_SHORT).show()
                        loadSchedules()
                    } catch (e: Exception) {
                        Toast.makeText(this@ScheduleLockActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fmt12(t: String): String {
        val parts = t.split(":")
        val h = parts[0].toInt(); val m = parts[1].toInt()
        return "%d:%02d %s".format(if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")
    }

    inner class ScheduleAdapter : BaseAdapter() {
        override fun getCount() = schedules.size
        override fun getItem(pos: Int) = schedules[pos]
        override fun getItemId(pos: Int) = schedules[pos].id

        override fun getView(pos: Int, convertView: View?, parent: android.view.ViewGroup): View {
            val sc = schedules[pos]
            val card = com.google.android.material.card.MaterialCardView(this@ScheduleLockActivity).apply {
                radius = 28f
                cardElevation = 0f
                strokeWidth = 2
                strokeColor = 0xFFE0E0E0.toInt()
                setCardBackgroundColor(0xFFFFFFFF.toInt())
                val lp = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 20) }
                layoutParams = lp
            }

            val row = LinearLayout(this@ScheduleLockActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(32, 28, 24, 28)
            }

            val info = LinearLayout(this@ScheduleLockActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            info.addView(TextView(this@ScheduleLockActivity).apply {
                text = sc.label
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFF202124.toInt())
                setPadding(0, 0, 0, 6)
            })
            info.addView(TextView(this@ScheduleLockActivity).apply {
                text = "🔒 ${fmt12(sc.lockTime)}  →  🔓 ${fmt12(sc.unlockTime)}"
                textSize = 14f
                setTextColor(0xFF202124.toInt())
                setPadding(0, 0, 0, 4)
            })
            info.addView(TextView(this@ScheduleLockActivity).apply {
                text = if (sc.days.isBlank()) "Every day"
                       else sc.days.split(",").filter { it.isNotBlank() }.joinToString(" · ")
                textSize = 12f
                setTextColor(0xFF80868B.toInt())
            })

            val statusBadge = TextView(this@ScheduleLockActivity).apply {
                text = if (sc.isActive) "ON" else "OFF"
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (sc.isActive) 0xFF137333.toInt() else 0xFF80868B.toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 999f
                    setColor(if (sc.isActive) 0xFFE6F4EA.toInt() else 0xFFF1F3F4.toInt())
                }
                setPadding(20, 8, 20, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(12, 0, 8, 0) }
            }

            val btnEdit = ImageButton(this@ScheduleLockActivity).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                background = null
                setColorFilter(0xFF5F6368.toInt())
                setOnClickListener { showDialog(sc) }
            }
            val btnDel = ImageButton(this@ScheduleLockActivity).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                background = null
                setColorFilter(0xFFC5221F.toInt())
                setOnClickListener {
                    AlertDialog.Builder(this@ScheduleLockActivity)
                        .setTitle("Delete?")
                        .setMessage("Delete \"${sc.label}\"?")
                        .setPositiveButton("Delete") { _, _ ->
                            lifecycleScope.launch {
                                try { api.deleteSchedule(sc.id); loadSchedules() } catch (_: Exception) {}
                            }
                        }
                        .setNegativeButton("Cancel", null).show()
                }
            }

            row.addView(info)
            row.addView(statusBadge)
            row.addView(btnEdit)
            row.addView(btnDel)
            card.addView(row)
            return card
        }
    }
}
