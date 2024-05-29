package com.example.smartenv

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.io.OutputStream

class IoTControlActivity : AppCompatActivity() {

    // BluetoothSocket 변수를 선언해서 MainActivity에서 값을 넘겨 받음
    private lateinit var btSocket: BluetoothSocket


    // 디폴트 값
    private var coldTemp = "18"
    private var hotTemp = "26"
    private var lowHum = "40"
    private var dustSet = "10"
    private var FanOn = "1"
    private var HumOn = "1"
    private var HeaterOn = "1"
    private var AirOn = "1"
    private var autoFan = "1"
    private var autoHum = "1"
    private var autoHeater = "1"
    private var autoAir = "1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.iot_control)

        // MyApplication에서 BluetoothSocket 가져오기
        btSocket = MyApplication.btSocket

        // 메뉴 툴바 적용시키기
        val toolbar2: androidx.appcompat.widget.Toolbar? = findViewById(R.id.MenuBar2)
        setSupportActionBar(toolbar2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }

        // 온도, 습도, 미세먼지 값을 입력하는 TextInputEditText
        val coldTempText: EditText = findViewById(R.id.coldtempin)
        val hotTempText: EditText = findViewById(R.id.hottempin)
        val lowHumText: EditText = findViewById(R.id.lowhumin)
        val dustsetinText: EditText = findViewById(R.id.dustsetin)

        // 온도, 습도, 미세먼지 적용 버튼
        val tempButton: Button = findViewById(R.id.tempbutton)
        val humButton: Button = findViewById(R.id.humbutton)
        val dustButton: Button = findViewById(R.id.dustbutton)

        // 선풍기, 온풍기, 가습기, 공기청정기 On Off
        val FanOnCheck = findViewById<CheckBox>(R.id.OnCheck1)
        val HumOnCheck = findViewById<CheckBox>(R.id.OnCheck2)
        val HeaterOnCheck = findViewById<CheckBox>(R.id.OnCheck3)
        val AirOnCheck = findViewById<CheckBox>(R.id.OnCheck4)

        // 자동 수동 togglebutton
        val fantoggle = findViewById<ToggleButton>(R.id.fantoggle)
        val humtoggle = findViewById<ToggleButton>(R.id.humtoggle)
        val heatertoggle = findViewById<ToggleButton>(R.id.heatertoggle)
        val airtoggle = findViewById<ToggleButton>(R.id.airtoggle)

