package com.gia.parentcontrol.util

import android.content.Context

object SecureAuthManager {

    private const val FILE = "parent_secure_auth"
    private const val KEY_PIN_HASH = "pin_hash"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setPin(context: Context, pin: String) {
        prefs(context).edit().putString(KEY_PIN_HASH, pin.sha256()).apply()
    }

    fun hasPin(context: Context) =
        prefs(context).getString(KEY_PIN_HASH, null) != null

    fun verifyPin(context: Context, input: String) =
        prefs(context).getString(KEY_PIN_HASH, null) == input.sha256()

    private fun String.sha256(): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
