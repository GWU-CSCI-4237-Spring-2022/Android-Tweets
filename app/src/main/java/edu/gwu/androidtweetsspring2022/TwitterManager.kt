package edu.gwu.androidtweetsspring2022

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject

class TwitterManager {

    private val okHttpClient: OkHttpClient

    init {
        val builder = OkHttpClient.Builder()

        // This will cause all network traffic to be logged to the console for easy debugging
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(loggingInterceptor)

        okHttpClient = builder.build()
    }

    fun retrieveOAuthToken(apiKey: String, apiSecret: String): String {
        val concatenatedSecrets = "$apiKey:$apiSecret"
        val base64Encoded = Base64.encodeToString(concatenatedSecrets.toByteArray(), Base64.NO_WRAP)

        val requestBody = "grant_type=client_credentials".toRequestBody(
            contentType = "application/x-www-form-urlencoded".toMediaType()
        )

        val request = Request.Builder()
            .url("https://api.twitter.com/oauth2/token")
            .header("Authorization", "Basic $base64Encoded")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string()

        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            val json = JSONObject(responseBody)
            return json.getString("access_token")
        }

        return ""
    }

    fun retrieveTweets(latitude: Double, longitude: Double, oAuthToken: String): List<Tweet> {
        val radius = "30mi"
        val searchQuery = "Android"

        // Form the Search Tweets request per the docs at: https://developer.twitter.com/en/docs/tweets/search/api-reference/get-search-tweets.html
        // The "Authorization" header here is similar to an API Key... we'll see with Lecture 7.
        val request: Request = Request.Builder()
            .url("https://api.twitter.com/1.1/search/tweets.json?q=$searchQuery&geocode=$latitude,$longitude,$radius")
            .header("Authorization", "Bearer $oAuthToken")
            .get()
            .build()

        // This executes the request and waits for a response from the server
        val response: Response = okHttpClient.newCall(request).execute()
        val responseBody: String? = response.body?.string()

        // The .isSuccessful checks to see if the status code is 200-299
        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            val tweets = mutableListOf<Tweet>()

            // Parse our way through the JSON hierarchy, picking out what we need from each Tweet
            val json: JSONObject = JSONObject(responseBody)
            val statuses: JSONArray = json.getJSONArray("statuses")

            for (i in 0 until statuses.length()) {
                val curr: JSONObject = statuses.getJSONObject(i)
                val text: String = curr.getString("text")

                val user: JSONObject = curr.getJSONObject("user")
                val name: String = user.getString("name")
                val handle: String = user.getString("screen_name")
                val profilePictureUrl: String = user.getString("profile_image_url_https")

                val tweet = Tweet(
                    username = name,
                    handle = handle,
                    content = text,
                    iconUrl = profilePictureUrl
                )

                tweets.add(tweet)
            }

            return tweets
        }

        return listOf()
    }
}