        // 버튼 클릭 시 온도 값을 HC-06을 통해 아두이노로 전송
        tempButton.setOnClickListener {
            // 온도 값 가져오기
            coldTemp = coldTempText.text.toString()
            hotTemp = hotTempText.text.toString()

            // 입력된 값이 비어있는지 확인
            if (coldTemp.isEmpty() || hotTemp.isEmpty()) {
                Toast.makeText(this, "온도를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 입력된 값이 정수인지 확인
            val coldTempInt = coldTemp.toIntOrNull()
            val hotTempInt = hotTemp.toIntOrNull()

            if (coldTempInt == null || hotTempInt == null) {
                Toast.makeText(this, "온도는 정수로 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 입력된 값이 0부터 100 사이에 있는지 확인
            if (coldTempInt < 0 || coldTempInt > 100 || hotTempInt < 0 || hotTempInt > 100) {
                Toast.makeText(this, "온도는 0부터 100 사이의 값으로 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // coldTemp가 hotTemp보다 큰지 확인
            if (coldTempInt > hotTempInt) {
                Toast.makeText(this, "온풍기 작동 온도는 선풍기 작동 온도보다 낮아야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendTemperature(coldTemp, hotTemp)
        }
        // 버튼 클릭 시 습도 값을 HC-06을 통해 아두이노로 전송
        humButton.setOnClickListener {
            // 습도 값 가져오기
            lowHum = lowHumText.text.toString()

            // 입력된 값이 비어있는지 확인
            if (lowHum.isEmpty()) {
                Toast.makeText(this, "습도를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 입력된 값이 정수인지 확인
            val lowHumInt = lowHum.toIntOrNull()

            if (lowHumInt == null) {
                Toast.makeText(this, "습도는 정수로 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 입력된 값이 0부터 100 사이에 있는지 확인
            if (lowHumInt < 0 || lowHumInt > 100) {
                Toast.makeText(this, "습도는 0부터 100 사이의 값으로 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendHumidity(lowHum)
        }
        // 버튼 클릭 시 미세먼지 설정 값을 HC-06을 통해 아두이노로 전송
        dustButton.setOnClickListener {
            dustSet = dustsetinText.text.toString()

            if (dustSet.isEmpty()) {
                Toast.makeText(this, "미세먼지 값을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dustSetInt = dustSet.toIntOrNull()

            if (dustSetInt == null || dustSetInt < 0) {
                Toast.makeText(this, "미세먼지 값은 0 이상의 정수로 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendDustSet(dustSet)
        }

        // 선풍기를 On, Off 체크
        FanOnCheck.setOnCheckedChangeListener { _, isChecked ->
            FanOn = if (isChecked) "1" else "0"
            sendFanOnOff(FanOn)
        }
        // 가습기를 On, Off 체크
        HumOnCheck.setOnCheckedChangeListener { _, isChecked ->
            HumOn = if (isChecked) "1" else "0"
            sendHumOnOff(HumOn)
        }
        // 온풍기를 On, Off 체크
        HeaterOnCheck.setOnCheckedChangeListener { _, isChecked ->
            HeaterOn = if (isChecked) "1" else "0"
            sendHeaterOnOff(HeaterOn)
        }
        // 공기청정기를 On, Off 체크
        AirOnCheck.setOnCheckedChangeListener { _, isChecked ->
            AirOn = if (isChecked) "1" else "0"
            sendAirOnOff(AirOn)
        }

        // 기기 자동 수동 설정 togglebutton
        fantoggle.setOnCheckedChangeListener { _, isChecked ->
            autoFan = if (isChecked) "1" else "0"
            sendFanAuto(autoFan)
        }

        humtoggle.setOnCheckedChangeListener { _, isChecked ->
            autoHum = if (isChecked) "1" else "0"
            sendHumAuto(autoHum)
        }

        heatertoggle.setOnCheckedChangeListener { _, isChecked ->
            autoHeater = if (isChecked) "1" else "0"
            sendHeaterAuto(autoHeater)
        }

        airtoggle.setOnCheckedChangeListener { _, isChecked ->
            autoAir = if (isChecked) "1" else "0"
            sendAirAuto(autoAir)
        }
    }

    private fun sendTemperature(coldTemp: String, hotTemp: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (온도)
            val data1 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data1.toByteArray())

            Toast.makeText(this, "온도 값이 전송되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "온도 값을 전송 실패하였습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendHumidity(lowHum: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (습도)
            val data2 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data2.toByteArray())

            Toast.makeText(this, "습도 값이 전송되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "습도 값을 전송 실패하였습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendDustSet(dustSet: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (미세먼지)
            val data3 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data3.toByteArray())

            Toast.makeText(this, "미세먼지 값이 전송되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "미세먼지 값을 전송 실패하였습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendFanOnOff(FanOn: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (선풍기 on/off)
            val data4 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data4.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "선풍기에 On/Off를 적용하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendHumOnOff(HumOn: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (가습기 on/off)
            val data5 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data5.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "가습기에 On/Off를 적용하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendHeaterOnOff(HeaterOn: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (온풍기 on/off)
            val data6 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data6.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "온풍기에 On/Off를 적용하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendAirOnOff(AirOn: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (공기청정기 on/off)
            val data7 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data7.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "공기청정기에 On/Off를 적용하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendFanAuto(autoFan: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (선풍기 자동/수동)
            val data8 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data8.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "선풍기 자동/수동 설정을 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendHumAuto(autoHum: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (가습기 자동/수동)
            val data9 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data9.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "가습기 자동/수동 설정을 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendHeaterAuto(autoHeater: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (온풍기 자동/수동)
            val data10 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data10.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "온풍기 자동/수동 설정을 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendAirAuto(autoAir: String) {
        try {
            // 블루투스 소켓에서 출력 스트림 가져오기
            val outputStream: OutputStream = btSocket.outputStream

            // 전송할 데이터 생성 (공기청정기 자동/수동)
            val data11 = "$coldTemp,$hotTemp,$lowHum,$dustSet,$FanOn,$HumOn,$HeaterOn,$AirOn,$autoFan,$autoHum,$autoHeater,$autoAir"

            // 데이터 전송
            outputStream.write(data11.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "공기청정기 자동/수동 설정을 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    // 메뉴바 관리 함수
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.SmartEnvControl -> { // iot_control.xml로 변경
                val intent = Intent(this, IoTControlActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.EnvTotal -> { // indoor_env.xml로 변경
                val intent = Intent(this, IndoorActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}