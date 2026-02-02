package com.transitolibre.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.transitolibre.data.dao.DepartureInfo
import com.transitolibre.data.entity.Stop
import com.transitolibre.data.repository.GtfsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StopDetailViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: GtfsRepository

    private lateinit var viewModel: StopDetailViewModel
    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = StopDetailViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        closeable.close()
    }

    @Test
    fun `initial state is Loading`() {
        assertEquals(StopDetailViewModel.UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `loadStop updates stop and loads departures`() = runTest {
        val testStop = Stop("1", "Test Stop", 48.8566, 2.3522)
        val testDepartures = listOf(
            DepartureInfo(
                tripId = "trip1",
                arrivalTime = "10:00:00",
                departureTime = "10:01:00",
                stopId = "1",
                stopSequence = 1,
                routeShortName = "A",
                routeLongName = "Line A",
                routeColor = "FF0000",
                tripHeadsign = "Destination"
            )
        )

        whenever(repository.getStopById("1")).thenReturn(testStop)
        whenever(repository.getNextDeparturesWithRouteInfo(any(), any(), any()))
            .thenReturn(testDepartures)

        viewModel.loadStop("1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testStop, viewModel.stop.value)
        assertTrue(viewModel.uiState.value is StopDetailViewModel.UiState.Success)
    }

    @Test
    fun `loadStop shows error when stop not found`() = runTest {
        whenever(repository.getStopById("invalid")).thenReturn(null)

        viewModel.loadStop("invalid")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is StopDetailViewModel.UiState.Error)
    }

    @Test
    fun `loadDepartures shows NoDepartures when list is empty`() = runTest {
        whenever(repository.getNextDeparturesWithRouteInfo(any(), any(), any()))
            .thenReturn(emptyList())

        viewModel.loadDepartures("1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StopDetailViewModel.UiState.NoDepartures, viewModel.uiState.value)
    }

    @Test
    fun `setStop updates stop and loads departures`() = runTest {
        val testStop = Stop("1", "Test Stop", 48.8566, 2.3522)
        val testDepartures = listOf(
            DepartureInfo(
                tripId = "trip1",
                arrivalTime = "10:00:00",
                departureTime = "10:01:00",
                stopId = "1",
                stopSequence = 1,
                routeShortName = "A",
                routeLongName = "Line A",
                routeColor = "FF0000",
                tripHeadsign = "Destination"
            )
        )

        whenever(repository.getNextDeparturesWithRouteInfo(any(), any(), any()))
            .thenReturn(testDepartures)

        viewModel.setStop(testStop)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testStop, viewModel.stop.value)
        assertEquals(testDepartures, viewModel.departures.value)
    }
}
