# KisanMitra-Android-Native
Offline Edge-AI crop disease diagnostic tool. Engineered with a Native TFLite Bridge to run flawlessly on low-tier 3GB RAM Android devices in rural areas without internet access.


# Kisan Mitra (Android Edge-AI Build) 🌾

Kisan Mitra ("Farmer's Friend") is an offline, low-latency mobile application designed to diagnose crop diseases (like Rice Blast) directly in the field. 

This repository contains the **Native Android Build**, engineered specifically for rural deployment where hardware is limited (3GB RAM) and internet connectivity is non-existent.

## ⚠️ Architectural Context & The "WebView" Pivot
**Initial Prototype:** The first version of this application was built using a standard Web-Wrapper (HTML/JS) running `TensorFlow.js`. 
**The Constraint:** Loading both the JS engine and the heavy neural network simultaneously caused an Out-of-Memory (OOM) fatal crash on our target hardware (sub-$100 Android devices with 3GB RAM).

**The Solution (This Repository): Native Bridge Architecture**
To bypass the memory throttle of Android WebViews, this build separates the UI from the compute engine:
1. **Frontend:** High-performance Android XML Layouts (Material Design) for zero-lag interaction.
2. **Backend:** Kotlin-based inference engine that talks directly to the hardware-accelerated TFLite interpreter.
3. **Efficiency:** By stripping away the browser engine, we dropped RAM usage by 70% and achieved sub-second diagnosis times-fully offline.

## ⚙️ Tech Stack
* **Language:** Kotlin (Native)
* **UI Framework:** Android XML / Material Design 3
* **Machine Learning:** TensorFlow Lite ('.tflite' quantized INT8 model)
* **Performance** Automatic Memory Recycling & Dynamic Tensor Sensing

## 📊 Performance Metrics (Target Hardware: 3GB RAM)
* **Internet Required:** 0% (Fully Offline)
* **Onboarding Experience:** A dedicated welcome screen for farmer explain
* **Confidence Guard:** 70% certainty threshold filter to prevent false diagnosis on non-leaf images.
* **Inference Time:** 1.8sec
* **Model Size:** 2.9MB

## 🛠️ How to Build Locally
1. Clone this repository.
2. Ensure Android Studio and the Android SDK are installed.
3. Place the pre-trained `New_81_model.tflite` and `labels.txt` files into the `android/app/src/main/assets/` directory.
4. Sync Gradle dependencies to load the lightweight `tensorflow-lite` libraries.
5. Build the APK and deploy to a physical device for hardware testing. (Emulators do not accurately represent rural hardware constraints).

---
*Developed for Project Crop-Guard. UI/UX design collaboration in progress.*
*Developed by Jaspreet (Leas System Architect) . Inspired by the philosophy of Steve Jobs: "I play the orchestra" 
