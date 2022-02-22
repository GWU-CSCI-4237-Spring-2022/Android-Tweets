package edu.gwu.androidtweetsspring2022

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.anko.doAsync

class TweetsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

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

        recyclerView = findViewById(R.id.recyclerView)

        val twitterManager = TwitterManager()
        val apiKey = getString(R.string.twitter_api_key)

        // Networking needs to be done on a background thread
        doAsync {
            // Use our TwitterManager to get Tweets from the Twitter API. If there is network
            // connection issues, the catch-block will fire and we'll show the user an error message.
            val tweets: List<Tweet> = try {
                twitterManager.retrieveTweets(37.7697583,-122.42079689999998, apiKey)
            } catch(exception: Exception) {
                Log.e("TweetsActivity", "Retrieving Tweets failed", exception)
                listOf<Tweet>()
            }

            runOnUiThread {
                if (tweets.isNotEmpty()) {
                    val adapter = TweetAdapter(tweets)
                    recyclerView.adapter = adapter
                    recyclerView.layoutManager = LinearLayoutManager(this@TweetsActivity)
                } else {
                    Toast.makeText(
                        this@TweetsActivity,
                        "Failed to retrieve Tweets!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}