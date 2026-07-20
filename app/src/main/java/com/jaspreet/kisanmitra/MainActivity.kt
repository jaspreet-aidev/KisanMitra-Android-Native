package com.jaspreet.kisanmitra

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
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var leafImageView: ImageView
    private lateinit var scanButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var btnGallery: Button
    private lateinit var btnCamera: Button
    private var selectedBitmap: Bitmap? = null

    private val imageSize = 224
    private var interpreter: Interpreter? = null

    // Rice Disease Labels (Order must match your TFLite model training)
    private val labels = arrayOf(
        "Bacterial Leaf Blight",
        "Brown Spot",
        "Healthy Rice Leaf",
        "Rice Hispa",
        "Leaf Blast",
        "Leaf Scald",
        "Leaf Smut",
        "Neck Blast",
        "Sheath Blight",
        "Tungro Virus"
    )

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        leafImageView = findViewById(R.id.imageView)
        scanButton = findViewById(R.id.btnScan)
        resultTextView = findViewById(R.id.tvResult)
        btnGallery = findViewById(R.id.btnGallery)
        btnCamera = findViewById(R.id.btnCamera)

        // Load TFLite Model
        try {
            interpreter = Interpreter(loadModelFile())
        } catch (e: Exception) {
            e.printStackTrace()
            resultTextView.text = "Error loading model: ${e.message}"
        }

        val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val bitmap = uriToBitmap(it)
                if (bitmap != null) {
                    processAndDisplayImage(bitmap)
                }
            }
        }

        val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                processAndDisplayImage(it)
            }
        }

        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnCamera.setOnClickListener {
            cameraLauncher.launch(null)
        }

        scanButton.setOnClickListener {
            selectedBitmap?.let {
                runInference(it)
            } ?: run {
                resultTextView.text = "Please select or take an image first."
            }
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("New_81_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun processAndDisplayImage(originalBitmap: Bitmap) {
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, imageSize, imageSize, true)
        selectedBitmap = resizedBitmap 
        leafImageView.setImageBitmap(resizedBitmap)
        resultTextView.text = "Image ready for scan."
    }

    private fun runInference(bitmap: Bitmap) {
        // 1. Prepare ByteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(imageSize * imageSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }

        // 2. Define output buffer
        val output = Array(1) { FloatArray(labels.size) } 

        // 3. Run Model
        interpreter?.run(byteBuffer, output)

        // Log scores for debugging
        for (i in labels.indices) {
            android.util.Log.d("AI_MODEL", "${labels[i]}: ${output[0][i]}")
        }

        // 4. Display result
        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        val confidence = if (maxIndex != -1) output[0][maxIndex] else 0f
        
        if (maxIndex != -1 && maxIndex < labels.size) {
            val diagnosis = labels[maxIndex]
            resultTextView.text = "Result: $diagnosis\nConfidence: ${String.format("%.2f", confidence * 100)}%"
        } else {
            resultTextView.text = "Detection failed."
        }
    }

    @Suppress("DEPRECATION")
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) { null }
    }
}
