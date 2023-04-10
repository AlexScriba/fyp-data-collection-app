package com.example.datacollectionappv1

import android.content.Context
import android.hardware.SensorManager

/**
 * Data Manager Class that uses SensorDataConstant to record data
 */
class DataManagerConstant(
    private val sensorManager: SensorManager,
    private val personName: String,
    private val sessionNumber: String,

    initData: Array<SensorInitData>,
) : DataManager {
    /**
     * Map to store each sensors SensorData class
     */
    private var sensors: MutableMap<Int, SensorDataConstant> = mutableMapOf()

    override val numSessionsDone : Int get () { return this.getArchiveSizes()[0] }

    init {
        sensors = mountSensors(sensorManager, initData)
    }

    override fun startRecording() {
        sensors.forEach{ (_, sensor) -> sensor.resetTimeStamp() }
        sensors.forEach{ (_, sensor) -> sensor.startRecording() }
    }

    override fun stopRecording(save: Boolean) {
        sensors.forEach { (_, sensor) -> sensor.stopRecording() }

        if(save){
            sensors.forEach { (_, sensor) -> sensor.saveAndClear() }
        } else {
            sensors.forEach { (_, sensor) -> sensor.clear() }
        }
    }

    private fun getArchiveSizes() : List<Int> {
        return sensors.map { (_, sensor) -> sensor.numRecordingsInArchive }
    }

    override fun saveHistoryCsv(context: Context) {
        sensors.forEach { (_, sensor) -> sensor.saveSensorCsv(
            context,
            DEFAULT_SAVE_DIR,
            personName,
            sessionNumber
        )
        }
    }

    /**
     * Create SensorData instances and add them to sensors as event listeners
     */
    private fun mountSensors(
        sensorManager: SensorManager,
        initData: Array<SensorInitData>,
    ) : MutableMap<Int, SensorDataConstant> {
        val sensors: MutableMap<Int, SensorDataConstant> = mutableMapOf()

        for (sData in initData) {
            val sensorData =
                SensorDataConstant(
                    sData.name,
                    sData.type,
                    0.0f,
                    0.0f,
                    0.0f,
                    timeStamp = 0,
                )

            sensors[sData.type] = sensorData

            sensorManager.getDefaultSensor(sData.type)?.also { sensor ->
                sensorManager.registerListener(
                    sensorData,
                    sensor,
                    android.hardware.SensorManager.SENSOR_DELAY_FASTEST,
//                    android.hardware.SensorManager.SENSOR_DELAY_UI
                )
            }
        }

        return sensors
    }
}
