package edu.gwu.androidtweetsspring2022

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TweetsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tweets)

        // Retrieves the data associated with the "LOCATION" key from the Intent used to launch
        // this Activity (the one we created in the MainActivity)
        val location: String = getIntent().getStringExtra("LOCATION")!!

        // getString(R.string.my_id) allows us to read a value from strings.xml.
        // You can supply additional data parameters if that string has any placeholders that need filling.
        val title: String = getString(R.string.tweets_title, location)

        // Set the screen title
        setTitle(title)
    }
}