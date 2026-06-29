package com.jaspreet.kisanmitra2

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // 1. Declare our UI variables
    private lateinit var leafImageView: ImageView
    private lateinit var scanButton: Button
    private lateinit var resultTextView: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 2. Tell the app to use our exact XML layout
        setContentView(R.layout.activity_main)

        // 3. Connect the Kotlin variables to the XML IDs we created
        leafImageView = findViewById(R.id.leafImageView)
        scanButton = findViewById(R.id.scanButton)
        resultTextView = findViewById(R.id.resultTextView)

        // 4. Test the button connection
        scanButton.setOnClickListener {
            // When you press the button, the text will change to prove the connection works.
            resultTextView.text = "System Ready. Waiting for AI..."
        }
    }
}