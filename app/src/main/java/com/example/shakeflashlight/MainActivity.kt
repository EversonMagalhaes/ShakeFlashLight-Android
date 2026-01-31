package com.example.shakeflashlight

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class MainActivity : AppCompatActivity() {
    private fun shakeVibrate() {
        val pattern = longArrayOf(0, 100, 50, 200) // Espera 0ms, vibra 100ms, para 50ms, vibra 100ms

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)) // -1 significa "não repetir"
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                // Para celulares muito antigos
                vibrator.vibrate(pattern, -1)
            }
        }
    }
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
                shakeVibrate()
            }
        }

        val switchService = findViewById<android.widget.Switch>(R.id.switchService)
        val seekBar = findViewById<android.widget.SeekBar>(R.id.seekBarSensitivity)
        val labelSensitivity = findViewById<android.widget.TextView>(R.id.labelSensitivity)

// Lógica para o SeekBar (Sensibilidade de 40 a 70)
        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val realValue = 40.0f + progress
                labelSensitivity.text = "Sensibilidade: $realValue"

                // Aqui atualizamos o detector em tempo real!
                // (Precisaremos ajustar o ShakeDetector para aceitar mudanças de threshold)
                shakeDetector.threshold = realValue
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        shakeDetector.threshold = 60.0f

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