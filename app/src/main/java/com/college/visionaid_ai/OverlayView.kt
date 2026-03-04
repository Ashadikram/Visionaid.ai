package com.college.visionaid_ai

import  android.content.Context
import  android.graphics.*
import android.icu.number.IntegerWidth
import  android.util.AttributeSet
import android.view.View
import org.checkerframework.checker.units.qual.Area
import org.tensorflow.lite.task.vision.detector.Detection
import org.w3c.dom.Attr

class OverlayView(context: Context, attr: AttributeSet?) : View(context, attr){

    private var results: List<Detection> = emptyList()
    private var distances: List<Float> = emptyList()
    private var imageWidth = 1
    private var imageHeight = 1

    private var smoothedRatio = 0f

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 50f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setResults(detections: List<Detection>,
                   imgWidth: Int,
                   imgHeight: Int,
                   distanceList: List<Float>)
    {
        results = detections
        imageWidth = imgWidth
        imageHeight = imgHeight
        distances = distanceList
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

       // canvas.drawRect(100f, 200f, 400f, 600f, boxPaint)

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        for ((index, detection) in results.withIndex()){

            val detection = results[index]
            val box  = detection.boundingBox

            val left = box.left * scaleX
            val top = box.top * scaleY
            val right = box.right * scaleX
            val bottom = box.bottom * scaleY

            canvas.drawRect(left, top, right, bottom, boxPaint)

            // distance calculation
            val distance = distances.getOrNull(index) ?: 0f
            val distanceCm = distance * 100

            val category = detection.categories.firstOrNull()
            if (category != null){
                val label = "${category.label} ${(category.score * 100).toInt()}%     ${distanceCm.toInt()} CM"
                canvas.drawText(label, left, top - 10, textPaint)
            }
        }
    }

    fun getDistance(boxArea: Float): Float {
        val screenArea = imageWidth * imageHeight
        val sizeRatio = boxArea / screenArea.toFloat()

        val safeRatio = sizeRatio.coerceIn(0f, 1f)

        smoothedRatio = (smoothedRatio * 0.8f ) + (safeRatio * 0.2f)

        val maxDistance = 1f
        val predicted = maxDistance * (1f - smoothedRatio)
        return  predicted.coerceAtLeast(0f)
    }
}