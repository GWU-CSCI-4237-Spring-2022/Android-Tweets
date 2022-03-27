package edu.gwu.androidtweetsspring2022

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import edu.gwu.androidtweetsspring2022.databinding.ActivityMapsBinding
import org.jetbrains.anko.doAsync

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var currentAddress: Address? = null

    private lateinit var currentLocation: ImageButton
    private lateinit var confirm: MaterialButton

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var locationProvider: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationProvider = LocationServices.getFusedLocationProviderClient(this)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val currentUser = FirebaseAuth.getInstance().currentUser
        title = getString(R.string.maps_title, currentUser!!.email)

        currentLocation = findViewById(R.id.current_location)
        confirm = findViewById(R.id.confirm)

        confirm.setOnClickListener {
            firebaseAnalytics.logEvent("confirm_clicked", null)
            if (currentAddress != null) {
                val tweetsIntent = Intent(this, TweetsActivity::class.java)
                tweetsIntent.putExtra("address", currentAddress)
                startActivity(tweetsIntent)
            }
        }

        currentLocation.setOnClickListener {
            firebaseAnalytics.logEvent("current_location_clicked", null)
            checkPermission()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun checkPermission() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val permissionResult = ContextCompat.checkSelfPermission(this, permission)
        if(permissionResult == PackageManager.PERMISSION_GRANTED) {
            Log.d("MapsActivity", "Permission check: granted")
            firebaseAnalytics.logEvent("permission_check_granted", null)
            useCurrentLocation()
        } else {
            Log.d("MapsActivity", "Permission check: not granted")
            firebaseAnalytics.logEvent("permission_check_not_granted", null)

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                200
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun useCurrentLocation() {
        firebaseAnalytics.logEvent("getting_current_location", null)

        // Using the Last Location would be fine for Android Tweets, but instead I'll show getting
        // a "fresh" location
        //
        // locationProvider.lastLocation.addOnSuccessListener { location: Location ->
        //     Log.d("MapsActivity", "Last location: ${location.latitude}, ${location.longitude}")
        //     doGeocoding(LatLng(location.latitude, location.longitude))
        // }

        val locationRequest = LocationRequest.create()
        locationRequest.interval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        locationProvider.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper() // Receive callbacks on main thread
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            firebaseAnalytics.logEvent("received_loccation_update", null)

            val location = result.lastLocation

            locationProvider.removeLocationUpdates(this)

            doGeocoding(LatLng(location.latitude, location.longitude))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 200) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MapsActivity", "Request permission result: granted")
                firebaseAnalytics.logEvent("request_permission_result_granted", null)
                useCurrentLocation()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                Log.d("MapsActivity", "Request permission result: regular deny")
                firebaseAnalytics.logEvent("request_permission_result_deny", null)
            } else {
                Log.d("MapsActivity", "Request permission result: deny, do not ask again")
                firebaseAnalytics.logEvent("request_permission_result_deny_donotask", null)
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapLongClickListener { coords: LatLng ->
            firebaseAnalytics.logEvent("map_long_press", null)
            doGeocoding(coords)
        }
    }

    private fun doGeocoding(coords: LatLng) {
        mMap.clear()

        doAsync {
            val geocoder: Geocoder = Geocoder(this@MapsActivity)
            val results: List<Address> = try {
                geocoder.getFromLocation(
                    coords.latitude,
                    coords.longitude,
                    10
                )
            } catch(exception: Exception) {
                firebaseAnalytics.logEvent("geocoding_failed", null)
                Firebase.crashlytics.recordException(exception)
                Log.e("MapsActivity", "Geocoding failed!", exception)
                listOf()
            }

            runOnUiThread {
                if (results.isNotEmpty()) {
                    firebaseAnalytics.logEvent("geocoding_success", Bundle().apply {
                        putString("count", "" + results.size)
                    })

                    val firstResult = results[0]
                    val addressLine = firstResult.getAddressLine(0)

                    val marker = MarkerOptions()
                        .position(coords)
                        .title(addressLine)

                    mMap.addMarker(marker)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 10.0f))

                    updateConfirmButton(firstResult)
                } else {
                    firebaseAnalytics.logEvent("no_geocoding_results", null)
                    Toast.makeText(this@MapsActivity, "No results found!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateConfirmButton(address: Address) {
        // Flip button to green
        // Change icon to check
        currentAddress = address
        confirm.icon = AppCompatResources.getDrawable(this, R.drawable.ic_check)
        confirm.text = address.getAddressLine(0)
        confirm.setBackgroundColor(getColor(R.color.buttonGreen))
    }
}