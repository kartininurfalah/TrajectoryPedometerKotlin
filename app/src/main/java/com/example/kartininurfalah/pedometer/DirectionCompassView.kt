package com.example.kartininurfalah.pedometer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CompassView : View {
    private var lebar = 0
    private var tinggi = 0
    private var matriks: Matrix? = null // to manage rotation of the compass view
    private var bitmap: Bitmap? = null
    private var bearing: Float = 0.toFloat() // rotation angle to North

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        initialize()
    }

    private fun initialize() {
        matriks = Matrix()
        // create bitmap for compass icon
        bitmap = BitmapFactory.decodeResource(resources,
                R.drawable.compass_icon)
    }

    fun setBearing(b: Float) {
        bearing = b
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        lebar = View.MeasureSpec.getSize(widthMeasureSpec)
        tinggi = View.MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(lebar, tinggi)
    }

    override fun onDraw(canvas: Canvas) {
        val bitmapWidth = bitmap!!.width
        val bitmapHeight = bitmap!!.height
        val canvasWidth = canvas.width
        val canvasHeight = canvas.height

        if (bitmapWidth > canvasWidth || bitmapHeight > canvasHeight) {
            // resize bitmap to fit in canvas
            bitmap = Bitmap.createScaledBitmap(bitmap!!,
                    (bitmapWidth * 0.85).toInt(), (bitmapHeight * 0.85).toInt(), true)
        }

        // center
        val bitmapX = bitmap!!.width / 2
        val bitmapY = bitmap!!.height / 2
        val parentX = width / 2
        val parentY = height / 2
        val centerX = parentX - bitmapX
        val centerY = parentY - bitmapY

        // calculate rotation angle
        val rotation = (360 - bearing).toInt()

        // reset matrix
        matriks!!.reset()
        matriks!!.setRotate(rotation.toFloat(), bitmapX.toFloat(), bitmapY.toFloat())
        // center bitmap on canvas
        matriks!!.postTranslate(centerX.toFloat(), centerY.toFloat())
        // draw bitmap
        canvas.drawBitmap(bitmap!!, matriks!!, paint)
    }

    companion object {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    }

}