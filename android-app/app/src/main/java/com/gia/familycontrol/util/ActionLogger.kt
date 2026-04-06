package com.gia.familycontrol.util

import android.content.Context
import android.util.Log

/**
 * Lightweight action logger for debugging lock/unlock/hide events.
 * Stores last 100 entries in SharedPreferences.
 */
object ActionLogger {

    private const val TAG = "ActionLogger"
    private const val PREFS = "gia_action_log"
    private const val KEY_LOG = "log"
    private const val MAX_ENTRIES = 100

    fun log(context: Context, action: String, detail: String = "") {
        val entry = "${System.currentTimeMillis()}|$action|$detail"
        Log.d(TAG, "ACTION: $action $detail")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_LOG, mutableSetOf())!!.toMutableSet()
        // Trim if over limit
        if (existing.size >= MAX_ENTRIES) {
            val oldest = existing.minByOrNull { it.substringBefore("|").toLongOrNull() ?: 0L }
            oldest?.let { existing.remove(it) }
        }
        existing.add(entry)
        prefs.edit().putStringSet(KEY_LOG, existing).apply()
    }

    fun getLog(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_LOG, emptySet())!!
            .sortedByDescending { it.substringBefore("|").toLongOrNull() ?: 0L }
}
