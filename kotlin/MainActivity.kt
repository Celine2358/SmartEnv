package com.example.smartenv

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter // 블루투스 기본 어댑터 객체
    private lateinit var btSocket: BluetoothSocket // 블루투스 통신 소켓 객체
    private val hc06Address = "98:DA:60:0A:F4:BD" // HC-06 모듈의 MAC 주소

    private lateinit var sensorViewModel: SensorViewModel
    private var isIndoorEnv = false // 변수 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sensorViewModel = (application as MyApplication).sensorViewModel

        // 블루투스 관리자 객체 생성
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Smart-Env와 통신을 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // Start 스위치를 켰을 때 블루투스 시작
        val btSwitch: Switch = findViewById(R.id.hc06switch)
        btSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 블루투스 사용 권한 요청
                requestBluetoothPermission()
                // 스위치가 켜진 상태이면 블루투스 연결 시작
                connectToHC06()
                btSwitch.text = "Stop?"
            } else {
                // 스위치가 꺼진 상태이면 블루투스 연결 종료
                disconnectHC06()
                btSwitch.text = "Start"
            }
        }

        // 메뉴 툴바 적용시키기
        val toolbar: androidx.appcompat.widget.Toolbar? = findViewById(R.id.MenuBar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }

        // 현재 날짜를 DateText에 표현
        val dateTextView: TextView = findViewById(R.id.DateText)
        val sdf1 = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        val currentDate: String = sdf1.format(Date())
        dateTextView.text = currentDate

        // 시간대에 따라 앱 배경화면 변경
        val timeBackground: ImageView = findViewById(R.id.TimeBackground)

        val sdf2 = SimpleDateFormat("HH", Locale.getDefault())
        val currentHour: Int = sdf2.format(Date()).toInt()

        val drawableId = if (currentHour in 6..17) {
            R.drawable.noon
        } else {
            R.drawable.night
        }

        timeBackground.setImageResource(drawableId)
    }

    private fun connectToHC06() {
        val hc06Device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(hc06Address)
        if (hc06Device == null) {
            Toast.makeText(this, "Smart-Env를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            btSocket = hc06Device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            MyApplication.btSocket = btSocket
            btSocket.connect()
            Toast.makeText(this, "Smart-Env와 연결되었습니다!!", Toast.LENGTH_SHORT).show()
            // 연결된 소켓으로 데이터 전송
            // 블루투스 소켓 연결 후 데이터 수신 스레드 시작
            startDataReceivingThread()
        } catch (e: IOException) {
            Toast.makeText(this, "Smart-Env와 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun disconnectHC06() {
        try {
            if (::btSocket.isInitialized && btSocket.isConnected) {
                btSocket.close()
                Toast.makeText(this, "Smart-Env와의 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.temp).text = "°C"
                findViewById<TextView>(R.id.hum).text = "%"
                findViewById<TextView>(R.id.dust).text = "?"
                findViewById<TextView>(R.id.batteryInt).text = "?"
            } else {
                Toast.makeText(this, "Smart-Env와의 연결이 이미 해제되었습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Smart-Env와의 연결 해제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun requestBluetoothPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), REQUEST_ENABLE_BT)
        } else {
            enableBluetooth()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 블루투스 권한이 허용된 경우
                enableBluetooth()
            } else {
                Toast.makeText(this, "블루투스 사용 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            connectToHC06()
        }
    }

    private fun startDataReceivingThread() {
        val inputStream = btSocket.inputStream // 데이터를 읽을 InputStream
        val buffer = ByteArray(1024) // 데이터를 읽을 버퍼

        // 데이터를 읽는 스레드 시작
        Thread {
            while (true) {
                try {
                    // 데이터를 읽어들임
                    val bytes = inputStream.read(buffer)
                    // 읽은 데이터를 문자열로 변환
                    val data = String(buffer, 0, bytes)
                    // 데이터를 화면에 표시
                    runOnUiThread { displayData(data) }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }.start()
    }

    // 블루투스 모듈로 받은 센서 값 출력
    private fun displayData(data: String) {
        val dataArray = data.split(",")
        if (dataArray.size == 4) {
            val temperature = dataArray[0]
            val humidity = dataArray[1]
            val dustValue = dataArray[2]
            val batteryPercent = dataArray[3].trim()

            findViewById<TextView>(R.id.temp).text = "$temperature °C"
            findViewById<TextView>(R.id.hum).text = "$humidity %"
            findViewById<TextView>(R.id.dust).text = dustValue
            findViewById<TextView>(R.id.batteryInt).text = "$batteryPercent %"

            // ViewModel에 데이터 저장
            sensorViewModel.temperature.value = temperature
            sensorViewModel.humidity.value = humidity
            sensorViewModel.dustValue.value = dustValue
        }
    }


    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }


    // 메뉴바 관리 함수
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.SmartEnvControl -> { // iot_control.xml로 변경
                val intent1 = Intent(this, IoTControlActivity::class.java)
                startActivity(intent1)
                true
            }
            R.id.EnvTotal -> { // indoor_env.xml로 변경
                val intent2 = Intent(this, IndoorActivity::class.java)
                startActivity(intent2)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}