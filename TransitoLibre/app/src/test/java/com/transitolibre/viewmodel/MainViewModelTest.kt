package com.transitolibre.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.transitolibre.data.dao.*
import com.transitolibre.data.entity.Stop
import com.transitolibre.data.repository.DatabaseStats
import com.transitolibre.data.repository.GtfsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: GtfsRepository

    private lateinit var viewModel: MainViewModel
    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        closeable.close()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        whenever(repository.getStatistics()).thenReturn(
            DatabaseStats(0, 0, 0, 0, 0, 0)
        )

        viewModel = MainViewModel(repository)

        assertEquals(MainViewModel.UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `loadDatabaseStats updates state to Empty when no stops`() = runTest {
        whenever(repository.getStatistics()).thenReturn(
            DatabaseStats(0, 0, 0, 0, 0, 0)
        )

        viewModel = MainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MainViewModel.UiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `loadDatabaseStats updates state to Success when stops exist`() = runTest {
        whenever(repository.getStatistics()).thenReturn(
            DatabaseStats(1, 100, 10, 50, 1000, 5)
        )

        viewModel = MainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MainViewModel.UiState.Success)
    }

    @Test
    fun `searchStops returns empty list for blank query`() = runTest {
        whenever(repository.getStatistics()).thenReturn(
            DatabaseStats(0, 0, 0, 0, 0, 0)
        )

        viewModel = MainViewModel(repository)
        viewModel.searchStops("")

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.searchResults.value?.isEmpty() == true)
    }

    @Test
    fun `searchStops returns results for valid query`() = runTest {
        val testStops = listOf(
            Stop("1", "Gare Centrale", 48.8566, 2.3522),
            Stop("2", "Gare du Nord", 48.8809, 2.3553)
        )
        whenever(repository.getStatistics()).thenReturn(
            DatabaseStats(0, 2, 0, 0, 0, 0)
        )
        whenever(repository.searchStops("Gare")).thenReturn(testStops)

        viewModel = MainViewModel(repository)
        viewModel.searchStops("Gare")

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.searchResults.value?.size)
    }

    @Test
    fun `selectStop updates selectedStop`() = runTest {
        whenever(repository.getStatistics()).thenReturn(
            DatabaseStats(0, 1, 0, 0, 0, 0)
        )

        val stop = Stop("1", "Test Stop", 48.8566, 2.3522)
        viewModel = MainViewModel(repository)
        viewModel.selectStop(stop)

        assertEquals(stop, viewModel.selectedStop.value)
    }

    @Test
    fun `setImporting updates isImporting state`() = runTest {
        whenever(repository.getStatistics()).thenReturn(
            DatabaseStats(0, 0, 0, 0, 0, 0)
        )

        viewModel = MainViewModel(repository)
        viewModel.setImporting(true)

        assertEquals(true, viewModel.isImporting.value)
    }

    @Test
    fun `onImportComplete resets import state`() = runTest {
        whenever(repository.getStatistics()).thenReturn(
            DatabaseStats(0, 100, 0, 0, 0, 0)
        )

        viewModel = MainViewModel(repository)
        viewModel.setImporting(true)
        viewModel.setImportProgress(50)
        viewModel.onImportComplete()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.isImporting.value)
        assertEquals(0, viewModel.importProgress.value)
    }
}
