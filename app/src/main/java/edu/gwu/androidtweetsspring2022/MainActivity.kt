package edu.gwu.androidtweetsspring2022

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar

class MainActivity : AppCompatActivity() {

    // See Lecture 2 for why `lateinit` is required for our UI variables

    private lateinit var username: EditText

    private lateinit var password: EditText

    private lateinit var login: Button

    private lateinit var progressBar: ProgressBar

    // onCreate is called the first time the Activity is to be shown to the user, so it a good spot
    // to put initialization logic.
    // https://developer.android.com/guide/components/activities/activity-lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tells Android which layout file should be used for this screen.
        setContentView(R.layout.activity_main)

        // Equivalent of a System.out.println (Android has different logging levels to organize logs -- .d is for DEBUG)
        // First parameter = the "tag" allows you to find related logging statements easier (e.g. all logs in the MainActivity)
        // Second parameter = the actual text you want to log
        Log.d("MainActivity", "onCreate called!")

        // The IDs we are using here should match what was set in the "id" field for our views
        // in our XML layout (which was specified by setContentView).
        // Android will "search" the UI for the elements with the matching IDs to bind to our variables.
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        login = findViewById(R.id.login)
        progressBar = findViewById(R.id.progressBar)

        // Kotlin shorthand for login.setEnabled(false).
        // If the getter / setter is unambiguous, Kotlin lets you use the "property-style syntax" so
        // it looks like you're accessing the data field directly to get / set.
        //
        // This sets the login button to be initially disabled when the UI loads.
        login.isEnabled = false

        // Using a lambda to implement a View.OnClickListener interface. We can do this because
        // an OnClickListener is an interface that only requires *one* function.
        //
        // This code block will run when the button is clicked (assuming the button is enabled).
        login.setOnClickListener { view: View ->
            progressBar.visibility = View.VISIBLE

            // An Intent is used to start a new Activity.
            // 1st param == a "Context" which is a reference point into the Android system. All Activities are Contexts by inheritance.
            // 2nd param == the Class-type of the Activity you want to navigate to.
            val intent: Intent = Intent(this, TweetsActivity::class.java)

            // An Intent can also be used like a Map (key-value pairs) to pass data between Activities.
            intent.putExtra("LOCATION", "Washington D.C.")

            // "Executes" our Intent to start a new Activity
            startActivity(intent)
        }

        // Using the same TextWatcher instance for both EditTexts so the same block of code runs on each character.
        username.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)
    }

    // Another example of explicitly implementing an interface (TextWatcher). We cannot use
    // a lambda in this case since there are multiple functions we need to implement.
    //
    // We're defining an "anonymous class" here using the `object` keyword (basically creating
    // a new, dedicated object to implement a TextWatcher for this variable assignment).
    //
    // A TextWatcher's functions will be called everytime the user types a character.
    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        // We can use any of the three functions -- here, we just use `onTextChanged` -- the goal
        // is the enable the login button only if there is text in both the username & password fields.
        override fun onTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // Kotlin shorthand for username.getText().toString()
            // .toString() is needed because getText() returns an Editable (basically a char array).
            val inputtedUsername: String = username.text.toString()
            val inputtedPassword: String = password.text.toString()
            val enableButton: Boolean = inputtedUsername.isNotBlank() && inputtedPassword.isNotBlank()

            // Kotlin shorthand for login.setEnabled(enableButton)
            login.isEnabled = enableButton
        }

        override fun afterTextChanged(p0: Editable?) {}

    }
}