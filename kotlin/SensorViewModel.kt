package com.example.smartenv

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SensorViewModel : ViewModel() {
    val temperature = MutableLiveData<String>()
    val humidity = MutableLiveData<String>()
    val dustValue = MutableLiveData<String>()
}