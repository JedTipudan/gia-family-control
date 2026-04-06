package com.gia.parentcontrol.util

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores parent PIN encrypted locally. Used to protect sensitive actions. */
object SecureAuthManager {

    private const val FILE = "parent_secure_auth"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_TEMP_UNTIL = "temp_access_until"

    private fun prefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, FILE, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun setPin(context: Context, pin: String) {
        prefs(context).edit().putString(KEY_PIN_HASH, pin.sha256()).apply()
    }

    fun hasPin(context: Context) = prefs(context).getString(KEY_PIN_HASH, null) != null

    fun verifyPin(context: Context, input: String) =
        prefs(context).getString(KEY_PIN_HASH, null) == input.sha256()

    fun grantTemporaryAccess(context: Context, minutes: Int) {
        val until = System.currentTimeMillis() + minutes * 60_000L
        prefs(context).edit().putLong(KEY_TEMP_UNTIL, until).apply()
        Log.d("SecureAuth", "Temp access granted for $minutes min")
    }

    fun isTemporaryAccessActive(context: Context): Boolean {
        val until = prefs(context).getLong(KEY_TEMP_UNTIL, 0L)
        return System.currentTimeMillis() < until
    }

    fun revokeTemporaryAccess(context: Context) {
        prefs(context).edit().putLong(KEY_TEMP_UNTIL, 0L).apply()
    }

    private fun String.sha256(): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
