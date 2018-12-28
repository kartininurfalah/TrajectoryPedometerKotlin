package com.example.kartininurfalah.pedometer.utils

import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors

var helloRunnable: Runnable = Runnable { println("Hello world") }

var executor = Executors.newScheduledThreadPool(1)
//executor.scheduleAtFixedRate(helloRunnable, 0, 3, TimeUnit.SECONDS)