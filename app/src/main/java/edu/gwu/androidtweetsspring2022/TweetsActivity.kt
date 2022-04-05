package edu.gwu.androidtweetsspring2022

import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.View
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

    private val currentTweets: MutableList<Tweet> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tweets)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Retrieves the data associated with the "LOCATION" key from the Intent used to launch
        // this Activity (the one we created in the MainActivity)
        // val location: String = getIntent().getStringExtra("LOCATION")!!
        val address: Address = getIntent().getParcelableExtra("address")!!

        recyclerView = findViewById(R.id.recyclerView)
        addTweet = findViewById(R.id.add_tweet)
        tweetContent = findViewById(R.id.tweet_content)

        if (savedInstanceState != null) {
            addTweet.hide()
            tweetContent.visibility = View.GONE

            currentTweets.addAll(savedInstanceState.getSerializable("tweets") as List<Tweet>)
            val adapter = TweetAdapter(currentTweets)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this@TweetsActivity)
        } else {
            // First time activity is being displayed
            getTweetsFromTwitter(address)
//        getTweetsFromFirebase(address)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val serializableList = ArrayList(currentTweets)
        outState.putSerializable("tweets", serializableList)
    }

    private fun getTweetsFromTwitter(address: Address) {
        addTweet.hide()
        tweetContent.visibility = View.GONE

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

            currentTweets.clear()
            currentTweets.addAll(tweets)

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

    private fun getTweetsFromFirebase(address: Address) {
        val state = address.adminArea ?: "Unknown"
        title = getString(R.string.tweets_firebase_title, state)

        val reference = firebaseDatabase.getReference("tweets/$state")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                firebaseAnalytics.logEvent("firebasedb_cancelled", null)
                Toast.makeText(
                    this@TweetsActivity,
                    R.string.failed_to_retrieve_tweets,
                    Toast.LENGTH_LONG
                ).show()

                Log.e("TweetsActivity", "DB connection issue", error.toException())
                Firebase.crashlytics.recordException(error.toException())
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                firebaseAnalytics.logEvent("firebasedb_data_change", null)

                val tweets = mutableListOf<Tweet>()
                snapshot.children.forEach { childSnapshot: DataSnapshot ->
                    try {
                        val tweet = childSnapshot.getValue(Tweet::class.java)
                        if (tweet != null) {
                            tweets.add(tweet)
                        }
                    } catch (exception: Exception) {
                        Log.e("TweetsActivity", "Failed to read Tweet", exception)
                        Firebase.crashlytics.recordException(exception)
                    }
                }

                val adapter = TweetAdapter(tweets)
                recyclerView.adapter = adapter
                recyclerView.layoutManager = LinearLayoutManager(this@TweetsActivity)
            }
        })



        addTweet.setOnClickListener {
            firebaseAnalytics.logEvent("add_tweets_clicked", null)
            val tweetContent = tweetContent.text.toString()
            val email: String = FirebaseAuth.getInstance().currentUser!!.email!!
            val tweet = Tweet(
                username = email,
                handle = email,
                content = tweetContent,
                iconUrl = ""
            )

            reference.push().setValue(tweet)
        }
    }
}