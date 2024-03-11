package com.aspire.ubinex

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aspire.ubinex.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private var tag : List<String> = listOf("Your AI companion. Ready to assist.",
        "Intelligence at your fingertips.",
        "Make the most of your day. Start with AI.",
        "Where inspiration meets innovation.",
        "Get smarter answers. Faster.",
        "The future of problem-solving is here.")

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable : Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        runnable = object : Runnable{
            override fun run() {

                binding.mainTagTv.text = tag.random()

                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable)
        //binding.mainTagTv.text = tag.random()


    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onResume() {
        super.onResume()
        handler.post(runnable)
    }

}