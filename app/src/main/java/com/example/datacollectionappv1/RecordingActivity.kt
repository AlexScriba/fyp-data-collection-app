package com.example.datacollectionappv1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class RecordingActivity : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var dataManager: DataManager
//    private lateinit var dataManager: DataManagerConstant
    private var vibrator: Vibrator? = null

    private lateinit var txtMessage: TextView
    private lateinit var txtNumRecordings: TextView
    private lateinit var txtCountdown: TextView
    private lateinit var txtName: TextView

    private lateinit var btnPause: Button
    private lateinit var btnFinish: Button

    private var name: String = "None"
    private var sessionNumber: String = "None"
    private var recDur: Int = -1
    private var numRec: Int = -1
    private var dataPerSecond: Int = -1
    private var recordingDelay: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recording_screen)

        /** Get values passed from start screen for init */
        val pName = intent.getStringExtra("name")
        val pRecDur = intent.getIntExtra("recDur", -1)
        val pNumRec = intent.getIntExtra("numRec", -1)
        val pDataPerSecond = intent.getIntExtra("dataPerSecond", -1)
        val pRecordingDelay = intent.getIntExtra("recordingDelay", -1)
        val pSessionNumber = intent.getStringExtra("sessionNumber")

        /** Validate data received from previous activity */
        if (pName == null) {
            errorClose("No name passed to Screen.")
            return
        }
        if (pRecDur == -1) {
            errorClose("No recording duration passed to Screen.")
            return
        }
        if (pNumRec == -1) {
            errorClose("No number of recordings passed to Screen.")
            return
        }
        if (pDataPerSecond == -1) {
            errorClose("No data per second rate passed to Screen.")
            return
        }
        if (pRecordingDelay == -1) {
            errorClose("No recording delay passed to Screen.")
            return
        }
        if(pSessionNumber == null) {
            errorClose("No session number passed")
            return
        }

        name = pName
        recDur = pRecDur
        numRec = pNumRec
        dataPerSecond = pDataPerSecond
        recordingDelay = pRecordingDelay
        sessionNumber = pSessionNumber

        /** Get UI elements */
        txtMessage = findViewById(R.id.txt_message)
        txtNumRecordings = findViewById(R.id.txt_disp_num_rec)
        txtCountdown = findViewById(R.id.txt_countdown)
        txtName = findViewById(R.id.txt_disp_name)

        btnFinish = findViewById(R.id.btn_finish)
        btnPause = findViewById(R.id.btn_pause)

        /** Init sensorManager and sensors with listeners */
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        val sensorSelection = arrayOf(
            SensorInitData("Accelerometer", Sensor.TYPE_ACCELEROMETER),
            SensorInitData("Gyroscope", Sensor.TYPE_GYROSCOPE),
            SensorInitData("Gravity", Sensor.TYPE_GRAVITY),
            SensorInitData("Rotation", Sensor.TYPE_ROTATION_VECTOR),
        )

