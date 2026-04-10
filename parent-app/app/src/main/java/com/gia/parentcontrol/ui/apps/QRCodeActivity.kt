package com.gia.parentcontrol.ui.apps

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.gia.parentcontrol.R
import com.gia.parentcontrol.network.RetrofitClient
import kotlinx.coroutines.launch

class QRCodeActivity : AppCompatActivity() {

    private val api by lazy { RetrofitClient.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)

        val prefs = getSharedPreferences("parent_prefs", MODE_PRIVATE)
        val pairCode = prefs.getString("pair_code", null)

        if (pairCode != null) {
            showQr(pairCode)
        } else {
            // Fetch from API if not cached
            val userId = prefs.getLong("user_id", -1L)
            if (userId == -1L) { finish(); return }
            lifecycleScope.launch {
                try {
                    val resp = api.getUserProfile(userId)
                    val code = resp.body()?.pairCode
                    if (code != null) {
                        prefs.edit().putString("pair_code", code).apply()
                        showQr(code)
                    } else {
                        Toast.makeText(this@QRCodeActivity,
                            "Pair code not found. Try logging out and back in.",
                            Toast.LENGTH_LONG).show()
                        finish()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@QRCodeActivity,
                        "Error loading pair code: ${e.message}",
                        Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClose)
            .setOnClickListener { finish() }
    }

    private fun showQr(pairCode: String) {
        findViewById<TextView>(R.id.tvPairCode).text = pairCode
        findViewById<ImageView>(R.id.ivQrCode).setImageBitmap(generateQr(pairCode))
    }

    private fun generateQr(content: String): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) for (y in 0 until 512)
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.parseColor("#202124") else android.graphics.Color.WHITE)
        return bmp
    }
}
