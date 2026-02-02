package com.transitolibre.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitolibre.data.entity.Stop
import com.transitolibre.data.repository.DatabaseStats
import com.transitolibre.data.repository.GtfsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: GtfsRepository
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        object Empty : UiState()
        data class Success(val stops: List<Stop>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _nearbyStops = MutableLiveData<List<Stop>>()
    val nearbyStops: LiveData<List<Stop>> = _nearbyStops

    private val _searchResults = MutableLiveData<List<Stop>>()
    val searchResults: LiveData<List<Stop>> = _searchResults

    private val _selectedStop = MutableLiveData<Stop?>()
    val selectedStop: LiveData<Stop?> = _selectedStop

    private val _databaseStats = MutableLiveData<DatabaseStats?>()
    val databaseStats: LiveData<DatabaseStats?> = _databaseStats

    private val _isImporting = MutableLiveData<Boolean>(false)
    val isImporting: LiveData<Boolean> = _isImporting

    private val _importProgress = MutableLiveData<Int>(0)
    val importProgress: LiveData<Int> = _importProgress

    private val _importMessage = MutableLiveData<String?>()
    val importMessage: LiveData<String?> = _importMessage

    private val _gtfsFileUri = MutableLiveData<Uri?>()
    val gtfsFileUri: LiveData<Uri?> = _gtfsFileUri

    init {
        loadDatabaseStats()
    }

    fun loadDatabaseStats() {
        viewModelScope.launch {
            try {
                val stats = repository.getStatistics()
                _databaseStats.value = stats
                _uiState.value = if (stats.stopCount == 0) UiState.Empty else UiState.Success(emptyList())
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadNearbyStops(lat: Double, lon: Double, limit: Int = 50) {
        viewModelScope.launch {
            try {
                val stops = repository.getNearestStops(lat, lon, limit)
                _nearbyStops.value = stops
            } catch (e: Exception) {
                // Handle error silently, don't crash
            }
        }
    }

    fun loadStopsInBounds(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) {
        viewModelScope.launch {
            try {
                val stops = repository.getStopsInBounds(minLat, maxLat, minLon, maxLon)
                _nearbyStops.value = stops
                _uiState.value = UiState.Success(stops)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
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

    fun selectStop(stop: Stop?) {
        _selectedStop.value = stop
    }

    fun setGtfsFileUri(uri: Uri) {
        _gtfsFileUri.value = uri
    }

    fun setImporting(importing: Boolean) {
        _isImporting.value = importing
    }

    fun setImportProgress(progress: Int) {
        _importProgress.value = progress
    }

    fun setImportMessage(message: String?) {
        _importMessage.value = message
    }

    fun onImportComplete() {
        _isImporting.value = false
        _importProgress.value = 0
        _importMessage.value = null
        loadDatabaseStats()
    }
}
