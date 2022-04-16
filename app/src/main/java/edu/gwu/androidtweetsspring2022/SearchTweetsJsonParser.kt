package edu.gwu.androidtweetsspring2022

import org.json.JSONArray
import org.json.JSONObject

class SearchTweetsJsonParser {
    fun parseJson(json: JSONObject): List<Tweet> {
        val tweets = mutableListOf<Tweet>()

        // Parse our way through the JSON hierarchy, picking out what we need from each Tweet
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
}