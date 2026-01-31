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


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Mudei de "SHAKE_V3_CHANNEL" para "SHAKE_FINAL_CHANNEL"
            val channelId = "SHAKE_FINAL_CHANNEL"
            val channelName = "Monitor de Chacoalho"
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Essencial para a lanterna funcionar"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 50, 200) // Tenta forçar o padrão de vibração no canal
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(serviceChannel)

        }
    }
//    private fun shakeVibrate() {
//        val pattern = longArrayOf(0, 100, 50, 200) // Espera 0ms, vibra 100ms, para 50ms, vibra 100ms
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
//            val vibrator = vibratorManager.defaultVibrator
//            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)) // -1 significa "não repetir"
//        } else {
//            @Suppress("DEPRECATION")
//            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
//            } else {
//                // Para celulares muito antigos
//                vibrator.vibrate(pattern, -1)
//            }
//        }
//    }

    private fun shakeVibrate() {
        val pattern = longArrayOf(0, 100, 50, 200)

        // Criamos um "atributo" dizendo que isso é um alerta de hardware
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), audioAttributes)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Passando o audioAttributes aqui é o segredo para o Samsung Android 11
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), audioAttributes)
                } else {
                    vibrator.vibrate(pattern, -1)
                }
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

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "SHAKE_FINAL_CHANNEL")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Lanterna Rápida Ativa")
            .setContentText("O sensor está monitorando movimentos")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Alta prioridade
            .setOngoing(true)
            .setSilent(false) // Garante que não seja uma notificação "muda"
            // Esta linha abaixo é crucial para Android 12+ (Samsung)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            // Se der erro de permissão no Samsung, ele cai aqui
            e.printStackTrace()
        }


        // O resto do seu código (shakeDetector...) está PERFEITO!
        shakeDetector = ShakeDetector {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > SHAKE_DELAY) {
                lastClickTime = currentTime
                isFlashOn = !isFlashOn
                toggleFlash(isFlashOn)
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
        // 1. Primeiro limpamos as nossas coisas
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        sensorManager.unregisterListener(shakeDetector)
        toggleFlash(false)

        // 2. POR ÚLTIMO e UMA ÚNICA VEZ chamamos o super
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}