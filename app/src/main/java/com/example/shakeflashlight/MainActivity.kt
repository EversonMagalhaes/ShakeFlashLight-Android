package com.example.shakeflashlight

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val cameraManager by lazy {
        getSystemService(CAMERA_SERVICE) as CameraManager
    }

    // Gerenciador de Sensores do Android
    private val sensorManager by lazy {
        getSystemService(SENSOR_SERVICE) as SensorManager
    }

    private var cameraId: String? = null
    private var isFlashOn = false // Estado da nossa lanterna
    private lateinit var shakeDetector: ShakeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Inicializamos o detector e passamos o que ele deve fazer (o onShake)
        shakeDetector = ShakeDetector {
            toggleFlashlight()
        }
    }

    private fun toggleFlashlight() {
        try {
            cameraId?.let { id ->
                isFlashOn = !isFlashOn
                cameraManager.setTorchMode(id, isFlashOn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Ativamos o sensor quando o app ganha foco
    override fun onResume() {
        super.onResume()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    // Desativamos o sensor quando o app vai para o fundo (economiza bateria)
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }
}