package com.example.smartenv

import android.app.Application
import android.bluetooth.BluetoothSocket
import java.io.OutputStream

class MyApplication : Application() {

    val sensorViewModel: SensorViewModel by lazy {
        SensorViewModel()
    }

    companion object {
        lateinit var btSocket: BluetoothSocket
    }

    override fun onCreate() {
        super.onCreate()
        // 초기화 필요 시 여기에 작성
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            btSocket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
