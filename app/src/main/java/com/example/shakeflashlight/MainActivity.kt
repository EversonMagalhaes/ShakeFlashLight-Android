package com.example.shakeflashlight

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "ShakePrefs"
    private val KEY_SENSITIVITY = "sensitivity_value"
    private val KEY_SERVICE_ACTIVE = "service_active" // Nova chave!

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.CAMERA)
        }

        // Corrigido: Apenas adiciona à lista, o requestPermissions final cuida de tudo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        val switchService = findViewById<Switch>(R.id.switchService)
        val seekBar = findViewById<SeekBar>(R.id.seekBarSensitivity)
        val labelSensitivity = findViewById<TextView>(R.id.labelSensitivity)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. CARREGAR PREFERÊNCIAS (Sensibilidade e Estado do Switch)
        val savedProgress = sharedPrefs.getInt(KEY_SENSITIVITY, 20)
        val isServiceActive = sharedPrefs.getBoolean(KEY_SERVICE_ACTIVE, false)

        seekBar.progress = savedProgress
        val currentThreshold = 40.0f + savedProgress
        labelSensitivity.text = "Sensibilidade: $currentThreshold"

        // Aqui está o pulo do gato: define o estado visual do botão
        switchService.isChecked = isServiceActive

        // 2. LÓGICA DO SWITCH
        switchService.setOnCheckedChangeListener { _, isChecked ->
            // SALVA o estado para o app lembrar depois
            sharedPrefs.edit().putBoolean(KEY_SERVICE_ACTIVE, isChecked).apply()

            val serviceIntent = Intent(this, ShakeService::class.java)
            serviceIntent.putExtra("threshold", 40.0f + seekBar.progress)

            if (isChecked) {
                serviceIntent.putExtra("threshold", 40.0f + seekBar.progress)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                stopService(serviceIntent)
            }
        }

        // 3. LÓGICA DO SEEKBAR
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                labelSensitivity.text = "Sensibilidade: ${40.0f + progress}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 20
                sharedPrefs.edit().putInt(KEY_SENSITIVITY, progress).apply()

                // Se estiver ligado, reinicia o serviço para atualizar a sensibilidade
                if (switchService.isChecked) {
                    val serviceIntent = Intent(this@MainActivity, ShakeService::class.java)
                    serviceIntent.putExtra("threshold", 40.0f + progress)
                    startService(serviceIntent)
                }
            }
        })


// Alterna lanterna visual ligada/desligada
        val imgShakeDemo = findViewById<ImageView>(R.id.imageShakeIllustration)

        val handler = android.os.Handler(mainLooper)

        var flashOn = false          // false = shake_off | true = shake_on
        var direction = 2           // esquerda / direita
        var moveCount = 0            // conta os movimentos

        handler.post(object : Runnable {
            override fun run() {

                // 1️⃣ Movimento esquerda / direita
                imgShakeDemo.animate()
                    .translationX((80f * direction).toFloat())
                    .translationY((-20f * direction).toFloat())
                    .setDuration(150)
                    .withEndAction {
                        direction *= -1
                    }
                    .start()

                moveCount++

                // 2️⃣ Após 2 movimentos, troca imagem e pausa
                if (moveCount >= 4) {
                    moveCount = 0
                    flashOn = !flashOn

                    imgShakeDemo.setImageResource(
                        if (flashOn) R.drawable.shake_on else R.drawable.shake_off
                    )

                    // Pausa maior ao trocar imagem (2,5s)
                    handler.postDelayed(this, 2500)
                } else {
                    // Pausa curta entre movimentos
                    handler.postDelayed(this, 300)
                }
            }
        })


    }
}