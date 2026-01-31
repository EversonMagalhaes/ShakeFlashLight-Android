package com.example.shakeflashlight

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
//import android.hardware.SensorManager

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    // Mantendo o seu valor de 50.0f
    private val threshold = 70.0f

    override fun onSensorChanged(event: SensorEvent?) {
        // 1. Verificamos se o evento não é nulo e se é do acelerômetro
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

            // 2. Pegamos apenas o valor do eixo X (movimento lateral)
            val x = event.values[0]

            // 3. Usamos o valor absoluto (Math.abs) para ignorar se é esquerda ou direita
            // Usamos o valor bruto (m/s²) para manter a compatibilidade com seu threshold de 50
            val actualAccelerationX = Math.abs(x)

            // 4. Se a força apenas no X for maior que 50, dispara
            if (actualAccelerationX > threshold) {
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Não precisamos implementar para este caso
    }
}