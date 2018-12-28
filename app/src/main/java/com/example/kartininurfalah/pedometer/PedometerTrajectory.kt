package com.example.kartininurfalah.pedometer

class PedometerTrajectory(val z: Double, val angle: Double, val steps: Int)

//class PedometerTrajectory(val position: Position, val accelerometer: AccelerometerCalibration,
//                          val angel: Double, val threshold: Double, val svm: Float, val steps: Int)

class AccelerometerTrajectory (val accelerometer: AccelerometerCalibration,
                          val angel: Double, val threshold: Double, val svm: Float, val steps: Int)