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
import androidx.core.graphics.toColorInt

class ShakeService : Service() {

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isFlashOn = false
    private var lastClickTime: Long = 0
    private val SHAKE_DELAY = 500

    private var isDetectorRegistered = false


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

        // Criamos o detector UMA VEZ aqui
        shakeDetector = ShakeDetector {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > SHAKE_DELAY) {
                lastClickTime = currentTime
                isFlashOn = !isFlashOn
                toggleFlash(isFlashOn)
                shakeVibrate()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val threshold = intent?.getFloatExtra("threshold", 60.0f) ?: 60.0f

        if (::shakeDetector.isInitialized) {
            shakeDetector.threshold = threshold
        }
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "SHAKE_FINAL_CHANNEL")
            .setSmallIcon(R.drawable.ic_flashlight_notif)
            .setColor("#0D47A1".toColorInt()) // Exemplo de cor (Teal escuro)
            .setColorized(true) // No Android moderno, isso destaca a cor no ícone e botões
            .setContentTitle("Lanterna Rápida Ativa")
            .setContentText("O sensor está monitorando movimentos")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Mude para LOW para ser discreto ao atualizar
            .setOngoing(true)
            .setOnlyAlertOnce(true) // A MÁGICA ESTÁ AQUI: Só alerta na primeira vez!
            .setContentIntent(pendingIntent) // <--- ESSA LINHA FAZ A MÁGICA
            .setAutoCancel(false) // Como é um serviço em primeiro plano, o SO gerencia is
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Categoria de serviço de sistema ajuda na prioridade de exibição
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
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


        shakeDetector.threshold = threshold

        // Registra o sensor apenas se ainda não estiver registrado
        if (!isDetectorRegistered) {
            val accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isDetectorRegistered = true
        }

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
        // Remove o listener para parar de gastar bateria e permitir reset
        sensorManager.unregisterListener(shakeDetector)
        isDetectorRegistered = false
        toggleFlash(false)
        // 2. POR ÚLTIMO e UMA ÚNICA VEZ chamamos o super
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}