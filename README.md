# WhoAreYou

WhoAreYou is an Android application that performs real-time face detection and classification using two TensorFlow Lite models. It leverages MediaPipe’s BlazeFace for face detection and a custom TFLite model for classification. The app uses CameraX for the live camera feed and Jetpack Compose for a modern UI that displays bounding boxes and labels on detected faces.

## Screenshot

![App Screenshot]([screenshot.png](https://github.com/prathamngundikere/WhoAreYou-Face_Recognition_Android_App/blob/master/screenshot/screenshot.jpg)

## Features

- **Real-Time Face Detection:** Uses MediaPipe's BlazeFace model.
- **Face Classification:** Classifies faces using a custom TFLite model.
- **Live Camera Preview:** Powered by CameraX.
- **Modern UI:** Built with Jetpack Compose.
- **Clean Architecture:** Utilizes ViewModel, Repository, and Kotlin Coroutines.

## Tech Stack

- Kotlin
- Jetpack Compose
- CameraX
- MediaPipe
- TensorFlow Lite
- Kotlin Coroutines & Flow
- Hilt (Dependency Injection)

## Installation and Running

### Prerequisites

- Latest version of Android Studio
- An Android device with a camera
- Minimum Android SDK version as specified in the project

### Steps

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/yourusername/WhoAreYou.git
   cd WhoAreYou
   ```

2. **Open in Android Studio:**

   Launch Android Studio and open the cloned repository.

3. **Assets Setup:**

   Place the following files in the `app/src/main/assets/` folder:
   - `blaze_face_short_range.tflite`
   - `model.tflite`
   - `labels.txt`

4. **Gradle Configuration:**

   Ensure that your TFLite model files are not compressed by adding the following to your `build.gradle.kts` file:

   ```kotlin
   android {
       packagingOptions {
           resources {
               excludes += "/META-INF/{AL2.0,LGPL2.1}"
               doesNotCompress("tflite")
           }
       }
       sourceSets {
           getByName("main") {
               assets.srcDirs("src/main/assets")
           }
       }
   }
   ```

5. **Build and Run:**

   Connect your Android device via USB and run the project from Android Studio.

### Manual Camera Permission Instructions

Since the app does not automatically request camera permissions, follow these steps to grant permission manually:

1. Open **Settings** on your Android device.
2. Navigate to **Apps** or **Applications**.
3. Find and select **WhoAreYou**.
4. Tap on **Permissions**.
5. Enable the **Camera** permission.

## Download

Download the latest APK from [this link](https://github.com/prathamngundikere/WhoAreYou-Face_Recognition_Android_App/releases/latest).

## Credits

Developed by [@prathamngundikere](https://github.com/prathamngundikere).

