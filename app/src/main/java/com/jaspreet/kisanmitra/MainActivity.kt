package com.jaspreet.kisanmitra2

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    // 1. Declare UI variables
    private lateinit var leafImageView: ImageView
    private lateinit var scanButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button

    // The Memory Squeeze Target (MobileNetV2 requires 224x224)
    private val imageSize = 224// 1. Define the launcher at the class level
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            processAndDisplayImage(it)
        }
    }

    // 2. Define the function to call the launcher
    private fun openCamera() {
        cameraLauncher.launch(null)
    }



    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. Connect the Kotlin variables to XML IDs
        leafImageView = findViewById(R.id.leafImageView)
        scanButton = findViewById(R.id.scanButton)
        resultTextView = findViewById(R.id.resultTextView)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)

        // 3. Camera Trigger (Fast Bridge)
        // Inside your onCreate method, where you set up your button:
        val cameraButton = findViewById<Button>(R.id.btnCamera)

        btnCamera.setOnClickListener {
            // 1. Check if we have permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                // 2. Request permission if not granted
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
            }
        }


        // 1. Find the button using the correct ID from activity_main.xml
// Assuming your button has android:id="@+id/btnCamera"
        val btnCamera = findViewById<Button>(R.id.btnCamera)

        btnCamera.setOnClickListener {
            try {
                // Run your camera logic here
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
                }
            } catch (e: Exception) {
                // If the system tries to kill the app, this catch block keeps it alive
                e.printStackTrace()
                Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }



        // 4. Gallery Trigger
        val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val bitmap = uriToBitmap(it)
                if (bitmap != null) {
                    processAndDisplayImage(bitmap)
                }
            }
        }

        // 5. Button Click Listeners
        btnCamera.setOnClickListener {
            cameraLauncher.launch(null)
        }

        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        scanButton.setOnClickListener {
            resultTextView.text = "System Ready. AI Scan will happen here in Phase 4."
        }
    }
    // 3. Add this callback to handle the user's response to the popup
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Permission denied. Camera cannot be opened.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // --- CORE ARCHITECTURE: MEMORY SQUEEZE ---

    @SuppressLint("SetTextI18n")
    private fun processAndDisplayImage(originalBitmap: Bitmap) {
        // INSTANTLY crush the massive image down to 224x224 to save RAM
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, imageSize, imageSize, true)

        // Display the safe, resized image on the screen
        leafImageView.setImageBitmap(resizedBitmap)

        // Clear the massive original image from memory immediately
        if (originalBitmap != resizedBitmap) {
            originalBitmap.recycle()
        }

        resultTextView.text = "Image loaded safely (224x224 pixels)."
    }

    // Helper function safely converts Gallery URI to Bitmap
    // (The Suppress tag silences the warning for our legacy device compatibility)
    @Suppress("DEPRECATION")
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true // Allows resizing
                }
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}