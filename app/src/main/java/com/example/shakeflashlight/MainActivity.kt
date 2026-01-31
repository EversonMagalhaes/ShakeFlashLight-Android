package com.example.shakeflashlight

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "ShakePrefs"
    private val KEY_SENSITIVITY = "sensitivity_value"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val switchService = findViewById<Switch>(R.id.switchService)
        val seekBar = findViewById<SeekBar>(R.id.seekBarSensitivity)
        val labelSensitivity = findViewById<TextView>(R.id.labelSensitivity)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. CARREGAR PREFERÊNCIAS
        val savedProgress = sharedPrefs.getInt(KEY_SENSITIVITY, 20)
        seekBar.progress = savedProgress
        val initialValue = 40.0f + savedProgress
        labelSensitivity.text = "Sensibilidade: $initialValue"

        // 2. LÓGICA DO SWITCH (INICIAR/PARAR MOTOR)
        switchService.setOnCheckedChangeListener { _, isChecked ->
            val serviceIntent = Intent(this, ShakeService::class.java)
            // Passamos o valor atual da sensibilidade para o serviço
            serviceIntent.putExtra("threshold", 40.0f + seekBar.progress)

            if (isChecked) {
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
                val realValue = 40.0f + progress.toFloat()
                labelSensitivity.text = "Sensibilidade: $realValue"

                // Se o serviço estiver rodando, opcionalmente podemos enviar o novo valor
                // Mas por enquanto, vamos focar em salvar para o próximo início
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 20
                sharedPrefs.edit().putInt(KEY_SENSITIVITY, progress).apply()

                // Dica: Se o Switch estiver ligado, reinicie o serviço para aplicar a nova sensibilidade
                if (switchService.isChecked) {
                    val serviceIntent = Intent(this@MainActivity, ShakeService::class.java)
                    serviceIntent.putExtra("threshold", 40.0f + progress)
                    startService(serviceIntent)
                }
            }
        })
    }
}