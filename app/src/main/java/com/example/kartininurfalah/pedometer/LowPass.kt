package com.example.kartininurfalah.pedometer

/*
* Low Pass Filter
*
* @author Kartini
* */
/*
    * time smoothing constant for low-pass filter 0 <= alpha 1 ; a smaller
    * value basically means more smoothing     *
    * */

object LowPassFilter {

    private val ALPHA = 0.2f

    /*fun filter(input: FloatArray, output: FloatArray?): FloatArray {
        if (output == null)
            return input

        for (i in input.indices) {
            output[i] = output[i] + ALPHA * input[i] - output[i]
        }
        return output
    }*/
    fun filter(input: FloatArray, output: FloatArray?, size: Int): FloatArray {
        if (output == null)
            return input

        for (i in 0 until size) {
            output[i] = output[i] + ALPHA * input[i] - output[i]
        }
        return output
    }
}
