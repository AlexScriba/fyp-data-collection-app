package com.example.datacollectionappv1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private val DEFAULT_NUM_RECORDING = 10
    private val DEFAULT_DURATION = 7
    private val DEFAULT_RECORDING_DELAY = 5
    // max speed seems to be 60 points per second
    private val DEFAULT_DATA_PER_SECOND = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * Setting default values to input field
         */

        findViewById<TextInputEditText>(R.id.inp_num_records).setText(DEFAULT_NUM_RECORDING.toString())
        findViewById<TextInputEditText>(R.id.inp_record_duration).setText(DEFAULT_DURATION.toString())
//        findViewById<TextInputEditText>(R.id.inp_data_per_second).setText(DEFAULT_DATA_PER_SECOND.toString())
        findViewById<TextInputEditText>(R.id.inp_recording_delay).setText(DEFAULT_RECORDING_DELAY.toString())

        /**
         * Set up start button
         */
        val btnStart = findViewById<Button>(R.id.btn_start)
        btnStart.setOnClickListener {
            startRecording()
        }
    }

    /**
     * Open new screen to start recording
     * Passes values from text fields to new page
     */
    private fun startRecording() {
        val name = findViewById<TextInputEditText>(R.id.inp_name).text.toString()
        val numRec = findViewById<TextInputEditText>(R.id.inp_num_records).text.toString()
        val recDur = findViewById<TextInputEditText>(R.id.inp_record_duration).text.toString()
        val dataPerSecond = "60"
        val recordingDelay = findViewById<TextInputEditText>(R.id.inp_recording_delay).text.toString()
        val sessionNumber = findViewById<TextInputEditText>(R.id.inp_session_number).text.toString()

        if(isValid(name, numRec, recDur, dataPerSecond, recordingDelay, sessionNumber)) {
            startActivity(Intent(this, RecordingActivity::class.java).apply {
                putExtra("name", name)
                putExtra("recDur", recDur.toInt())
                putExtra("numRec", numRec.toInt())
                putExtra("dataPerSecond", dataPerSecond.toInt())
                putExtra("recordingDelay", recordingDelay.toInt())
                putExtra("sessionNumber", sessionNumber)
            })
        } else {
            Toast.makeText(
                this,
                "Please make sure all fields are filled in and valid.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Validate user inputs
     */
    private fun isValid(name: String, numRec: String, recDur: String, dataPerSecond: String, recording: String, sessionNumber: String): Boolean{
        if(name == "") return false
        if(numRec == "" || numRec.toIntOrNull() == null) return false
        if(recDur == "" || recDur.toIntOrNull() == null) return false
        if(dataPerSecond == "" || dataPerSecond.toIntOrNull() == null) return false
        if(recording == "" || recording.toIntOrNull() == null) return false
        if(sessionNumber == "") return false

        return true
    }
}