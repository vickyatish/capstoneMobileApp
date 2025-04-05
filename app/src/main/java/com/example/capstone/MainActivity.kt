package com.example.capstone

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.capstone.databinding.ActivityMainBinding
import com.example.capstone.detection.Detector // Import the abstract class
import com.example.capstone.detection.SignDetector // Import concrete class
import com.example.capstone.detection.WalkwayDamageDetector // Import concrete class
import org.tensorflow.lite.task.vision.detector.Detection
// Removed IOException import as we catch general Exception now for broader init issues
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    // References to the specific detector instances
    private var signDetector: SignDetector? = null
    private var walkwayDamageDetector: WalkwayDamageDetector? = null

    // The currently active detector (polymorphic reference)
    private var currentDetector: Detector? = null

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val DETECTOR_SIGN = 0
        private const val DETECTOR_WALKWAY = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Initialization ---
        cameraExecutor = Executors.newSingleThreadExecutor()

        // --- Initialize Detectors Concurrently ---
        // We attempt to load both detectors on a background thread.
        // The UI (toggle button) will be enabled only if BOTH load successfully.
        cameraExecutor.execute {
            var signSuccess = false
            var walkwaySuccess = false


            // --- Attempt to Initialize Sign Detector ---
            try {
                Log.d(TAG, "Initializing SignDetector...")
                signDetector = SignDetector(applicationContext)
                Log.d(TAG, "SignDetector Initialized successfully.")
                signSuccess = true
            } catch (e: Exception) { // Catch broader exceptions during TFLite initialization
                Log.e(TAG, "Failed to initialize SignDetector: ${e.message}", e)
                // signDetector remains null
            }

            // --- Attempt to Initialize Walkway Damage Detector ---
            try {
                Log.d(TAG, "Initializing WalkwayDamageDetector...")
                walkwayDamageDetector = WalkwayDamageDetector(applicationContext)
                Log.d(TAG, "WalkwayDamageDetector Initialized successfully.")
                walkwaySuccess = true
            } catch (e: Exception) { // Catch broader exceptions during TFLite initialization
                Log.e(TAG, "Failed to initialize WalkwayDamageDetector: ${e.message}", e)
                // walkwayDamageDetector remains null
            }

            // --- Update UI based on Initialization Results (on Main Thread) ---
            runOnUiThread {
                if (signSuccess && walkwaySuccess) {
                    // BOTH succeeded: Enable toggle and capture
                    Log.i(TAG, "Both detectors initialized successfully.")
                    currentDetector = signDetector // Set default detector (e.g., sign)
                    binding.modelToggle.isEnabled = true // <<< ENABLE TOGGLE
                    binding.captureButton.isEnabled = true
                    binding.modelToggle.isChecked = false // Ensure toggle visually matches default (Sign=off)
                    Toast.makeText(this, "Models loaded. Default: Sign", Toast.LENGTH_SHORT).show()

                } else if (signSuccess) {
                    // Only Sign succeeded: Disable toggle, enable capture with Sign model
                    Log.w(TAG, "Only SignDetector initialized successfully.")
                    currentDetector = signDetector
                    binding.modelToggle.isEnabled = false // Cannot toggle
                    binding.captureButton.isEnabled = true
                    Toast.makeText(this, "Sign Model loaded (Walkway failed). Toggle disabled.", Toast.LENGTH_LONG).show()

                } else if (walkwaySuccess) {
                    // Only Walkway succeeded: Disable toggle, enable capture with Walkway model
                    Log.w(TAG, "Only WalkwayDamageDetector initialized successfully.")
                    currentDetector = walkwayDamageDetector
                    binding.modelToggle.isEnabled = false // Cannot toggle
                    binding.captureButton.isEnabled = true
                    Toast.makeText(this, "Walkway Model loaded (Sign failed). Toggle disabled.", Toast.LENGTH_LONG).show()

                } else {
                    // BOTH failed: Keep buttons disabled
                    Log.e(TAG, "Failed to initialize ANY detector.")
                    currentDetector = null
                    binding.modelToggle.isEnabled = false
                    binding.captureButton.isEnabled = false
                    Toast.makeText(this, "Error loading detection models. App cannot function.", Toast.LENGTH_LONG).show()
                }
            }
        } // End cameraExecutor.execute

        // --- UI Listeners ---
        binding.captureButton.setOnClickListener {
            if (currentDetector != null) {
                takePhoto()
            } else {
                // This case should ideally be prevented by the button being disabled
                // if currentDetector is null after initialization.
                Toast.makeText(this, "Detector not ready.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Capture button clicked but currentDetector is null!")
            }
        }

        // Initial state: Disable buttons until detectors are loaded and UI is updated
        binding.modelToggle.isEnabled = false
        binding.captureButton.isEnabled = false

        binding.modelToggle.setOnCheckedChangeListener { _, isChecked ->
            // This listener will only be effective if the toggle is enabled (i.e., both models loaded)
            val selectedDetectorType = if (isChecked) DETECTOR_WALKWAY else DETECTOR_SIGN
            switchDetector(selectedDetectorType)
        }

        // --- Permissions and Camera Setup ---
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun switchDetector(detectorType: Int) {
        // This function assumes the specific detector instance (signDetector or
        // walkwayDamageDetector) is non-null if we are attempting to switch to it.
        // This is safe because the toggle is only enabled if both are non-null.
        val previousDetector = currentDetector // For logging, optional

        currentDetector = when (detectorType) {
            DETECTOR_SIGN -> signDetector
            DETECTOR_WALKWAY -> walkwayDamageDetector
            else -> {
                Log.e(TAG, "Invalid detector type requested: $detectorType")
                null // Should not happen with boolean toggle
            }
        }

        if (currentDetector != null && currentDetector != previousDetector) {
            binding.overlayView.clear() // Clear previous detections on switch
            val modelName = currentDetector?.let { it::class.java.simpleName } ?: "Unknown Model"
            Toast.makeText(this, "Switched to $modelName", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Switched detector. New: ${currentDetector?.modelPath}, Previous: ${previousDetector?.modelPath}")
        } else if (currentDetector == null) {
            // This case should ideally not be reached if the toggle is managed correctly
            Log.e(TAG, "Switch failed: Could not find detector for type $detectorType")
            Toast.makeText(this, "Selected model is unavailable.", Toast.LENGTH_SHORT).show()
            binding.captureButton.isEnabled = false // Safety measure
        }
        // If currentDetector == previousDetector, no action needed.
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish() // Close app if permissions are denied
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                // Set target resolution or aspect ratio if needed for model accuracy
                imageCapture = ImageCapture.Builder()
                    // .setTargetResolution(Size(width, height)) // Example
                    // .setTargetAspectRatio(AspectRatio.RATIO_4_3) // Example
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll() // Unbind use cases before rebinding
                cameraProvider?.bindToLifecycle(
                    this, // LifecycleOwner
                    cameraSelector, // CameraSelector
                    preview, // Preview use case
                    imageCapture // ImageCapture use case
                )
                Log.d(TAG, "Camera started and use cases bound successfully")

            } catch (exc: Exception) { // Catch CameraX exceptions
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Failed to start camera: ${exc.localizedMessage}", Toast.LENGTH_LONG).show()
                // Consider disabling capture button or showing persistent error
                binding.captureButton.isEnabled = false
            }
        }, ContextCompat.getMainExecutor(this)) // Run listener on main thread
    }


    private fun takePhoto() {
        binding.overlayView.clear() // Clear previous drawings
        val imageCapture = imageCapture ?: return // Return if imageCapture is not initialized
        val activeDetector = currentDetector ?: run {
            Log.e(TAG, "takePhoto called but currentDetector is null!")
            Toast.makeText(this, "Detector not active.", Toast.LENGTH_SHORT).show()
            return // Return if no detector is active
        }

        Toast.makeText(this, "Capturing...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Attempting photo capture with detector: ${activeDetector.modelPath}")

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this), // Use main executor for callback simplicity for now
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    Log.d(TAG, "Image captured successfully. Format: ${image.format}, Rotation: ${image.imageInfo.rotationDegrees}")
                    // Process image and run detection on the background thread
                    cameraExecutor.execute {
                        val bitmap = imageProxyToBitmap(image)
                        val rotationDegrees = image.imageInfo.rotationDegrees
                        image.close() // IMPORTANT: Close ImageProxy promptly

                        if (bitmap != null) {
                            Log.d(TAG, "Bitmap created successfully. Running detection...")
                            // Use the currently active detector via the abstract reference
                            val results = activeDetector.detect(bitmap, rotationDegrees) // Pass rotation
                            runOnUiThread {
                                displayDetectionResults(results, bitmap)
                            }
                        } else {
                            Log.e(TAG, "Failed to convert ImageProxy to Bitmap")
                            runOnUiThread {
                                Toast.makeText(baseContext, "Failed to process image", Toast.LENGTH_SHORT).show()
                                // Optionally clear overlay or show placeholder
                                binding.overlayView.clear()
                            }
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    runOnUiThread {
                        Toast.makeText(baseContext, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Consider making this more robust or using a library if needed
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            when (image.format) {
                ImageFormat.JPEG -> {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                ImageFormat.YUV_420_888 -> {
                    // Using the built-in converter. May need optimization for performance.
                    image.toBitmap()
                }
                else -> {
                    Log.e(TAG, "Unsupported image format: ${image.format}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            null
        }
    }


    private fun displayDetectionResults(results: List<Detection>?, bitmap: Bitmap) {
        // Check if results are null (indicating an error during detection)
        if (results == null) {
            Log.e(TAG, "Detection results were null (detector error).")
            Toast.makeText(this, "Detection failed.", Toast.LENGTH_SHORT).show()
            binding.overlayView.clear() // Clear overlay on error
            return
        }

        // Check if the list is empty (no objects detected)
        if (results.isEmpty()) {
            Log.d(TAG, "No objects detected by ${currentDetector?.modelPath ?: "current detector"}.")
            Toast.makeText(this, "No objects detected.", Toast.LENGTH_SHORT).show()
            binding.overlayView.clear() // Clear overlay if no detections
            return
        }

        // Log detection details and update the overlay
        Log.d(TAG, "Displaying ${results.size} detections using ${currentDetector?.modelPath ?: "current detector"}")
        binding.overlayView.setResults(
            results,
            bitmap.height, // Original bitmap height
            bitmap.width   // Original bitmap width
        )
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called. Releasing resources.")
        // Shut down executor FIRST to stop processing tasks and prevent RejectedExecutionException
        cameraExecutor.shutdown()
        Log.d(TAG, "CameraExecutor shut down.")

        // Close detectors - Important to release TFLite resources
        try {
            signDetector?.close()
            Log.d(TAG, "SignDetector closed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing SignDetector", e)
        }
        try {
            walkwayDamageDetector?.close()
            Log.d(TAG, "WalkwayDamageDetector closed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WalkwayDamageDetector", e)
        }

        // CameraProvider resources are managed by lifecycle, but explicit unbind is safe practice
        // Though cameraProvider might be null if startCamera failed.
        try {
            cameraProvider?.unbindAll()
            Log.d(TAG, "CameraProvider unbound.")
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding CameraProvider", e)
        }
        Log.d(TAG, "onDestroy finished.")
    }
}