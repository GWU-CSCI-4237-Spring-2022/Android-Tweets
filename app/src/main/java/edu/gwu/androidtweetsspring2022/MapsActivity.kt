package edu.gwu.androidtweetsspring2022

import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import edu.gwu.androidtweetsspring2022.databinding.ActivityMapsBinding
import org.jetbrains.anko.doAsync

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapLongClickListener { coords: LatLng ->
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
                    Log.e("MapsActivity", "Geocoding failed!", exception)
                    listOf()
                }

                runOnUiThread {
                    if (results.isNotEmpty()) {
                        val firstResult = results[0]
                        val addressLine = firstResult.getAddressLine(0)

                        val marker = MarkerOptions()
                            .position(coords)
                            .title(addressLine)

                        mMap.addMarker(marker)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 10.0f))
                    } else {
                        Toast.makeText(this@MapsActivity, "No results found!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}