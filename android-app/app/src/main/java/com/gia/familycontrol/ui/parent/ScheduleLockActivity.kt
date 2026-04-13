package com.gia.familycontrol.ui.parent

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.R
import com.gia.familycontrol.model.ScheduledLock
import com.gia.familycontrol.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduleLockActivity : AppCompatActivity() {

    private val api by lazy { RetrofitClient.create(this) }
    private var childDeviceId: Long = -1L
    private val schedules = mutableListOf<ScheduledLock>()
    private lateinit var listView: ListView
    private lateinit var adapter: ScheduleAdapter
    private lateinit var emptyView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_lock)

        childDeviceId = getSharedPreferences("gia_prefs", MODE_PRIVATE).getLong("child_device_id", -1L)

        supportActionBar?.title = "Scheduled Lock"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        listView  = findViewById(R.id.lvSchedules)
        emptyView = findViewById(R.id.emptyView)
        adapter   = ScheduleAdapter()
        listView.adapter = adapter

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
            .setOnClickListener { showDialog(null) }

        // Always try to fetch latest child device from API
        lifecycleScope.launch {
            try {
                val res = api.getChildDevices()
                if (res.isSuccessful) {
                    val id = res.body()?.firstOrNull()?.id
                    if (id != null) {
                        childDeviceId = id
                        getSharedPreferences("gia_prefs", MODE_PRIVATE)
                            .edit().putLong("child_device_id", id).apply()
                    }
                }
            } catch (_: Exception) {}
            loadSchedules()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadSchedules() {
        lifecycleScope.launch {
            try {
                val res = api.getSchedules()
                if (res.isSuccessful) {
                    schedules.clear()
                    schedules.addAll(res.body() ?: emptyList())
                    adapter.notifyDataSetChanged()
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
            Toast.makeText(this, "⚠️ No child device paired yet. Pair a device first so the schedule can target it.", Toast.LENGTH_LONG).show()
            // Still allow opening dialog — deviceId will be set once paired
        }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_lock, null)
        val etLabel      = view.findViewById<EditText>(R.id.etLabel)
        val tvLockTime   = view.findViewById<TextView>(R.id.tvLockTime)
        val tvUnlockTime = view.findViewById<TextView>(R.id.tvUnlockTime)
        val btnLockTime  = view.findViewById<Button>(R.id.btnPickLock)
        val btnUnlockTime= view.findViewById<Button>(R.id.btnPickUnlock)
        val switchActive = view.findViewById<Switch>(R.id.switchActive)
        val dayButtons   = listOf<ToggleButton>(
            view.findViewById(R.id.tbMon), view.findViewById(R.id.tbTue),
            view.findViewById(R.id.tbWed), view.findViewById(R.id.tbThu),
            view.findViewById(R.id.tbFri), view.findViewById(R.id.tbSat),
            view.findViewById(R.id.tbSun)
        )
        val dayKeys = listOf("MON","TUE","WED","THU","FRI","SAT","SUN")

        var lockTime   = existing?.lockTime   ?: "20:00"
        var unlockTime = existing?.unlockTime ?: "06:00"

        etLabel.setText(existing?.label ?: "Bedtime")
        tvLockTime.text   = fmt12(lockTime)
        tvUnlockTime.text = fmt12(unlockTime)
        switchActive.isChecked = existing?.isActive ?: true

        // Pre-select days
        val selectedDays = existing?.days?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        dayKeys.forEachIndexed { i, key -> dayButtons[i].isChecked = selectedDays.contains(key) }

        btnLockTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                lockTime = "%02d:%02d".format(h, m)
                tvLockTime.text = fmt12(lockTime)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
        }

        btnUnlockTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                unlockTime = "%02d:%02d".format(h, m)
                tvUnlockTime.text = fmt12(unlockTime)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "New Schedule" else "Edit Schedule")
            .setView(view)
            .setPositiveButton(if (existing == null) "Create" else "Save") { _, _ ->
                val days = dayKeys.filterIndexed { i, _ -> dayButtons[i].isChecked }.joinToString(",")
                val body = ScheduledLock(
                    id         = existing?.id ?: 0,
                    deviceId   = if (childDeviceId != -1L) childDeviceId else 0,
                    label      = etLabel.text.toString().ifBlank { "Bedtime" },
                    lockTime   = lockTime,
                    unlockTime = unlockTime,
                    days       = days,
                    isActive   = switchActive.isChecked
                )
                if (existing == null) createSchedule(body) else updateSchedule(body)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createSchedule(body: ScheduledLock) {
        lifecycleScope.launch {
            try {
                val res = api.createSchedule(body)
                if (res.isSuccessful) { Toast.makeText(this@ScheduleLockActivity, "✅ Schedule created", Toast.LENGTH_SHORT).show(); loadSchedules() }
                else Toast.makeText(this@ScheduleLockActivity, "Failed: ${res.code()}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this@ScheduleLockActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun updateSchedule(body: ScheduledLock) {
        lifecycleScope.launch {
            try {
                val res = api.updateSchedule(body.id, body)
                if (res.isSuccessful) { Toast.makeText(this@ScheduleLockActivity, "✅ Schedule updated", Toast.LENGTH_SHORT).show(); loadSchedules() }
                else Toast.makeText(this@ScheduleLockActivity, "Failed: ${res.code()}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this@ScheduleLockActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun deleteSchedule(id: Long) {
        AlertDialog.Builder(this)
            .setTitle("Delete Schedule")
            .setMessage("Delete this schedule?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        api.deleteSchedule(id)
                        Toast.makeText(this@ScheduleLockActivity, "Deleted", Toast.LENGTH_SHORT).show()
                        loadSchedules()
                    } catch (e: Exception) { Toast.makeText(this@ScheduleLockActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun fmt12(t: String): String {
        val parts = t.split(":")
        val h = parts[0].toInt(); val m = parts[1].toInt()
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(h12, m, ampm)
    }

    inner class ScheduleAdapter : BaseAdapter() {
        override fun getCount() = schedules.size
        override fun getItem(pos: Int) = schedules[pos]
        override fun getItemId(pos: Int) = schedules[pos].id

        override fun getView(pos: Int, convertView: View?, parent: android.view.ViewGroup): View {
            val v = convertView ?: LayoutInflater.from(this@ScheduleLockActivity)
                .inflate(R.layout.item_schedule, parent, false)
            val sc = schedules[pos]

            v.findViewById<TextView>(R.id.tvScheduleLabel).text = sc.label
            v.findViewById<TextView>(R.id.tvScheduleTimes).text =
                "🔒 ${fmt12(sc.lockTime)}  →  🔓 ${fmt12(sc.unlockTime)}"
            v.findViewById<TextView>(R.id.tvScheduleDays).text =
                if (sc.days.isBlank()) "Every day"
                else sc.days.split(",").filter { it.isNotBlank() }.joinToString(" · ")
            v.findViewById<TextView>(R.id.tvScheduleStatus).apply {
                text = if (sc.isActive) "ON" else "OFF"
                setTextColor(if (sc.isActive) 0xFF34D399.toInt() else 0xFF9CA3AF.toInt())
            }
            v.findViewById<ImageButton>(R.id.btnEdit).setOnClickListener { showDialog(sc) }
            v.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener { deleteSchedule(sc.id) }
            return v
        }
    }
}
