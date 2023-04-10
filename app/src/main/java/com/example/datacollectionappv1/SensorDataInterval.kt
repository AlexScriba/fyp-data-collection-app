package com.example.datacollectionappv1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.SystemClock
import java.io.File
import java.io.FileOutputStream

class DataPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val timeStamp: Long
) {
    val dataAsCSV: String get () { return "${x},${y},${z},${timeStamp}" }
}

/**
 * SensorData class that records data at set intervals
 */
class SensorDataInterval (
    val name: String,
    val type: Int,

    private var _x : Float = 0f,
    private var _y: Float = 0f,
    private var _z: Float = 0f,
    private var timeStamp: Long = 0,

)  : SensorEventListener {

    /**
     * Timestamp to record data points with timestamp since recording start
     */
    private var initTimestamp: Long = SystemClock.elapsedRealtimeNanos()

    /**
     * DataStructures to store recording history
     */
    private var history: MutableList<DataPoint> = mutableListOf()
    private val archive: MutableList<MutableList<DataPoint>> = mutableListOf()

    val numPointsInCurrentHistory: Int get () {return history.size}
    val numRecordingsInArchive: Int get () {return archive.size}

    /**
     * Sensor value accessors
     */
    val x: Float get () {return _x}
    val y: Float get () {return _y}
    val z: Float get () {return _z}

    /**
     * Set values of sensor
     */
    private fun setValues(tx: Float, ty: Float, tz: Float, ts: Long) {
        this._x = tx
        this._y = ty
        this._z = tz
        this.timeStamp = ts
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != this.type) return

        val tx = event.values[0]
        val ty = event.values[1]
        val tz = event.values[2]
        val ts = event.timestamp - initTimestamp

        this.setValues(tx, ty, tz, ts)
    }

    fun resetTimeStamp () {
        this.initTimestamp = SystemClock.elapsedRealtimeNanos()
    }

    /**
     * Saves current sensor readings to history as a datapoint
     */
    fun recordCurrentData () {
        val currentTimestamp = SystemClock.elapsedRealtimeNanos() - initTimestamp
        this.history.add(DataPoint(_x, _y, _z, currentTimestamp))
    }

    /**
     * Saves history of current recording and Resets values
     */
    fun saveAndClear() {
        archive.add(history)
        this.clear()
    }

    /**
     * Clears history without saving data to archive
     */
    fun clear() {
        history = mutableListOf()

        this.setValues(0f,0f, 0f, 0)
        initTimestamp = SystemClock.elapsedRealtimeNanos()
    }

    override fun toString(): String {
        return "${name}:\nx: ${x}\ny: ${y}\nz: $z\nts: $timeStamp"
    }

    /**
     * Convert each recording Data to CSV format and save in a file
     */
    fun saveSensorCsv(context: Context, rootDir: String, personName: String, personSession: String) {
        val fileHeaders = "x,y,z,timeStamp\n"

        val filePath = "${rootDir}/${this.name}"
        val storeFolder = File(context.filesDir, filePath).also {
            it.mkdirs()
        }

        archive.forEachIndexed { index, recording ->
            val fileName = "${personName}_${personSession}_${index}.csv"

            val file = File(storeFolder, fileName)
            file.deleteRecursively()

            val fileContents = recording
                .map { point -> point.dataAsCSV }
                .fold(fileHeaders) {body, line -> body + "${line}\n"}

            FileOutputStream(file).use {
                it.write(fileContents.toByteArray())
            }
        }

    }
}