package com.example.datacollectionappv1

import android.content.Context
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Runnable

val DEFAULT_SAVE_DIR = "Recordings"

/**
 * TODO : add cleanup function to remove listeners from sensors
 * TODO : add isRecording to be able to check if it is recording or not (also look into if there
 *        are any issues with pressing stop or start when it is already stopped or started
 */
class DataManagerInterval (
        sensorManager: SensorManager,
        private val personName: String,
        private val sessionNumber: String,
        dataPerSecond: Int,
        initData: Array<SensorInitData>,
) : DataManager {
    private var sensors: MutableMap<Int, SensorDataInterval> = mutableMapOf()

    private val milliDelay: Long = 1000 / dataPerSecond.toLong()

    override val numSessionsDone : Int get () { return this.getArchiveSizes()[0] }

    /** Setup for repeatedly saving data */
    private var mainHandler: Handler = Handler(Looper.getMainLooper())
    private val recordDataLoop =
            object : Runnable {
                override fun run() {
                    // save data snapshot of current sensor data
                    sensors.forEach { (_, sensor) -> sensor.recordCurrentData() }

                    mainHandler.postDelayed(this, milliDelay)
                }
            }

    /** Init data per sensor and attach listeners for each sensor */
    init {
        val retSensors = mountSensors(sensorManager, initData)
        sensors = retSensors
    }


    /** Start recording data from all sensors */
    override fun startRecording() {
        sensors.forEach { (_, sensor) -> sensor.resetTimeStamp() }
        mainHandler.post(recordDataLoop)
    }

    /**
     * Stop recording data from all sensors Can save history data after recording or simply clear
     * data
     */
    override fun stopRecording(save: Boolean) {
        mainHandler.removeCallbacks(recordDataLoop)

        if (save) {
            sensors.forEach { (_, sensor) -> sensor.saveAndClear() }
        } else {
            sensors.forEach { (_, sensor) -> sensor.clear() }
        }
    }

    private fun getArchiveSizes(): List<Int> {
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

    private fun mountSensors(
        sensorManager: SensorManager,
        initData: Array<SensorInitData>,
        ): MutableMap<Int, SensorDataInterval> {
        val sensors: MutableMap<Int, SensorDataInterval> = mutableMapOf()

        for (sData in initData) {
            val sensorData =
                SensorDataInterval(
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
                    SensorManager.SENSOR_DELAY_FASTEST,
//                    SensorManager.SENSOR_DELAY_UI
                )
            }
        }

        return sensors
    }
}


