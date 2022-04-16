package edu.gwu.androidtweetsspring2022

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityTestRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testLoginSuccess() {
        val username = onView(withHint("Username"))
        val password = onView(withHint("Password"))
        val login = onView(withId(R.id.login))
        val signUp = onView(withId(R.id.signUp))

        username.perform(clearText())

        login.check(matches(not(isEnabled())))
        signUp.check(matches(not(isEnabled())))

        username.perform(typeText("nickasdfasdf@gwu.edu"))
        password.perform(typeText("abcd12345"))

        login.check(matches(isEnabled()))
        signUp.check(matches(isEnabled()))

        login.perform(click())

        val mapsTitle = onView(withText("Welcome, nick@gwu.edu"))
        mapsTitle.check(matches(isDisplayed()))
    }
}