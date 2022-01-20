package edu.gwu.androidtweetsspring2022

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        System.out.println("onCreate (sys out)")

        Log.d("MainActivity", "onCreate called")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
    }

    override fun onPause() {
        Log.d("MainActivity", "onPause called")
        super.onPause()
    }

    override fun onDestroy() {
        Log.d("MainActivity", "onDestroy called")
        super.onDestroy()
    }
}