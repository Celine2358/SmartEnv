package com.example.smartenv

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class IndoorActivity : AppCompatActivity() {

    private lateinit var sensorViewModel: SensorViewModel
    private var isUnitConverted: Boolean = false // ug/m3 변환 상태를 저장할 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.indoor_env)

        // 메뉴 툴바 적용시키기
        val toolbar3: androidx.appcompat.widget.Toolbar? = findViewById(R.id.MenuBar3)
        setSupportActionBar(toolbar3)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main3)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sensorViewModel = (application as MyApplication).sensorViewModel

        val dustvalueView = findViewById<TextView>(R.id.dustvalue)

        // 온도, 습도, 미세먼지 값 전달
        sensorViewModel.temperature.observe(this, Observer { temperature ->
            findViewById<TextView>(R.id.tempvalue).text = "$temperature °C"
        })

        sensorViewModel.humidity.observe(this, Observer { humidity ->
            findViewById<TextView>(R.id.humvalue).text = "$humidity %"
        })

        // 미세먼지 값 업데이트 시 체크박스 상태에 따라 변환
        sensorViewModel.dustValue.observe(this, Observer { dustValue ->
            val dustValueFloat = dustValue.toFloatOrNull() ?: 0f
            if (isUnitConverted) {
                val adcValue = dustValueFloat
                val voltage = adcValue * (5.0 / 1023.0) // ADC 값을 전압으로 변환
                val dustConcentration = 0.5 * voltage * 1000 // 전압을 µg/m³로 변환
                dustvalueView.text = String.format("%.2f ug/m³", dustConcentration) // 변환된 값을 TextView에 설정
            } else {
                dustvalueView.text = dustValue.toString()
            }
        })

        val unitConvCheckBox = findViewById<CheckBox>(R.id.unitconv)

        unitConvCheckBox.setOnCheckedChangeListener { _, isChecked ->
            isUnitConverted = isChecked // 체크박스 상태 업데이트
            val dustValue = sensorViewModel.dustValue.value?.toFloatOrNull() ?: 0f
            if (isChecked) {
                // CheckBox가 체크되었을 때: dustValue를 µg/m³로 변환
                val adcValue = dustValue // dustValue가 ADC 값이라고 가정
                val voltage = adcValue * (5.0 / 1023.0) // ADC 값을 전압으로 변환
                val dustConcentration = 0.5 * voltage * 1000 // 전압을 µg/m³로 변환
                dustvalueView.text = String.format("%.2f ug/m³", dustConcentration) // 변환된 값을 TextView에 설정
            } else {
                // CheckBox가 체크 해제되었을 때 원래의 dustValue를 표시
                dustvalueView.text = dustValue.toString()
            }
        }

        // EnvPoint 결과 계산
        val envScore = calculateEnvPoint(
            sensorViewModel.temperature.value?.toFloatOrNull() ?: 0f,
            sensorViewModel.humidity.value?.toFloatOrNull() ?: 0f,
            sensorViewModel.dustValue.value?.toFloatOrNull() ?: 0f
        )
        findViewById<TextView>(R.id.envpoint).text = "$envScore 점"

        // 점수에 따라 표정 이미지 설정
        EnvExpression(envScore)

        // 온도, 습도, 미세먼지 값에 따라 조언을 생성하고 설정
        val EnvAdvice = setAdvice(
            sensorViewModel.temperature.value?.toFloatOrNull() ?: 0f,
            sensorViewModel.humidity.value?.toFloatOrNull() ?: 0f,
            sensorViewModel.dustValue.value?.toFloatOrNull() ?: 0f
        )
        findViewById<TextView>(R.id.advice).text = EnvAdvice
    }

    // 실내 환경 점수를 계산하는 함수
    private fun calculateEnvPoint(temperature: Float, humidity: Float, dustValue: Float): Int {
        val temperaturePoint = resultTemperaturePoint(temperature)
        val humidityPoint = resultHumidityPoint(humidity)
        val dustPoint = resultDustPoint(dustValue)

        return (temperaturePoint * 0.4 + humidityPoint * 0.3 + dustPoint * 0.3).toInt()
    }

    // 온도 점수 계산
    private fun resultTemperaturePoint(temperature: Float): Int {
        return when {
            temperature in 20.0..22.0 -> 100
            temperature < 20 -> (100 - (20 - temperature) * 5).toInt().coerceIn(0, 100)
            else -> (100 - (temperature - 22) * 5).toInt().coerceIn(0, 100)
        }
    }
    // 습도 점수 계산
    private fun resultHumidityPoint(humidity: Float): Int {
        return when {
            humidity in 40.0..60.0 -> 100
            humidity < 40 -> (100 - (40 - humidity) * 2.5).toInt().coerceIn(0, 100)
            else -> (100 - (humidity - 60) * 2.5).toInt().coerceIn(0, 100)
        }
    }
    // 미세먼지 점수 계산
    private fun resultDustPoint(dustValue: Float): Int {
        return when {
            dustValue <= 12 -> 100
            dustValue <= 35.4 -> (100 - (dustValue - 12) * 2).toInt().coerceIn(0, 100)
            dustValue <= 55.4 -> (100 - (dustValue - 35.4) * 3).toInt().coerceIn(0, 100)
            else -> 0
        }
    }

    // 환경 점수에 따라 표정 변경
    private fun EnvExpression(envScore: Int) {
        val expressionImageView = findViewById<ImageView>(R.id.expression)
        val imageResource = when (envScore) {
            in 90..100 -> R.drawable.verygood
            in 70..89 -> R.drawable.good
            in 50..69 -> R.drawable.soso
            in 30..49 -> R.drawable.bad
            else -> R.drawable.worst
        }
        expressionImageView.setImageResource(imageResource)
    }

    // 온도, 습도, 미세먼지 값을 바탕으로 조언을 생성하는 함수
    private fun setAdvice(temperature: Float, humidity: Float, dustValue: Float): String {
        val adviceList = mutableListOf<String>()

        // 온도 조언
        if (temperature < 20) {
            adviceList.add("현재 온도가 조금 낮아요. 따뜻하게 온풍기를 켜보세요!")
        } else if (temperature > 22) {
            adviceList.add("지금 온도가 높아요. 선풍기로 시원하게 만들어 보세요!")
        }

        // 습도 조언
        if (humidity < 40) {
            adviceList.add("습도가 낮네요. 가습기를 켜서 촉촉하게 유지하세요.")
        } else if (humidity > 60) {
            adviceList.add("습도가 높아요. 선풍기로 환기시켜보세요.")
        }

        // 미세먼지 조언
        if (dustValue > 35.4) {
            adviceList.add("미세먼지 농도가 높아요. 공기청정기를 켜서 공기를 맑게 해주세요.")
        }

        return if (adviceList.isEmpty()) {
            "실내 환경이 쾌적해요! 계속 이렇게 유지하세요."
        } else {
            adviceList.joinToString("\n\n")
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