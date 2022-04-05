package edu.gwu.androidtweetsspring2022

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Address
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.*
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import java.util.*

class MainActivity : AppCompatActivity() {

    // See Lecture 2 for why `lateinit` is required for our UI variables

    private lateinit var username: EditText

    private lateinit var password: EditText

    private lateinit var login: Button

    private lateinit var signUp: Button

    private lateinit var progressBar: ProgressBar

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    // onCreate is called the first time the Activity is to be shown to the user, so it a good spot
    // to put initialization logic.
    // https://developer.android.com/guide/components/activities/activity-lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tells Android which layout file should be used for this screen.
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val sharedPrefs: SharedPreferences =
            getSharedPreferences("android-tweets", Context.MODE_PRIVATE)

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
        signUp = findViewById(R.id.signUp)
        progressBar = findViewById(R.id.progressBar)

        // Kotlin shorthand for login.setEnabled(false).
        // If the getter / setter is unambiguous, Kotlin lets you use the "property-style syntax" so
        // it looks like you're accessing the data field directly to get / set.
        //
        // This sets the login button to be initially disabled when the UI loads.
        login.isEnabled = false
        signUp.isEnabled = false

        // Using a lambda to implement a View.OnClickListener interface. We can do this because
        // an OnClickListener is an interface that only requires *one* function.
        //
        // This code block will run when the button is clicked (assuming the button is enabled).
        login.setOnClickListener { view: View ->

            firebaseAnalytics.logEvent("login_button_clicked", null)

            val inputtedUsername: String = username.text.toString()
            val inputtedPassword: String = password.text.toString()

            progressBar.visibility = View.VISIBLE

            firebaseAuth
                .signInWithEmailAndPassword(inputtedUsername, inputtedPassword)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.INVISIBLE

                    if (task.isSuccessful) {
                        firebaseAnalytics.logEvent("login_success", null)

                        val user = firebaseAuth.currentUser
                        Toast.makeText(this, "Logged in as ${user!!.email}", Toast.LENGTH_LONG).show()


                        // Save the username to SharedPreferences
                        sharedPrefs
                            .edit()
                            .putString("USERNAME", inputtedUsername)
                            .apply()

                        // An Intent is used to start a new Activity.
                        // 1st param == a "Context" which is a reference point into the Android system. All Activities are Contexts by inheritance.
                        // 2nd param == the Class-type of the Activity you want to navigate to.
                        val intent: Intent = Intent(this, MapsActivity::class.java)

                        // An Intent can also be used like a Map (key-value pairs) to pass data between Activities.
                        // intent.putExtra("LOCATION", "Washington D.C.")

                        // "Executes" our Intent to start a new Activity
                        startActivity(intent)
                    } else {
                        val exception = task.exception

                        if (exception != null) {
                            Firebase.crashlytics.recordException(exception)
                        }

                        // A when-statement is like a switch / case statement in Java
                        when (exception) {
                            is FirebaseAuthInvalidUserException -> {
                                val bundle = Bundle()
                                bundle.putString("reason", "no_registered_account")
                                firebaseAnalytics.logEvent("login_failed", bundle)

                                Toast.makeText(
                                    this,
                                    R.string.login_failure_doesnt_exist,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                val bundle = Bundle()
                                bundle.putString("reason", "invalid_credentials")
                                firebaseAnalytics.logEvent("login_failed", bundle)

                                Toast.makeText(
                                    this,
                                    R.string.login_failure_wrong_credentials,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                val bundle = Bundle()
                                bundle.putString("reason", "generic")
                                firebaseAnalytics.logEvent("login_failed", bundle)

                                Toast.makeText(
                                    this,
                                    getString(R.string.login_failure_generic, exception),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
        }

        signUp.setOnClickListener {
            val inputtedUsername: String = username.text.toString()
            val inputtedPassword: String = password.text.toString()

            progressBar.visibility = View.VISIBLE

            firebaseAnalytics.logEvent("signup_clicked", null)

            firebaseAuth
                .createUserWithEmailAndPassword(inputtedUsername, inputtedPassword)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.INVISIBLE

                    if (task.isSuccessful) {
                        showNotification()
                        
                        firebaseAnalytics.logEvent("signup_success", null)
                        val user = firebaseAuth.currentUser
                        Toast.makeText(this, "Successfully registered as ${user!!.email}", Toast.LENGTH_LONG).show()
                    } else {
                        val exception = task.exception

                        if (exception != null) {
                            Firebase.crashlytics.recordException(exception)
                        }

                        // A when-statement is like a switch / case statement in Java
                        when (exception) {
                            is FirebaseAuthWeakPasswordException -> {
                                val bundle = Bundle()
                                bundle.putString("reason", "weak_password")
                                firebaseAnalytics.logEvent("signup_failed", bundle)
                                Toast.makeText(
                                    this,
                                    R.string.signup_failure_weak_password,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is FirebaseAuthUserCollisionException -> {
                                val bundle = Bundle()
                                bundle.putString("reason", "existing_account")
                                firebaseAnalytics.logEvent("signup_failed", bundle)
                                Toast.makeText(
                                    this,
                                    R.string.signup_failure_already_exists,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                val bundle = Bundle()
                                bundle.putString("reason", "invalid_credentials")
                                firebaseAnalytics.logEvent("signup_failed", bundle)
                                Toast.makeText(
                                    this,
                                    R.string.signup_failure_invalid_format,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                val bundle = Bundle()
                                bundle.putString("reason", "generic")
                                firebaseAnalytics.logEvent("signup_failed", bundle)
                                Toast.makeText(
                                    this,
                                    getString(R.string.signup_failure_generic, exception),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
            }
        }

        // Using the same TextWatcher instance for both EditTexts so the same block of code runs on each character.
        username.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)


        // Restore the saved username from SharedPreferences and display it to the user when the screen loads.
        // Default to the empty string if there is no saved username.
        val savedUsername = sharedPrefs.getString("USERNAME", "")
        username.setText(savedUsername)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val title = "Default Notification"
            val description = "All notifications will be posted under this type!"
            val id = "default"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val notificationChannel = NotificationChannel(id, title, importance)
            notificationChannel.description = description

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun showNotification() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        mainActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingMainActivityIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, 0)

        val tweetsActivityIntent = Intent(this, TweetsActivity::class.java)

        val fakeAddress = Address(Locale.getDefault())
        fakeAddress.latitude = 37.54
        fakeAddress.longitude = -77.43
        fakeAddress.locality = "Richmond"
        fakeAddress.adminArea = "Virginia"

        tweetsActivityIntent.putExtra("address", fakeAddress)

        val taskStackBuilder = TaskStackBuilder.create(this)
        taskStackBuilder.addNextIntentWithParentStack(tweetsActivityIntent)

        val stackPendingIntent = taskStackBuilder.getPendingIntent(0, 0)

        // Create notification
        val notification: Notification =
            NotificationCompat.Builder(this, "default")
                .setContentTitle("Welcome to Android Tweets")
                .setContentText("Click this notification to open the app!")
                .setSmallIcon(R.drawable.ic_check)
                .setContentIntent(pendingMainActivityIntent)
                .setAutoCancel(true)
                .addAction(0, "Go to Virginia", stackPendingIntent)
                .build()

        // Show notification to user
        NotificationManagerCompat.from(this).notify(0, notification)
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
            signUp.isEnabled = enableButton
        }

        override fun afterTextChanged(p0: Editable?) {}

    }
}