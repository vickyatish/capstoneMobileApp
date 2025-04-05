# Android Object Detection App

This Android application utilizes TensorFlow Lite to perform object detection. It is designed with an abstract `Detector` class to allow for easy switching between different detection models.


![Capstone Mobile App Screenshot](https://imgur.com/a/X7iDGM1)

## Project Structure

The core structure of this project is based on the following class design:

* **`Detector` (Abstract Class)**: This class defines the common interface for all detector subclasses. It includes the following abstract methods:
    * `restart()`
    * `init()`
    * `create()`
    * `detect()` [cite: 1, 2, 3]
* **`SignDetector` (Subclass of `Detector`)**:  This class implements the `Detector` interface to detect signs. [cite: 2]
* **`WalkwayDamageDetector` (Subclass of `Detector`)**: This class implements the `Detector` interface to detect walkway damage. [cite: 3]

This design allows for polymorphic behavior, where the specific detection logic is determined at runtime. [cite: 5, 6, 9]

## Implementation Details

The application's main logic resides within the `MainActivity` (or a similar component), where detector instances are created and used.

```kotlin
lateinit var detector: Detector
lateinit var signDetector: SignDetector
lateinit var walkwayDamageDetector: WalkwayDamageDetector

fun setupDetectors() {
    // Pass in real constructor args as needed
    signDetector  SignDetector(/* ... */)
    walkwayDamageDetector  WalkwayDamageDetector(/* ... */)

    val useSignDetection = true // or some switch/flag

    detector = if (useSignDetection) {
        signDetector
    } else {
        walkwayDamageDetector
    }

    // Later in your code:
    detector.init()
    detector.detect()
}
```


## Key Points:

Kotlin's polymorphism allows us to assign subclasses (SignDetector, WalkwayDamageDetector) to a superclass variable (detector) without casting.
Method calls on the detector instance will be dynamically dispatched to the correct subclass implementation.
Constructors in the subclass can accept arguments, such as context and model path.
TensorFlow Lite Models
This application uses two TensorFlow Lite models for object detection:

'sidewalkmodel.tflite'
'regressionmodel.tflite'
Important: These models are renamed detection models with metadata downloaded from TensorFlow Hub. Ensure that these files are placed in the appropriate directory within the Android project before running the application.

## Note on Model Recognition:

It is likely that issues encountered with the app not recognizing the intended model earlier were due to missing metadata or the app's inability to properly read the metadata of the TFLite models.

Additional Considerations
Model Instancing: The document raises the question of single vs. dual model instancing.
Dual Model: Faster switching but higher GPU usage.
Single Model: Efficient GPU usage but slower switching.    
Further development should consider the optimal approach for the specific use case.

## Author
Yatish Sekaran