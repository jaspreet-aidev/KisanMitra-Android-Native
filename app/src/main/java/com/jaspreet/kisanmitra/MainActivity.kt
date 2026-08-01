package com.jaspreet.kisanmitra

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log.d
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale



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
        try {
            val interpreter = interpreter ?: throw Exception("AI brain not ready. ")

            // 1. Get Model requirements
            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)

            val modelImageSize = inputTensor.shape()[1] //Usually 224
            val dataType = inputTensor.dataType()        //Detect Float#@ vs INT8
            val numClasses = outputTensor.shape()[1]  //Detected from your .tflite
            val outputDataType = outputTensor.dataType()
            d("AI_MODEL", "Model Type: $dataType | Size: $modelImageSize")

            // 2. Pre-process Image (Resize & Downsample)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, modelImageSize, modelImageSize, true)

            // 3. Prepare Buffer (Calculate size based on Datatype)
            // Float32 = 4 bytes per channel | INT8 = 1 byte per channel
            val bytesPerChannel = if (dataType == DataType.FLOAT32) 4 else 1
            val byteBuffer = ByteBuffer.allocateDirect(bytesPerChannel * modelImageSize * modelImageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(modelImageSize * modelImageSize)
            resizedBitmap.getPixels(intValues, 0 ,modelImageSize, 0, 0, modelImageSize, modelImageSize)

            //4. Pixel-to-Tensor Conversion
            byteBuffer.rewind()
            for (pixel in intValues) {
                val r = (pixel shr 16 and 0xFF)
                val g = (pixel shr 8 and 0xFF)
                val b = (pixel and 0xFF)

                if (dataType == DataType.FLOAT32) {
                    //Normalize to 0.0 - 1.0 for  Float models
                    byteBuffer.putFloat(r / 255.0f)
                    byteBuffer.putFloat(g / 255.0F)
                    byteBuffer.putFloat(b / 255.0f)
                }
                else {
                    // Use raw bytes for Quantized models
                    byteBuffer.put(r.toByte())
                    byteBuffer.put(g.toByte())
                    byteBuffer.put(b.toByte())
                }
            }

            //5. Run Modelwith Dynamic Output Buffer
            var maxIndex = -1
            var confidence = 0.0f

            if ( outputDataType == DataType.INT8) {
                //Quantized Model Path
                val output = Array(1) { ByteArray(numClasses) }
                interpreter.run(byteBuffer, output)

                //Find max index in bytes (higher value = hihger probability)
                var maxVal = -129
                for (i in 0 until numClasses) {
                    if (output[0][i] > maxVal) {
                        maxVal = output[0][i].toInt()
                        maxIndex = i
                    }
                }
                //for quantized models , confidence is harder to show without scale/zero point,
                //but we can show the raw probability relative to the range
                confidence = (maxVal + 128 ) / 255.0f
            }
            else {
                //Float Model Path
                val output = Array(1) { FloatArray(numClasses) }
                interpreter.run(byteBuffer, output)

                maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
                confidence = if (maxIndex != -1) output[0][maxIndex] else 0f
            }


            //6. Memory Cleanup ( Crtical for #Gb RAM devices )
            if (resizedBitmap != bitmap) resizedBitmap.recycle()
            // Aggresive GC hint
            System.gc()

            // 7. UI Update
            if (maxIndex != -1 && maxIndex < labels.size) {
                val diagnosis = labels[maxIndex]
                resultTextView.text = String.format(Locale.US, "Results: %s\nConfidence: %.2f%%", diagnosis, confidence * 100)
            }
            else {
                resultTextView.text = "Diagnosis Failed: Mode; index out of bounds."
            }

        }
        catch (e: Exception) {
            android.util.Log.e("AI_MODEL", "Inference error", e)
            resultTextView.text = "Error: ${e.message}"
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
