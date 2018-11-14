package com.example.kartininurfalah.pedometer.utils

import com.example.kartininurfalah.pedometer.listener.StepListener
import kotlin.math.sqrt

class SVMStepDetector {
    var lastStepNS: Long = 0
    var TIME_NS: Long = 1000000000
    var THRESHOLD = 5f

    var listener:StepListener? = null

    fun registerListener(stepListener: StepListener) {
        listener = stepListener
    }

    fun calculateSVM(x: Float, y: Float, z: Float): Float {
        return sqrt((x*x) + (y*y) + (z*z))
    }

    fun updateAccelerometer(time: Long, x: Float, y: Float, z: Float) {
        val svm = calculateSVM(x, y, z)
        if (time > lastStepNS + TIME_NS && svm >= THRESHOLD) {
            listener?.step(time)
            lastStepNS = time
        }
    }
}