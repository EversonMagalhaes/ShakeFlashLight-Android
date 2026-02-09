package com.example.shakeflashlight

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
//import android.hardware.SensorManager

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    // Mantendo o seu valor de 50.0f
    var threshold: Float = 60.0f

//    override fun onSensorChanged(event: SensorEvent?) {
//        // 1. Verificamos se o evento não é nulo e se é do acelerômetro
//        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
//
//            // 2. Pegamos apenas o valor do eixo X (movimento lateral)
//            val x = event.values[0]
//
//            // 3. Usamos o valor absoluto (Math.abs) para ignorar se é esquerda ou direita
//            // Usamos o valor bruto (m/s²) para manter a compatibilidade com seu threshold de 50
//            val actualAccelerationX = Math.abs(x)
//
//            // 4. Se a força apenas no X for maior que 50, dispara
//            if (actualAccelerationX > threshold) {
//                onShake()
//            }
//        }
//    }

    private var movementCount = 0
    private var lastMovementTime: Long = 0
    private var startTime = System.currentTimeMillis()

    override fun onSensorChanged(event: SensorEvent?) {
        // 🛡️ Filtro de Segurança: Ignora os primeiros 3 segundos de "partida" do sensor
        if (System.currentTimeMillis() - startTime < 3000) return

        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val actualAccelerationX = Math.abs(x)

            if (actualAccelerationX > threshold) {
                val now = System.currentTimeMillis()

                // Filtro de tempo: evita contar o mesmo balanço várias vezes.
                // Só conta um novo movimento se passar pelo menos 200ms do último.
                if (now - lastMovementTime > 200) {
                    movementCount++
                    lastMovementTime = now

                    // Só chama o onShake() após 3 movimentos válidos
                    if (movementCount >= 2) {
                        onShake()
                        movementCount = 0 // Reseta para a próxima vez
                    }
                }
            } else {
                // Se o usuário parar de chacoalhar por mais de 1 segundo, reseta a contagem
                if (System.currentTimeMillis() - lastMovementTime > 1000) {
                    movementCount = 0
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Não precisamos implementar para este caso
    }
}