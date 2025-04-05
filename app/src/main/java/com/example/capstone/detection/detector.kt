package com.example.capstone.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.IOException

abstract class Detector(
    protected val context: Context,
    // Ensure modelPath is public (default) for access from MainActivity
    val modelPath: String,
    initialThreshold: Float = 0.5f, // Use a different name for constructor param
    protected var numThreads: Int = 2,
    protected var maxResults: Int = 5
) {
    protected var objectDetector: ObjectDetector? = null
    protected var gpuSupported: Boolean = false

    // Property for threshold with a custom setter
    var threshold: Float = initialThreshold
        set(value) {
            if (field != value) { // Only update if the value actually changed
                field = value // Update the backing field
                Log.d(TAG,"Threshold changed to $value for $modelPath. Re-initializing detector.")
                // Re-setup detector because threshold changed
                close() // Close existing detector first
                try {
                    setupObjectDetector() // Re-initialize with the new threshold
                } catch (e: IOException){
                    Log.e(TAG, "Failed to re-initialize detector after threshold change for $modelPath", e)
                    // Handle re-initialization failure if necessary
                    // Maybe set objectDetector back to null or throw?
                    objectDetector = null
                }
            }
        }

    init {
        // Initial setup uses the initialThreshold value passed to the constructor
        try {
            setupObjectDetector()
        } catch (e: IOException) {
            Log.e(TAG, "Initial detector setup failed for $modelPath during init block.", e)
            // Decide how to handle construction failure - maybe rethrow?
            throw e // Rethrow the exception to signal construction failure
        }
    }

    protected open fun setupObjectDetector() {
        // Ensure the current 'threshold' property value is used during setup
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold) // Use the property here
                .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)
        /*
        val compatList = CompatibilityList()
        if (compatList.isDelegateSupportedOnThisDevice) {
            baseOptionsBuilder.useGpu()
            gpuSupported = true
            Log.d(TAG, "GPU acceleration enabled for $modelPath")
        } else {
            gpuSupported = false
            Log.d(TAG, "GPU acceleration not available for $modelPath, using CPU.")
        }
        */
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        // Clear existing detector before creating a new one within setup
        objectDetector?.close() // Ensure any previous instance is closed
        objectDetector = null // Reset reference

        try {
            Log.d(TAG, "Loading model: $modelPath with threshold: $threshold")
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelPath, optionsBuilder.build())
            Log.d(TAG, "Model loaded successfully: $modelPath")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "TFLite failed to load model '$modelPath': ${e.message}")
            throw IOException("Failed to load TFLite model '$modelPath'", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating object detector for '$modelPath': ${e.message}", e)
            throw IOException("Failed to create detector for '$modelPath'", e)
        }
}


open fun detect(image: Bitmap, imageRotation: Int): List<Detection>? {
// ... (detection logic remains the same) ...
if (objectDetector == null) {
    Log.e(TAG, "Detector for $modelPath not initialized successfully. Cannot detect.")
    return null
}

val startTime = System.nanoTime()
val imageProcessor = ImageProcessor.Builder()
    .add(Rot90Op(-imageRotation / 90))
    .build()
val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

return try {
    val results = objectDetector?.detect(tensorImage)
    val inferenceTime = (System.nanoTime() - startTime) / 1_000_000
    Log.d(TAG, "Detector ($modelPath) Inference Time: $inferenceTime ms")
    results
} catch (e: Exception) {
    Log.e(TAG, "Error during detection with $modelPath: ${e.message}", e)
    null
}
}

open fun close() {
objectDetector?.close()
objectDetector = null
Log.d(TAG, "Object Detector for $modelPath closed.")
}

// REMOVED the explicit setThreshold function - logic is now in the property setter
// fun setThreshold(threshold: Float) { ... }

// REMOVED the explicit getModelPath function - use .modelPath directly
// fun getModelPath(): String = modelPath

fun isGpuSupported(): Boolean = gpuSupported

companion object {
const val TAG = "DetectorBase"
}
}