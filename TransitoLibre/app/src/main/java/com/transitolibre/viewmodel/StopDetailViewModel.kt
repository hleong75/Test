package com.transitolibre.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitolibre.data.dao.DepartureInfo
import com.transitolibre.data.entity.Stop
import com.transitolibre.data.repository.GtfsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StopDetailViewModel(
    private val repository: GtfsRepository
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val departures: List<DepartureInfo>) : UiState()
        data class Error(val message: String) : UiState()
        object NoDepartures : UiState()
    }

    private val _uiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState: LiveData<UiState> = _uiState

    private val _stop = MutableLiveData<Stop?>()
    val stop: LiveData<Stop?> = _stop

    private val _departures = MutableLiveData<List<DepartureInfo>>()
    val departures: LiveData<List<DepartureInfo>> = _departures

    fun loadStop(stopId: String) {
        viewModelScope.launch {
            try {
                val stopData = repository.getStopById(stopId)
                _stop.value = stopData
                if (stopData != null) {
                    loadDepartures(stopId)
                } else {
                    _uiState.value = UiState.Error("Arrêt non trouvé")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun loadDepartures(stopId: String, limit: Int = 15) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val currentTime = getCurrentTime()
                val departureList = repository.getNextDeparturesWithRouteInfo(stopId, currentTime, limit)
                _departures.value = departureList

                _uiState.value = if (departureList.isEmpty()) {
                    UiState.NoDepartures
                } else {
                    UiState.Success(departureList)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun setStop(stop: Stop) {
        _stop.value = stop
        loadDepartures(stop.stopId)
    }

    private fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }
}
