package com.example.kartininurfalah.pedometer

import android.content.Context
import android.hardware.*
import android.hardware.SensorManager.getAltitude
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.widget.*
import com.example.kartininurfalah.pedometer.listener.StepListener
import com.example.kartininurfalah.pedometer.utils.SVMStepDetector
import com.example.kartininurfalah.pedometer.utils.StepDetector
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executor
import java.util.concurrent.Executors

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
    var hasil: String? = null
//    lateinit var mySpinner: Spinner
    private var mySpinner: EditText? = null

    private var started = false

    internal var interval = 1000 // 1000ms
    internal var flag = false
    internal var handler: Handler? = null

    //spinner
//    val threshold = mySpinner!!.text.toString()
    //sensor gravity
    private val sensorGravity: Sensor? = null
    private val TEXT_NUM_STEPS = "Number of Steps: "
    var numSteps: Int = 0

    private val TAG = "MainActivity"
    val NA = "N/A"
    val FIXED = "FIXED"

    val offset = 1.0
    var x = 0.0
    var y = 0.0
    var timeSave = 0
    var altitude = 0.0f

    //pedometer
    var currentPostion = Position(0.0, 0.0, 0.0)
    //accell
    var currentAccel = AccelerometerCalibration(0f, 0f, 0f)
    //calculate svm for define threshold
    var svm: Float = 0.0f

    fun calculateSVM(x: Float, y: Float, z: Float): Float {
        return sqrt((x*x) + (y*y) + (z*z))
    }
    //Pedometer
    val pedoRef = FirebaseDatabase.getInstance().getReference("pedometer")
    var pedoChild = pedoRef
    //AccelThreshold
    val pedoAccel = FirebaseDatabase.getInstance().getReference("AccelerometerThreshold")
    var pedoChildAccel = pedoAccel
    //All data pedometer and accelerometer threshold
    val pedoRefAll = FirebaseDatabase.getInstance().getReference("accelerometer_pedometer")
    var pedoChildAll = pedoRefAll

    private fun hoam() {
        System.out.println("Hello World!");
        System.out.println("Hello World!");
        System.out.println("Hello World!");
        System.out.println("Hello World!");
    }

    var helloRunnable: Runnable = Runnable { println("Hello world") }

    var executor = Executors.newScheduledThreadPool(1)
    override fun getMainExecutor(): Executor {
        return super.getMainExecutor()
    }


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

             //we need to use a low pass filter to make data smoothed
            smoothed = LowPassFilter.filter(event.values, gravity, 3)
            gravity[0] = smoothed[0]
            gravity[1] = smoothed[1]
            gravity[2] = smoothed[2]

            accelOrMagnetic = true
            Log.d(TAG, "onSensorChanged: X: " + event.values[0] + "Y: " + event.values[1] + "Z: " + event.values[2])

            xAccelValue.text = "xAccelValue: " + gravity[0]
            yAccelValue.text = "yAccelValue: " + gravity[1]
            zAccelValue.text = "zAccelValue: " + gravity[2]
            simpleStepDetector!!.updateAccelerometer(event.timestamp, gravity[0], gravity[1], gravity[2])

//            x = x + cos(90 - bearing)
//            y = y + sin(90 - bearing)

            svm = calculateSVM(gravity[0], gravity[1], gravity[2])

//            val newPosition = Position(0.0,0.0,0.0)
//            currentPostion = newPosition

            currentAccel = AccelerometerCalibration(gravity[0], gravity[1], gravity[2])
            val trajectory = AccelerometerTrajectory(currentAccel, bearing,
                    mySpinner!!.text.toString().toDouble(), svm , (numSteps) )

