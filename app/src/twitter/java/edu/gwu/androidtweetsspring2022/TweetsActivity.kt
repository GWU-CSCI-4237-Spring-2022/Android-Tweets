package edu.gwu.androidtweetsspring2022

import android.location.Address
import android.os.Bundle
import android.renderscript.Sampler
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import org.jetbrains.anko.doAsync
import java.util.*

class TweetsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    private lateinit var addTweet: FloatingActionButton

    private lateinit var tweetContent: EditText

    private lateinit var firebaseDatabase: FirebaseDatabase

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tweets)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Retrieves the data associated with the "LOCATION" key from the Intent used to launch
        // this Activity (the one we created in the MainActivity)
        // val location: String = getIntent().getStringExtra("LOCATION")!!
        val address: Address = getIntent().getParcelableExtra(MapsActivity.Constants.INTENT_KEY_ADDRESS)!!

        recyclerView = findViewById(R.id.recyclerView)
        addTweet = findViewById(R.id.add_tweet)
        tweetContent = findViewById(R.id.tweet_content)

        getTweetsFromTwitter(address)
    }

    private fun getTweetsFromTwitter(address: Address) {
        // getString(R.string.my_id) allows us to read a value from strings.xml.
        // You can supply additional data parameters if that string has any placeholders that need filling.
        val title: String = getString(R.string.tweets_title, address.getAddressLine(0))

        // Set the screen title
        setTitle(title)

        val twitterManager = TwitterManager()
        val apiKey = getString(R.string.twitter_api_key)
        val apiSecret = getString(R.string.twitter_api_secret)

        // Networking needs to be done on a background thread
        doAsync {
            // Use our TwitterManager to get Tweets from the Twitter API. If there is network
            // connection issues, the catch-block will fire and we'll show the user an error message.
            val tweets: List<Tweet> = try {
                val oAuthToken: String = twitterManager.retrieveOAuthToken(apiKey, apiSecret)
                twitterManager.retrieveTweets(address.latitude,address.longitude, oAuthToken)
            } catch(exception: Exception) {
                Log.e("TweetsActivity", "Retrieving Tweets failed", exception)
                Firebase.crashlytics.recordException(exception)
                firebaseAnalytics.logEvent("twitter_failed", null)
                listOf<Tweet>()
            }

            runOnUiThread {
                if (tweets.isNotEmpty()) {
                    firebaseAnalytics.logEvent("twitter_success", null)
                    val adapter = TweetAdapter(tweets)
                    recyclerView.adapter = adapter
                    recyclerView.layoutManager = LinearLayoutManager(this@TweetsActivity)
                } else {
                    firebaseAnalytics.logEvent("no_tweets", null)
                    Toast.makeText(
                        this@TweetsActivity,
                        R.string.failed_to_retrieve_tweets,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}