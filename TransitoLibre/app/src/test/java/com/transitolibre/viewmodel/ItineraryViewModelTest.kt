package com.transitolibre.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.transitolibre.data.entity.Stop
import com.transitolibre.data.repository.GtfsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: GtfsRepository

    private lateinit var viewModel: ItineraryViewModel
    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = ItineraryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        closeable.close()
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(ItineraryViewModel.UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `setDepartureLocation updates departure`() {
        viewModel.setDepartureLocation(48.8566, 2.3522)

        val departure = viewModel.departure.value
        assertNotNull(departure)
        assertEquals(48.8566, departure?.lat ?: 0.0, 0.001)
        assertEquals(2.3522, departure?.lon ?: 0.0, 0.001)
        assertTrue(departure?.isCurrentLocation == true)
    }

    @Test
    fun `setDepartureStop updates departure`() {
        val stop = Stop("1", "Gare Centrale", 48.8566, 2.3522)
        viewModel.setDepartureStop(stop)

        val departure = viewModel.departure.value
        assertNotNull(departure)
        assertEquals("Gare Centrale", departure?.name)
        assertFalse(departure?.isCurrentLocation == true)
    }

    @Test
    fun `setArrivalStop updates arrival`() {
        val stop = Stop("2", "Gare du Nord", 48.8809, 2.3553)
        viewModel.setArrivalStop(stop)

        val arrival = viewModel.arrival.value
        assertNotNull(arrival)
        assertEquals("Gare du Nord", arrival?.name)
    }

    @Test
    fun `swapDepartureArrival exchanges values`() {
        val stopDep = Stop("1", "Gare Centrale", 48.8566, 2.3522)
        val stopArr = Stop("2", "Gare du Nord", 48.8809, 2.3553)

        viewModel.setDepartureStop(stopDep)
        viewModel.setArrivalStop(stopArr)
        viewModel.swapDepartureArrival()

        assertEquals("Gare du Nord", viewModel.departure.value?.name)
        assertEquals("Gare Centrale", viewModel.arrival.value?.name)
    }

    @Test
    fun `calculateRoute updates state to Success`() = runTest {
        viewModel.setDepartureLocation(48.8566, 2.3522)
        viewModel.setArrivalStop(Stop("2", "Gare du Nord", 48.8809, 2.3553))

        viewModel.calculateRoute()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ItineraryViewModel.UiState.Success)
        assertNotNull(viewModel.routeResult.value)
    }

    @Test
    fun `clearRoute resets state to Idle`() = runTest {
        viewModel.setDepartureLocation(48.8566, 2.3522)
        viewModel.setArrivalStop(Stop("2", "Gare du Nord", 48.8809, 2.3553))
        viewModel.calculateRoute()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearRoute()

        assertEquals(ItineraryViewModel.UiState.Idle, viewModel.uiState.value)
        assertNull(viewModel.routeResult.value)
    }

    @Test
    fun `searchStops returns empty for blank query`() = runTest {
        viewModel.searchStops("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.searchResults.value?.isEmpty() == true)
    }

    @Test
    fun `searchStops returns results for valid query`() = runTest {
        val testStops = listOf(
            Stop("1", "Gare Centrale", 48.8566, 2.3522)
        )
        whenever(repository.searchStops("Gare")).thenReturn(testStops)

        viewModel.searchStops("Gare")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.searchResults.value?.size)
    }
}
