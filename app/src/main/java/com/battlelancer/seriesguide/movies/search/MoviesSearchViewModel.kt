// SPDX-License-Identifier: Apache-2.0
// Copyright 2020-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.MoviesDiscoverLink
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.movies.TmdbMoviesDataSource
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.uwetrottmann.tmdb2.entities.BaseMovie
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(FlowPreview::class)
class MoviesSearchViewModel(
    application: Application,
    link: MoviesDiscoverLink
) : AndroidViewModel(application) {

    data class Filters(
        val queryString: String,
        val releaseYear: Int?,
        val originalLanguage: String?,
        val watchProviderIds: List<Int>?,
    )

    private val queryString = MutableStateFlow("")
    private val releaseYear = MutableStateFlow<Int?>(null)
    private val originalLanguage = MutableStateFlow<String?>(null)
    private val watchProviderIds =
        SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
            .getEnabledWatchProviderIdsFlow(SgWatchProvider.Type.MOVIES.id)

    private val tmdb = SgApp.getServicesComponent(application).tmdb()

    val items: Flow<PagingData<BaseMovie>> = combine(
        watchProviderIds,
        queryString,
        releaseYear,
        originalLanguage
    ) { watchProviderIds: List<Int>, queryString: String, releaseYear: Int?, originalLanguage: String? ->
        Filters(queryString, releaseYear, originalLanguage, watchProviderIds)
    }
        .debounce(200) // below 300ms to not be perceived as lag
        .flatMapLatest {
            Pager(
                // Note: currently TMDB page is 20 items, on phones around 9 are displayed at once.
                PagingConfig(pageSize = 20, enablePlaceholders = true)
            ) {
                val languageCode = MoviesSettings.getMoviesLanguage(application)
                val regionCode = MoviesSettings.getMoviesRegion(application)
                val watchRegion = StreamingSearch.getCurrentRegionOrNull(application)
                TmdbMoviesDataSource(
                    context = application,
                    tmdb = tmdb,
                    link = link,
                    query = it.queryString,
                    languageCode = languageCode,
                    regionCode = regionCode,
                    releaseYear = it.releaseYear,
                    originalLanguageCode = it.originalLanguage,
                    watchProviderIds = it.watchProviderIds,
                    watchRegion = watchRegion
                )
            }.flow
        }
        .cachedIn(viewModelScope)

    fun updateQuery(query: String) {
        queryString.value = query
    }

    fun updateYear(year: Int?) {
        releaseYear.value = year
    }

    fun updateLanguage(languageCode: String?) {
        originalLanguage.value = languageCode
    }

}

class MoviesSearchViewModelFactory(
    private val application: Application,
    private val link: MoviesDiscoverLink
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MoviesSearchViewModel(application, link) as T
    }

}
