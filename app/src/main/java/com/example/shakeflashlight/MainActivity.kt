package com.example.shakeflashlight

import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // O lazy garante que a variável só seja inicializada quando for usada pela primeira vez
    private val cameraManager by lazy {
        getSystemService(CAMERA_SERVICE) as CameraManager
    }

    // ID da câmera que possui o flash (geralmente a "0")
    private var cameraId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa o ID da câmera
        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}