//        dataManager =
//                DataManagerInterval(
//                        sensorManager,
//                        name,
//                        sessionNumber,
//                        dataPerSecond,
//                        sensorSelection,
//                )

        dataManager =
                DataManagerConstant(
                    sensorManager,
                    name,
                    sessionNumber,
                    sensorSelection
                )

        /**
         * Init vibration
         */
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        /**
         * Init event listeners for buttons
         */
        btnPause.setOnClickListener {
            this.buttonStart()
        }

        btnFinish.setOnClickListener {
            this.buttonBack()
        }

        // Start session (temp for now)
        txtName.text = name
        displayMessage("Press start to begin.")
        updateStats()
    }

    /**
     * sequences for recording process
     */
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private val recordSequence: Runnable = Runnable {
        this.startRecording()
        mainHandler.postDelayed(pauseSequence, (recDur * 1000).toLong())

        updateStats()
    }
    private val pauseSequence: Runnable = Runnable {
        this.stopRecording()

        if(dataManager.numSessionsDone >= numRec) {
            updateStats()
            onFinishSession()
            return@Runnable
        }

        mainHandler.postDelayed(recordSequence, (recordingDelay * 1000).toLong())

        updateStats()
    }

    private fun cancelSequences() {
        mainHandler.removeCallbacks(recordSequence)
        mainHandler.removeCallbacks(pauseSequence)
    }

    /**
     * Function to handle UI and start recording
     */
    private fun startRecording() {
        // do UI stuff here
        displayMessage("Recording")
        runAfterCountdown(recDur) {}
        pingUser()

        // start recording on data manager
        dataManager.startRecording()
    }

    /**
     * Function to handle UI and stop recording
     */
    private fun stopRecording() {
        // stop recording on data manager
        dataManager.stopRecording()
        pingUser()

        // do UI stuff here
        displayMessage("Please Reset")
        runAfterCountdown(recordingDelay) {}
    }

    /**
     * Function to handle logic once recording session is finished
     * Handle UI and Saving Data
     */
    private fun onFinishSession() {
        cancelSequences()
        stopCountdown()

        // save session data
        displayMessage("Saving...")
        dataManager.saveHistoryCsv(this)


        var message = "Done Recording: (${dataManager.numSessionsDone} sessions saved)\n"

        val filePath = File(this.filesDir, DEFAULT_SAVE_DIR).absolutePath
        message += filePath

        displayMessage(message)
        btnPause.isEnabled = false

        btnFinish.setText(R.string.txt_btn_back)
        btnFinish.setOnClickListener { buttonBack() }

    }

    /**
     * On Click handler for start button
     */
    private fun buttonStart() {
        btnPause.setText(R.string.txt_btn_pause)
        btnPause.setOnClickListener { buttonPause() }

        btnFinish.setText(R.string.txt_btn_finish)
        btnFinish.setOnClickListener { buttonFinish() }

        // Start session (temp for now)
        displayMessage("Starting recording in:")
        runAfterCountdown(recordingDelay) { mainHandler.post(recordSequence) }
    }

    /**
     * On Click handler for pause button
     */
    private fun buttonPause() {
        stopCountdown()
        cancelSequences()

        btnPause.setText(R.string.txt_btn_pause_start)
        btnPause.setOnClickListener { buttonStart() }

        displayMessage("Session Paused.\nClick Start to resume recording.")
    }

    /**
     * On Click handler for finish button
     */
    private fun buttonFinish() {
        dataManager.stopRecording(false)

        this.onFinishSession()

        btnFinish.setOnClickListener { this.buttonBack() }
        btnFinish.setText(R.string.txt_btn_back)
    }

    /**
     * On Click handler for back button
     */
    private fun buttonBack() {
        finish()
    }

    /**
     * Function to handle displaying information to user
     * Displays number of recordings to user
     */
    private fun updateStats() {
        txtNumRecordings.text = dataManager.numSessionsDone.toString()
    }

    /**
     * Function to display a message to a text view in UI
     */
    private fun displayMessage(msg: String) {
        txtMessage.text = msg
    }

    /**
     * Function to display an error and stop looper threads
     */
    private fun errorClose(msg: String = "An error occurred.", delay: Int = 0) {
        Toast.makeText(this, "Recording Error: $msg", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({ finish() }, delay.toLong())
    }

    /**
     * Returns long representing input seconds
     */
    private fun seconds(s: Int) : Long {
        return (s * 1000).toLong()
    }

    /**
     * Handle countdowns
     */
    private var activeCountdown: CountDownTimer? = null

    private fun runAfterCountdown(time: Int, func: () -> Unit) {
        activeCountdown?.cancel()

        txtCountdown.text = time.toString()

        activeCountdown = object : CountDownTimer(seconds(time), time.toLong()) {
            override fun onTick(millisUntilFin: Long) {
                txtCountdown.text = (millisUntilFin/1000 + 1).toString()
            }
            override fun onFinish() { func() }
        }
        activeCountdown?.start()
    }

    private fun stopCountdown() {
        activeCountdown?.cancel()
        txtCountdown.text = ""
    }

    /**
     * Handle pinging user
     * Through Vibration and Sound
     */
    private fun pingUser(vibrate: Boolean = true, sound: Boolean = true) {
        if(vibrate) buzzVibrator()
        if(sound) playSound()
    }

    private fun buzzVibrator(duration: Long = 400) {
        if(Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator?.vibrate(duration)
        }
    }

    private fun playSound(){
        val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r: Ringtone = RingtoneManager.getRingtone(applicationContext, notification)
        r.play()
    }

}
