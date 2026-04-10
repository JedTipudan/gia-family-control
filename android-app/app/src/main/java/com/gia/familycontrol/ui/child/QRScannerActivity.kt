package com.gia.familycontrol.ui.child

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.databinding.ActivityQrScannerBinding
import com.gia.familycontrol.model.PairRequest
import com.gia.familycontrol.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessaging
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kotlinx.coroutines.launch

class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private val api by lazy { RetrofitClient.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (checkCameraPermission()) {
            startScanning()
        } else {
            requestCameraPermission()
        }

        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), 200
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startScanning() {
        binding.barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                val pairCode = result.text?.trim() ?: return
                // Accept any non-empty scanned code and try to pair
                if (pairCode.isNotEmpty()) {
                    binding.barcodeScanner.pause()
                    pairWithCode(pairCode)
                }
            }
            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })
    }

    private fun pairWithCode(pairCode: String) {
        binding.tvStatus.text = "Pairing..."

        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            lifecycleScope.launch {
                try {
                    val response = api.pairDevice(PairRequest(
                        pairCode = pairCode,
                        deviceName = Build.MODEL,
                        deviceModel = Build.MANUFACTURER,
                        androidVersion = Build.VERSION.RELEASE,
                        fcmToken = fcmToken
                    ))

                    if (response.isSuccessful && response.body() != null) {
                        val device = response.body()!!
                        getSharedPreferences("gia_prefs", MODE_PRIVATE).edit()
                            .putLong("device_id", device.id).apply()
                        Toast.makeText(this@QRScannerActivity, "✅ Paired successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        val msg = when {
                            errorBody.contains("already have a child", ignoreCase = true) ->
                                "Parent already has a child paired. Unpair first."
                            errorBody.contains("already paired", ignoreCase = true) ->
                                "This child is already paired with a parent."
                            errorBody.contains("Invalid pair code", ignoreCase = true) ->
                                "Invalid pair code. Make sure you scan the parent's QR."
                            else -> "Pairing failed (${response.code()}): $errorBody"
                        }
                        binding.tvStatus.text = msg
                        Toast.makeText(this@QRScannerActivity, msg, Toast.LENGTH_LONG).show()
                        binding.barcodeScanner.resume()
                    }
                } catch (e: Exception) {
                    binding.tvStatus.text = "Connection error: ${e.message}"
                    binding.barcodeScanner.resume()
                }
            }
        }.addOnFailureListener {
            binding.tvStatus.text = "Failed to get device token. Try again."
            binding.barcodeScanner.resume()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }
}
