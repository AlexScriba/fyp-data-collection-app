package com.example.datacollectionappv1

import android.content.Context
import android.hardware.SensorManager

class SensorInitData(val name: String, val type: Int)

interface DataManager {
    val numSessionsDone: Int

    fun startRecording()
    fun stopRecording(save: Boolean = true)
    fun saveHistoryCsv(context: Context)
}

