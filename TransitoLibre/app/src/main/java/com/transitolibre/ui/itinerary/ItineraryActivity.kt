package com.transitolibre.ui.itinerary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.transitolibre.R
import com.transitolibre.databinding.ActivityItineraryBinding
import com.transitolibre.viewmodel.ItineraryViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

class ItineraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItineraryBinding
    private val viewModel: ItineraryViewModel by viewModel()
    private var mapLibreMap: MapLibreMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        binding = ActivityItineraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap(savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            mapLibreMap = map

            map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) { style ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(46.603354, 1.888334))
                    .zoom(5.0)
                    .build()

                enableLocationComponent(style)
            }
        }
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSwap.setOnClickListener {
            viewModel.swapDepartureArrival()
            val depText = binding.etDeparture.text.toString()
            binding.etDeparture.setText(binding.etArrival.text)
            binding.etArrival.setText(depText)
        }

        binding.btnCalculate.setOnClickListener {
            val arrivalText = binding.etArrival.text.toString()
            if (arrivalText.isNotBlank()) {
                viewModel.calculateRoute()
            } else {
                Toast.makeText(this, R.string.arrival, Toast.LENGTH_SHORT).show()
            }
        }

        // Set current location as departure by default
        setCurrentLocationAsDeparture()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ItineraryViewModel.UiState.Loading -> {
                    binding.progressLoading.visibility = View.VISIBLE
                }
                is ItineraryViewModel.UiState.Success -> {
                    binding.progressLoading.visibility = View.GONE
                    drawRoute(state.route)
                }
                is ItineraryViewModel.UiState.Error -> {
                    binding.progressLoading.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is ItineraryViewModel.UiState.Idle -> {
                    binding.progressLoading.visibility = View.GONE
                }
            }
        }

        viewModel.departure.observe(this) { point ->
            point?.let {
                if (!it.isCurrentLocation) {
                    binding.etDeparture.setText(it.name)
                }
            }
        }

        viewModel.arrival.observe(this) { point ->
            point?.let {
                binding.etArrival.setText(it.name)
            }
        }
    }

    private fun setCurrentLocationAsDeparture() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mapLibreMap?.locationComponent?.lastKnownLocation?.let { location ->
                viewModel.setDepartureLocation(location.latitude, location.longitude)
            }
        }
    }

    private fun enableLocationComponent(style: Style) {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val locationComponent = mapLibreMap?.locationComponent
                locationComponent?.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, style).build()
                )
                locationComponent?.isLocationComponentEnabled = true
                locationComponent?.cameraMode = CameraMode.NONE
                locationComponent?.renderMode = RenderMode.COMPASS

                // Get current location for departure
                locationComponent?.lastKnownLocation?.let { location ->
                    viewModel.setDepartureLocation(location.latitude, location.longitude)
                }
            }
        } catch (e: Exception) {
            // Handle silently
        }
    }

    private fun drawRoute(route: ItineraryViewModel.RouteResult) {
        // In a full implementation, this would draw a polyline on the map
        // For now, just show a toast with route info
        val distanceKm = route.distance / 1000
        val durationMin = route.duration / 60
        Toast.makeText(
            this,
            "Distance: %.1f km, Durée: %d min".format(distanceKm, durationMin),
            Toast.LENGTH_LONG
        ).show()
    }

    // Lifecycle methods for MapView
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
}
