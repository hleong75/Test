package com.transitolibre.di

import com.transitolibre.data.database.GtfsDatabase
import com.transitolibre.data.repository.GtfsRepository
import com.transitolibre.parser.GtfsParser
import com.transitolibre.viewmodel.MainViewModel
import com.transitolibre.viewmodel.StopDetailViewModel
import com.transitolibre.viewmodel.ItineraryViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Database
    single { GtfsDatabase.getInstance(androidContext()) }

    // DAOs
    single { get<GtfsDatabase>().agencyDao() }
    single { get<GtfsDatabase>().stopDao() }
    single { get<GtfsDatabase>().routeDao() }
    single { get<GtfsDatabase>().tripDao() }
    single { get<GtfsDatabase>().stopTimeDao() }
    single { get<GtfsDatabase>().calendarDao() }

    // Repository
    single {
        GtfsRepository(
            agencyDao = get(),
            stopDao = get(),
            routeDao = get(),
            tripDao = get(),
            stopTimeDao = get(),
            calendarDao = get()
        )
    }

    // Parser
    factory { GtfsParser() }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { StopDetailViewModel(get()) }
    viewModel { ItineraryViewModel(get()) }
}
