package com.example.capstone // Replace with your package name

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.LinkedList
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    // Store the dimensions of the image used for detection
    private var detectionImageWidth: Int = 1
    private var detectionImageHeight: Int = 1

    init {
        initPaints()
    }

    fun clear() {
        results = LinkedList<Detection>()
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate() // Trigger redraw to clear the screen
        initPaints() // Re-initialize paints
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color) // Define this color
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (results.isEmpty()) {
            return // Nothing to draw
        }

        // Calculate the scale factor if view dimensions are available
        // This maps coordinates from the detection image size to the view size
        scaleFactor = max(width * 1f / detectionImageWidth, height * 1f / detectionImageHeight)

        for (result in results) {
            val boundingBox = result.boundingBox

            // Adjust coordinates based on the scale factor
            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // Draw bounding box
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text for Results with confidence.
            val drawableText =
                result.categories.joinToString(separator = "\n") {
                    "${it.label} (${String.format("%.2f", it.score)})"
                }

            // --- Draw text background ---
            // Calculate text width and height for background justification
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            // Ensure text stays within view bounds
            var textLeft = left + 8 // Add slight offset from box
            var textTop = top + bounds.height() + 8 // Position below the top line

            // Adjust if text goes off screen horizontally
            if (textLeft + textWidth > width) {
                textLeft = width.toFloat() - textWidth - 8 // Move it left
            }
            // Adjust if text goes off screen vertically (less common for top placement)
            if (textTop - textHeight < 0) {
                textTop = top + textHeight + 16 // Move below box if it would go off top
            } else {
                textTop = top - 8 // Place above box is usually better
            }


            canvas.drawRect(
                textLeft, // left
                textTop - textHeight, // top
                textLeft + textWidth + 8, // right (+padding)
                textTop + 8, // bottom (+padding)
                textBackgroundPaint
            )

            // Draw text for detected object label.
            canvas.drawText(drawableText, textLeft, textTop, textPaint)
        }
    }

    fun setResults(
        detectionResults: List<Detection>,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults
        detectionImageHeight = imageHeight
        detectionImageWidth = imageWidth

        // Request redraw
        invalidate()
    }
}