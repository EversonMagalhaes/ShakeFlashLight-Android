package com.example.shakeflashlight

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ShakeService : Service() {

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isFlashOn = false
    private var lastClickTime: Long = 0
    private val SHAKE_DELAY = 500

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
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    override fun onCreate() {
        super.onCreate()
        // Mova para cá! O serviço é quem precisa manter o processador acordado.
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "ShakeFlashlight:WakeLock")
        wakeLock?.acquire()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: Exception) {}

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val threshold = intent?.getFloatExtra("threshold", 60.0f) ?: 60.0f

        // 1. Criar Canal de Notificação (Obrigatório para Android 8+)
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "SHAKE_SERVICE_CHANNEL")
            .setContentTitle("Lanterna Rápida Ativa")
            .setContentText("Chacoalhe para ligar/desligar")
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Use um ícone temporário
            .build()

        // 2. Transforma em Foreground Service (Não deixa o Android matar o app)
        startForeground(1, notification)

        // 3. Inicia o Detector
        shakeDetector = ShakeDetector {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > SHAKE_DELAY) {
                lastClickTime = currentTime
                isFlashOn = !isFlashOn
                toggleFlash(isFlashOn)
                // Chame o seu shakeVibrate() aqui também se quiser!
                shakeVibrate()
            }
        }
        shakeDetector.threshold = threshold

        val accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)

        return START_STICKY
    }

    private fun toggleFlash(status: Boolean) {
        try {
            cameraId?.let { cameraManager.setTorchMode(it, status) }
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        // Solta o WakeLock quando o usuário desligar o Switch (stopService)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
        sensorManager.unregisterListener(shakeDetector)
        toggleFlash(false) // Garante que apaga ao desligar o serviço
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "SHAKE_SERVICE_CHANNEL",
                "Serviço de Lanterna",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}