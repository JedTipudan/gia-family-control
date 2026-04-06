package com.gia.parentcontrol.ui.apps

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.gia.parentcontrol.R

class QRCodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)

        val pairCode = getSharedPreferences("parent_prefs", MODE_PRIVATE)
            .getString("pair_code", null)

        if (pairCode == null) {
            finish(); return
        }

        findViewById<TextView>(R.id.tvPairCode).text = pairCode
        findViewById<ImageView>(R.id.ivQrCode).setImageBitmap(generateQr(pairCode))
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClose)
            .setOnClickListener { finish() }
    }

    private fun generateQr(content: String): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) for (y in 0 until 512)
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#111111"))
        return bmp
    }
}