//            saveAccelerometer(trajectory)

        } else if (event!!.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            smoothed = LowPassFilter.filter(event.values, geomagnetic, 3)
            geomagnetic[0] = smoothed[0]
            geomagnetic[1] = smoothed[1]
            geomagnetic[2] = smoothed[2]
            accelOrMagnetic = true

        }else if (event!!.sensor.type == Sensor.TYPE_PRESSURE) {
            /*smoothed = LowPassFilter.filter(event.values, pressureBarometer)
            pressureBarometer[0] = smoothed[0]*/
            val s = LowPassFilter.filter(event.values, pressureBarometer, 1)
            pressureBarometer[0] = s[0]

            Log.d(TAG, "onSensorChanged: Pressure: " + pressureBarometer[0])
//            altitude = getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureBarometer[0]);
            pressureValue.text = "Pressure: " + pressureBarometer[0]
//            pressureValue.text = "Pressure: " + altitude
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

//    private fun ceil(x: Double): Double{
//        return ceil(x)
//    }

    override fun step(timeNs: Long) {

        svm = calculateSVM(gravity[0], gravity[1], gravity[2])
        val number:Double = x
        val number3digits:Double = String.format("%.3f", number).toDouble()
        val number2digits:Double = String.format("%.2f", number3digits).toDouble()
        val solution:Double = String.format("%.1f", number2digits).toDouble()

        x = solution

        val numberY:Double = y
        val number3digitsY:Double = String.format("%.3f", numberY).toDouble()
        val number2digitsY:Double = String.format("%.2f", number3digitsY).toDouble()
        val solutionY:Double = String.format("%.1f", number2digitsY).toDouble()

        y = solutionY

        val newPosition = Position(currentPostion.x + x, currentPostion.y + y, pressureBarometer[0].toDouble())
//        val newPosition = Position(currentPostion.x + x, currentPostion.y + y, altitude.toDouble())
        currentPostion = newPosition
//
//        val trajectory = PedometerTrajectory(newPostition, bearing, numSteps)
//
        val dataAccelerometer = AccelerometerCalibration(gravity[0], gravity[1], gravity[2])
        val trajectory = PedometerTrajectory(pressureBarometer[0].toDouble(), bearing, numSteps)
//        val trajectory = PedometerTrajectory(altitude.toDouble(), bearing, numSteps)

        savePedometer(trajectory)

        numSteps++
        x = x + cos(90  - bearing)
        y = y + sin(90 - bearing)

        tvSteps.text = TEXT_NUM_STEPS.plus(numSteps)

    }

    private fun savePedometer(position: PedometerTrajectory) {
        pedoChild.push().setValue(position).addOnFailureListener {
            it.printStackTrace()
            Toast.makeText(this, "DB Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAccelerometer(position: AccelerometerTrajectory) {
        pedoChildAll.push().setValue(position).addOnFailureListener {
            it.printStackTrace()
            Toast.makeText(this, "DB Failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSteps2.text = "x: " + currentPostion.x.toString() + " - y: " + currentPostion.y.toString()

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
        mySpinner = findViewById(R.id.threshold_spinner) as EditText

       //accelerometer
        xAccelValue = findViewById(R.id.xAccelValue) as TextView
        yAccelValue = findViewById(R.id.yAccelValue) as TextView
        zAccelValue = findViewById(R.id.zAccelValue) as TextView
        xMagnoValue = findViewById(R.id.xMagnoValue) as TextView
        yMagnoValue = findViewById(R.id.yMagnoValue) as TextView
        zMagnoValue = findViewById(R.id.zMagnoValue) as TextView
        pressureValue = findViewById(R.id.pressure) as TextView

//        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager!!.registerListener(this@MainActivity, accelerometer, SensorManager.SENSOR_DELAY_GAME)
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

        btnStart.setOnClickListener {
            val i = 1
            pedoChild = pedoRef.push()
//            pedoChildAccel = pedoAccel.push()
            pedoChildAll = pedoRefAll.push()
            started = true
            numSteps = 0
            val threshold = mySpinner!!.text.toString()
//            hasil = mySpinner.selectedItem.toString()
//            Log.d(TAG,"hasil: " + hasil)
            simpleStepDetector?.STEP_THRESHOLD = threshold.toFloat()

            currentPostion = Position(0.0, 0.0, 0.0)

            //SAVE DB
//            x = x + cos(90 - bearing)
//            y = y + sin(90 - bearing)
//            svm = calculateSVM(gravity[0], gravity[1], gravity[2])
//            val newPosition = Position(currentPostion.x + x, currentPostion.y + y, pressureBarometer[0].toDouble())
//            currentPostion = newPosition
//
//            val accelerometerThreshold = AccelerometerCalibration(gravity[0], gravity[1], gravity[2])
//            val trajectory = PedometerTrajectory(newPosition, accelerometerThreshold, bearing,
//                    mySpinner.selectedItem.toString().toDouble(), svm , (numSteps) )
//
//            savePedometer(trajectory)

            //START
            startTime = SystemClock.uptimeMillis()
            handler!!.removeCallbacks(updateTimerThread)
            handler!!.postDelayed(updateTimerThread, 0)
            sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME)
            sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL)
            sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL)
        }

        btnStop.setOnClickListener {
            tvSteps2.text = "x: " + currentPostion.x.toString() + " - y: " + currentPostion.y.toString()
            currentPostion = Position(0.0, 0.0, 0.0)
            x=0.0
            y=0.0
            timeSwapBuff += timeInMiliseconds
            handler!!.removeCallbacks(updateTimerThread)
            started = false
            sensorManager!!.unregisterListener(this)
        }

        btnReset.setOnClickListener {
            tvSteps2.text = "x: " + currentPostion.x.toString() + " - y: " + currentPostion.y.toString()
            currentPostion = Position(0.0, 0.0, 0.0)
            x=0.0
            y=0.0

            started = true
            updatedTime = 0L
            timeInMiliseconds = 0L
            timeSwapBuff = 0L
            startTime = SystemClock.uptimeMillis()
            (findViewById(R.id.waktu) as TextView).text = "00:00:00"
            tvSteps.text = TEXT_NUM_STEPS.plus("0")
            sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL)

        }
    }

}