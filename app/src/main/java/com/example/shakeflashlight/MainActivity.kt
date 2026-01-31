package com.example.shakeflashlight

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
//import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
//import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private var isFlashOn = false
    private var lastClickTime: Long = 0
    private val SHAKE_DELAY = 1000 // 1 segundo de intervalo

    private val cameraManager by lazy {
        getSystemService(CAMERA_SERVICE) as CameraManager
    }

    private val sensorManager by lazy {
        getSystemService(SENSOR_SERVICE) as SensorManager
    }

    private var cameraId: String? = null
    private lateinit var shakeDetector: ShakeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: Exception) {
            Log.e("SHAKE_APP", "Erro ao acessar câmera: ${e.message}")
        }

        // Inicializamos o detector com a lógica de tempo (Debounce)
        shakeDetector = ShakeDetector {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > SHAKE_DELAY) {
                lastClickTime = currentTime
                isFlashOn = !isFlashOn
                toggleFlash(isFlashOn)
            }
        }
    }

    private fun toggleFlash(status: Boolean) {
        try {
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, status)
                Log.d("SHAKE_APP", "Lanterna status: $status")
            }
        } catch (e: Exception) {
            Log.e("SHAKE_APP", "Erro: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }
}