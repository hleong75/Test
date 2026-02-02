package com.transitolibre.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitolibre.data.entity.Stop
import com.transitolibre.data.repository.GtfsRepository
import kotlinx.coroutines.launch

class ItineraryViewModel(
    private val repository: GtfsRepository
) : ViewModel() {

    data class ItineraryPoint(
        val name: String,
        val lat: Double,
        val lon: Double,
        val isCurrentLocation: Boolean = false
    )

    data class RouteResult(
        val polyline: List<Pair<Double, Double>>,
        val distance: Double,
        val duration: Int,
        val instructions: List<RouteInstruction>
    )

    data class RouteInstruction(
        val text: String,
        val distance: Double,
        val duration: Int
    )

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val route: RouteResult) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _departure = MutableLiveData<ItineraryPoint?>()
    val departure: LiveData<ItineraryPoint?> = _departure

    private val _arrival = MutableLiveData<ItineraryPoint?>()
    val arrival: LiveData<ItineraryPoint?> = _arrival

    private val _searchResults = MutableLiveData<List<Stop>>()
    val searchResults: LiveData<List<Stop>> = _searchResults

    private val _routeResult = MutableLiveData<RouteResult?>()
    val routeResult: LiveData<RouteResult?> = _routeResult

    fun setDepartureLocation(lat: Double, lon: Double) {
        _departure.value = ItineraryPoint(
            name = "Ma position",
            lat = lat,
            lon = lon,
            isCurrentLocation = true
        )
    }

    fun setDepartureStop(stop: Stop) {
        _departure.value = ItineraryPoint(
            name = stop.name,
            lat = stop.lat,
            lon = stop.lon,
            isCurrentLocation = false
        )
    }

    fun setArrivalStop(stop: Stop) {
        _arrival.value = ItineraryPoint(
            name = stop.name,
            lat = stop.lat,
            lon = stop.lon,
            isCurrentLocation = false
        )
    }

    fun searchStops(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val results = repository.searchStops(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun calculateRoute() {
        val dep = _departure.value ?: return
        val arr = _arrival.value ?: return

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Placeholder for GraphHopper API call
                // In production, this would call the GraphHopper routing API
                val mockRoute = RouteResult(
                    polyline = listOf(
                        Pair(dep.lat, dep.lon),
                        Pair(arr.lat, arr.lon)
                    ),
                    distance = calculateDistance(dep.lat, dep.lon, arr.lat, arr.lon),
                    duration = 600,
                    instructions = listOf(
                        RouteInstruction("Départ de ${dep.name}", 0.0, 0),
                        RouteInstruction("Arrivée à ${arr.name}", 0.0, 0)
                    )
                )
                _routeResult.value = mockRoute
                _uiState.value = UiState.Success(mockRoute)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Erreur de calcul d'itinéraire")
            }
        }
    }

    fun clearRoute() {
        _routeResult.value = null
        _uiState.value = UiState.Idle
    }

    fun swapDepartureArrival() {
        val tempDeparture = _departure.value
        _departure.value = _arrival.value
        _arrival.value = tempDeparture
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c * 1000
    }
}
