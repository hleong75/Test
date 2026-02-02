package com.transitolibre.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.transitolibre.R
import com.transitolibre.data.entity.Stop
import com.transitolibre.databinding.ActivityMainBinding
import com.transitolibre.databinding.BottomSheetStopDetailBinding
import com.transitolibre.ui.detail.DepartureAdapter
import com.transitolibre.ui.itinerary.ItineraryActivity
import com.transitolibre.viewmodel.MainViewModel
import com.transitolibre.viewmodel.StopDetailViewModel
import com.transitolibre.worker.GtfsImportWorker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBinding: BottomSheetStopDetailBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private val viewModel: MainViewModel by viewModel()
    private val stopDetailViewModel: StopDetailViewModel by viewModel()

    private var mapLibreMap: MapLibreMap? = null

    private val searchAdapter = StopSearchAdapter { stop ->
        onStopSelected(stop)
    }

    private val departureAdapter = DepartureAdapter()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableLocationComponent()
            }
            else -> {
                Toast.makeText(this, R.string.error_gps_disabled, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { startGtfsImport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap(savedInstanceState)
        setupBottomSheet()
        setupSearch()
        setupFabs()
        setupImportButton()
        observeViewModel()
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            mapLibreMap = map

            map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) { style ->
                // Set default camera position (France)
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(46.603354, 1.888334))
                    .zoom(5.0)
                    .build()

                // Request location permission
                checkLocationPermission()

                // Listen for map movements to load visible stops
                map.addOnCameraIdleListener {
                    loadVisibleStops()
                }
            }

            // Handle marker clicks
            map.addOnMapClickListener { latLng ->
                // Find nearest stop to click
                viewModel.nearbyStops.value?.minByOrNull { stop ->
                    val stopLatLng = LatLng(stop.lat, stop.lon)
                    latLng.distanceTo(stopLatLng)
                }?.let { nearestStop ->
                    val stopLatLng = LatLng(nearestStop.lat, nearestStop.lon)
                    if (latLng.distanceTo(stopLatLng) < 100) { // Within 100 meters
                        onStopSelected(nearestStop)
                        return@addOnMapClickListener true
                    }
                }
                false
            }
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBinding = BottomSheetStopDetailBinding.bind(
            binding.bottomSheetContainer.findViewById(R.id.bottomSheet)
        )

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetBinding.root)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBinding.rvDepartures.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = departureAdapter
        }
    }

    private fun setupSearch() {
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchAdapter
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    binding.searchResultsCard.visibility = View.GONE
                    searchAdapter.submitList(emptyList())
                } else {
                    viewModel.searchStops(newText)
                }
                return true
            }
        })

        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.searchResultsCard.visibility = View.GONE
            }
        }
    }

    private fun setupFabs() {
        binding.fabMyLocation.setOnClickListener {
            centerOnUserLocation()
        }

        binding.fabItinerary.setOnClickListener {
            startActivity(Intent(this, ItineraryActivity::class.java))
        }
    }

    private fun setupImportButton() {
        binding.btnImport.setOnClickListener {
            filePickerLauncher.launch("application/zip")
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(this) { results ->
            if (results.isNotEmpty()) {
                binding.searchResultsCard.visibility = View.VISIBLE
                searchAdapter.submitList(results)
            } else {
                binding.searchResultsCard.visibility = View.GONE
            }
        }

        viewModel.nearbyStops.observe(this) { stops ->
            updateMapMarkers(stops)
        }

        viewModel.isImporting.observe(this) { isImporting ->
            binding.importProgressCard.visibility = if (isImporting) View.VISIBLE else View.GONE
        }

        viewModel.importProgress.observe(this) { progress ->
            binding.importProgressBar.progress = progress
            binding.importProgressPercent.text = "$progress%"
        }

        viewModel.importMessage.observe(this) { message ->
            binding.importProgressMessage.text = message ?: getString(R.string.loading)
        }

        // Observe stop detail ViewModel
        stopDetailViewModel.uiState.observe(this) { state ->
            when (state) {
                is StopDetailViewModel.UiState.Loading -> {
                    bottomSheetBinding.progressLoading.visibility = View.VISIBLE
                    bottomSheetBinding.rvDepartures.visibility = View.GONE
                    bottomSheetBinding.tvEmptyState.visibility = View.GONE
                }
                is StopDetailViewModel.UiState.Success -> {
                    bottomSheetBinding.progressLoading.visibility = View.GONE
                    bottomSheetBinding.rvDepartures.visibility = View.VISIBLE
                    bottomSheetBinding.tvEmptyState.visibility = View.GONE
                    departureAdapter.submitList(state.departures)
                }
                is StopDetailViewModel.UiState.NoDepartures -> {
                    bottomSheetBinding.progressLoading.visibility = View.GONE
                    bottomSheetBinding.rvDepartures.visibility = View.GONE
                    bottomSheetBinding.tvEmptyState.visibility = View.VISIBLE
                }
                is StopDetailViewModel.UiState.Error -> {
                    bottomSheetBinding.progressLoading.visibility = View.GONE
                    bottomSheetBinding.rvDepartures.visibility = View.GONE
                    bottomSheetBinding.tvEmptyState.visibility = View.VISIBLE
                    bottomSheetBinding.tvEmptyState.text = state.message
                }
            }
        }

        stopDetailViewModel.stop.observe(this) { stop ->
            stop?.let {
                bottomSheetBinding.tvStopName.text = it.name
            }
        }
    }

    private fun onStopSelected(stop: Stop) {
        // Hide search results
        binding.searchResultsCard.visibility = View.GONE
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()

        // Center map on stop
        mapLibreMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(stop.lat, stop.lon), 16.0)
        )

        // Show bottom sheet with stop details
        stopDetailViewModel.setStop(stop)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun updateMapMarkers(stops: List<Stop>) {
        // Markers would be added using GeoJSON source and symbol layer
        // For now, stops are accessible via map click handler
        // TODO: Add proper marker rendering with GeoJSON source
    }

    private fun loadVisibleStops() {
        mapLibreMap?.let { map ->
            val bounds = map.projection.visibleRegion.latLngBounds
            viewModel.loadStopsInBounds(
                bounds.latitudeSouth,
                bounds.latitudeNorth,
                bounds.longitudeWest,
                bounds.longitudeEast
            )
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableLocationComponent()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableLocationComponent() {
        mapLibreMap?.style?.let { style ->
            try {
                val locationComponent = mapLibreMap?.locationComponent

                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationComponent?.activateLocationComponent(
                        LocationComponentActivationOptions.builder(this, style).build()
                    )
                    locationComponent?.isLocationComponentEnabled = true
                    locationComponent?.cameraMode = CameraMode.NONE
                    locationComponent?.renderMode = RenderMode.COMPASS
                }
            } catch (e: Exception) {
                Toast.makeText(this, R.string.error_gps_disabled, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun centerOnUserLocation() {
        mapLibreMap?.locationComponent?.lastKnownLocation?.let { location ->
            mapLibreMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    15.0
                )
            )
        } ?: run {
            Toast.makeText(this, R.string.error_gps_disabled, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGtfsImport(uri: Uri) {
        viewModel.setImporting(true)
        viewModel.setGtfsFileUri(uri)

        val workRequest = GtfsImportWorker.createWorkRequest(uri)

        WorkManager.getInstance(this).enqueueUniqueWork(
            GtfsImportWorker.WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )

        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(workRequest.id)
            .observe(this) { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(GtfsImportWorker.KEY_PROGRESS, 0)
                        val message = workInfo.progress.getString(GtfsImportWorker.KEY_MESSAGE)
                        viewModel.setImportProgress(progress)
                        viewModel.setImportMessage(message)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        viewModel.onImportComplete()
                        Toast.makeText(this, R.string.parsing_complete, Toast.LENGTH_SHORT).show()
                    }
                    WorkInfo.State.FAILED -> {
                        viewModel.onImportComplete()
                        val errorMessage = workInfo.outputData.getString(GtfsImportWorker.KEY_MESSAGE)
                        Toast.makeText(
                            this,
                            errorMessage ?: getString(R.string.parsing_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> { }
                }
            }
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
