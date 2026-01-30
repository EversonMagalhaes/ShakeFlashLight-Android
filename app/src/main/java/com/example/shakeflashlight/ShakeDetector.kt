package com.example.shakeflashlight

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    // Limiar de aceleração para considerar uma chacoalhada (ajustável)
    private val threshold = 12.0f

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Cálculo da aceleração total (vetor resultante)
            // Fórmula: sqrt(x² + y² + z²) - gravidade
            val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val gravity = SensorManager.GRAVITY_EARTH
            val actualAcceleration = acceleration - gravity

            if (actualAcceleration > threshold) {
                onShake() // Dispara a função que passamos pelo construtor
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Não precisamos implementar para este caso
    }
}