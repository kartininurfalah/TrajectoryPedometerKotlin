package com.example.kartininurfalah.pedometer

import android.content.Context
import android.hardware.*
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import com.example.kartininurfalah.pedometer.listener.StepListener
import com.example.kartininurfalah.pedometer.utils.StepDetector
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SensorEventListener, StepListener {

    private var simpleStepDetector: StepDetector? = null
    //sensor manager
    private var sensorManager: SensorManager? = null
    //Gravity for accelerometer data
    private val gravity = FloatArray(3)
    //magnetic data
    private val geomagnetic = FloatArray(3)
    //rotation data
    private val rotation = FloatArray(9)
    //orientatiomn (azimuth, pitch, roll)
    private val orientation = FloatArray(3)
    //smoothed values
    private var smoothed = FloatArray(3)
    //sensor accelerometer, gyroscop, magnetic field, barometer
    private val pressureBarometer = FloatArray(1)
    private var accelerometer: Sensor? = null
    private var mMagno: Sensor? = null
    private var mPressure: Sensor? = null

    private val geomagneticField: GeomagneticField? = null
    private var bearing = 0.0

    //textview
    lateinit var xAccelValue: TextView
    lateinit var yAccelValue: TextView
    lateinit var zAccelValue: TextView
    lateinit var xMagnoValue: TextView
    lateinit var yMagnoValue: TextView
    lateinit var zMagnoValue: TextView
    lateinit var pressureValue: TextView
    lateinit var timerValue: TextView
    private var textDirection: TextView? = null

    //IMAGE
    lateinit var imageCompassView: ImageView
    private var compassView: CompassView? = null
    //array SVM
    private val SVM = arrayOf<Float>()
    fun kuadrat(x: Float, y: Float, z: Float) {
        val sum = (x*x) + (y*y) + (z*z)
    }
    //pedometer
    private var startTime = 0L
    internal var timeInMiliseconds = 0L
    internal var timeSwapBuff = 0L
    internal var updatedTime = 0L
    internal var elapsedTime: Long = 0
    private val REFRESH_RATE = 100
    private val hours: String? = null
    val minutes: String? = null
    val seconds: String? = null
    val milliseconds: String? = null
    var batas: Float = 0.toFloat()
    var hasil: String? = null
    lateinit var mySpinner: Spinner
    private var started = false

    internal var interval = 500 // 1000ms
    internal var flag = false
    internal var handler: Handler? = null


    //sensor gravity
    private val sensorGravity: Sensor? = null
    private val TEXT_NUM_STEPS = "Number of Steps: "
    private var numSteps: Int = 0

    private val TAG = "MainActivity"
    val NA = "N/A"
    val FIXED = "FIXED"


    private val processSensors = object : Runnable {
        override fun run() {

            flag = true
            handler!!.postDelayed(this, interval.toLong())
        }
    }
    private val updateTimerThread = object : Runnable {
        override fun run() {


            timeInMiliseconds = SystemClock.uptimeMillis() - startTime
            updatedTime = timeSwapBuff + timeInMiliseconds
            var secs = (updatedTime / 1000).toInt()
            val min = secs / 60
            secs = secs % 60
            val miliseconds = (updatedTime % 1000).toInt()
            timerValue.setText("" + min + ":"
                    + String.format("%02d", secs) + ":"
                    + String.format("%03d", miliseconds))
            handler!!.postDelayed(this, 0)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        if (p0!!.getType() == Sensor.TYPE_MAGNETIC_FIELD && p1 == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            //manage fact that compass data are unreliable...
            //toast ? display on screen ?
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        /**/
        var accelOrMagnetic = false

        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector!!.updateAccelerometer(event.timestamp, event.values[0], event.values[1], event.values[2])

             //we need to use a low pass filter to make data smoothed
            smoothed = LowPassFilter.filter(event.values, gravity)
            gravity[0] = smoothed[0]
            gravity[1] = smoothed[1]
            gravity[2] = smoothed[2]

            accelOrMagnetic = true
            Log.d(TAG, "onSensorChanged: X: " + event.values[0] + "Y: " + event.values[1] + "Z: " + event.values[2])

            xAccelValue.text = "xAccelValue: " + event.values[0]
            yAccelValue.text = "yAccelValue: " + event.values[1]
            zAccelValue.text = "zAccelValue: " + event.values[2]
        } else if (event!!.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            smoothed = LowPassFilter.filter(event.values, geomagnetic)
            geomagnetic[0] = smoothed[0]
            geomagnetic[1] = smoothed[1]
            geomagnetic[2] = smoothed[2]
            accelOrMagnetic = true

        }else if (event!!.sensor.type == Sensor.TYPE_PRESSURE) {
            smoothed = LowPassFilter.filter(event.values, pressureBarometer)
            pressureBarometer[0] = smoothed[0]

            Log.d(TAG, "onSensorChanged: Pressure: " + event.values[0])

            pressureValue.text = "Pressure: " + event.values[0]
        }

        //get rotation matrix to get gravity and magnetic data
        SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)
        //get bearing to target
        SensorManager.getOrientation(rotation, orientation)
        // east degrees of true North
        bearing = orientation[0].toDouble()
        //convert from radians ti degrees
        bearing = Math.toDegrees(bearing)

        // fix different between true North and Magnetical North
        if (geomagneticField != null) {
            bearing += geomagneticField.declination.toDouble()
        }

        //bearing must be in 0-360
        if (bearing < 0) {
            bearing += 360.0
        }

        //update compass view
        compassView!!.setBearing(bearing.toFloat())

        if (accelOrMagnetic) {
            compassView!!.postInvalidate()
        }
        updateTextDirection(bearing)

    }

    private fun updateTextDirection(bearing: Double) {
        val range = (bearing / (360f / 16f)).toInt()
        var dirTxt = ""

        if (range == 15 || range == 0)
            dirTxt = "N"
        if (range == 1 || range == 2)
            dirTxt = "NE"
        if (range == 3 || range == 4)
            dirTxt = "E"
        if (range == 5 || range == 6)
            dirTxt = "SE"
        if (range == 7 || range == 8)
            dirTxt = "S"
        if (range == 9 || range == 10)
            dirTxt = "SW"
        if (range == 11 || range == 12)
            dirTxt = "W"
        if (range == 13 || range == 14)
            dirTxt = "NW"

        textDirection!!.setText("" + bearing.toInt() + 176.toChar() + "" + dirTxt) // char 176  = degrees

    }

    override fun step(timeNs: Long) {
        numSteps++
        tvSteps.text = TEXT_NUM_STEPS.plus(numSteps)
    }

   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

       //handler
        handler = Handler()

       // Get an instance of the SensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        simpleStepDetector = StepDetector()
        simpleStepDetector!!.registerListener(this)

        // keep screen light on (wake lock light)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

       //compas
       //display the degree of compass
        textDirection = findViewById(R.id.textCompas) as TextView
        compassView = findViewById(R.id.imageCompas) as CompassView

       //waktu
        timerValue = findViewById(R.id.waktu) as TextView
        mySpinner = findViewById(R.id.threshold_spinner) as Spinner

       //accelerometer
        xAccelValue = findViewById(R.id.xAccelValue) as TextView
        yAccelValue = findViewById(R.id.yAccelValue) as TextView
        zAccelValue = findViewById(R.id.zAccelValue) as TextView
        xMagnoValue = findViewById(R.id.xMagnoValue) as TextView
        yMagnoValue = findViewById(R.id.yMagnoValue) as TextView
        zMagnoValue = findViewById(R.id.zMagnoValue) as TextView
        pressureValue = findViewById(R.id.pressure) as TextView

        if (accelerometer != null) {
            sensorManager!!.registerListener(this@MainActivity, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "onCreate: Registered accelerometer listener")
        } else {
            xAccelValue.text = "Accelerometer not suported"
            yAccelValue.text = "Accelerometer not suported"
            zAccelValue.text = "Accelerometer not suported"
        }

        mMagno = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (mMagno != null) {
           sensorManager!!.registerListener(this@MainActivity, mMagno, SensorManager.SENSOR_DELAY_NORMAL)
           Log.d(TAG, "onCreate: Registered Magnometer listener")
        } else {
            xMagnoValue.setText("Magnometer not suported")
            yMagnoValue.setText("Magnometer not suported")
            zMagnoValue.setText("Magnometer not suported")
        }

        mPressure = sensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (mPressure != null) {
            sensorManager!!.registerListener(this@MainActivity, mPressure, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "onCreate: Registered Pressure listener")
        } else {
            pressureValue.text = "Pressure not suported"
        }

        btnStart.setOnClickListener(View.OnClickListener {
            val i = 1
            started = true
            numSteps = 0
            hasil = mySpinner.selectedItem.toString()
            Log.d(TAG,"hasil: " + hasil)
            batas = java.lang.Float.parseFloat(hasil)
            startTime = SystemClock.uptimeMillis()
            //SVM[i] = Math.sqrt(kuadrat(x = xAccelValue, y = yAccelValue, z = zAccelValue))
            handler!!.removeCallbacks(updateTimerThread)
            handler!!.postDelayed(updateTimerThread, 0)
            sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST)
            sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_FASTEST)
        })

        btnStop.setOnClickListener(View.OnClickListener {
            timeSwapBuff += timeInMiliseconds
            handler!!.removeCallbacks(updateTimerThread)
            started = false
            sensorManager!!.unregisterListener(this)
        })
    }

}