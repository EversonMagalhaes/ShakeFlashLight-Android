package com.example.shakeflashlight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
//            val sharedPrefs = context.getSharedPreferences("ShakePrefs", Context.MODE_PRIVATE)
//            val isServiceActive = sharedPrefs.getBoolean("service_active", false)
//            val savedProgress = sharedPrefs.getInt("sensitivity_value", 20)
//
//            // Só inicia se o usuário tiver deixado o Switch ligado anteriormente
//            if (isServiceActive) {
//                val serviceIntent = Intent(context, ShakeService::class.java).apply {
//                    putExtra("threshold", 40.0f + savedProgress)
//                }
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    context.startForegroundService(serviceIntent)
//                } else {
//                    context.startService(serviceIntent)
//                }
//            }
//        }
//    }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {

            // 1. Criamos o contexto seguro para ler ANTES da senha
            val safeContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }

            // 2. Usamos o safeContext para pegar as preferências
            val sharedPrefs = safeContext.getSharedPreferences("ShakePrefs", Context.MODE_PRIVATE)
            val isServiceActive = sharedPrefs.getBoolean("service_active", false)
            val savedProgress = sharedPrefs.getInt("sensitivity_value", 20)

            if (isServiceActive) {
                val serviceIntent = Intent(safeContext, ShakeService::class.java).apply {
                    putExtra("threshold", 40.0f + savedProgress)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeContext.startForegroundService(serviceIntent)
                } else {
                    safeContext.startService(serviceIntent)
                }
            }
        }
    }

}