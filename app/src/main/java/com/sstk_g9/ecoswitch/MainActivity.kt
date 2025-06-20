package com.sstk_g9.ecoswitch

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var count = 0
    private lateinit var countTextView: TextView
    private lateinit var incrementButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        countTextView = findViewById(R.id.countTextView)
        incrementButton = findViewById(R.id.incrementButton)

        updateCountDisplay()

        incrementButton.setOnClickListener {
            count++
            updateCountDisplay()
        }
    }

    private fun updateCountDisplay() {
        countTextView.text = count.toString()
    }
}