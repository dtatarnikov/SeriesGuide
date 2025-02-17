// SPDX-License-Identifier: Apache-2.0
// Copyright 2014, 2016-2018, 2020-2021, 2023-2024 Uwe Trottmann

package com.battlelancer.seriesguide.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.settings.DisplaySettings.isVeryHighDensityScreen

object TmdbSettings {

    private const val KEY_LAST_UPDATED = "com.uwetrottmann.seriesguide.tmdb.lastupdated"

    /* If the image URL changes the old one should be working for a while. If watch providers
    change, typically only affects new shows or new seasons. As users likely check a show once a
     week, updating weekly should be fine. */
    private const val UPDATE_INTERVAL_MS = 7 * DateUtils.DAY_IN_MILLIS

    private const val KEY_TMDB_BASE_URL = "com.battlelancer.seriesguide.tmdb.baseurl"
    const val POSTER_SIZE_SPEC_W154 = "w154"
    const val POSTER_SIZE_SPEC_W342 = "w342"

    /**
     * As of 2024-11:
     *
     * - w300 (300 × 169 px) JPEG is around 9-16 kB
     * - w780 (780 × 439 px) JPEG is around 30-70 kB
     * - w1280 (1280 × 720 px) JPEG is around 60-140 KB
     *
     * Samples:
     *
     * - https://image.tmdb.org/t/p/original/8Tvnx22rzhwArofFPhmcfaBvgjN.jpg (smallest)
     * - https://image.tmdb.org/t/p/original/j4WEC9Jh4AyXF8ynpX3pz633tse.jpg
     * - https://image.tmdb.org/t/p/original/5Bh7EE3p6OOS0NzH22AE0N7DYO8.jpg (largest)
     */
    const val BACKDROP_SMALL_SIZE_SPEC = "w780"
    private const val IMAGE_SIZE_SPEC_ORIGINAL = "original"
    const val DEFAULT_BASE_URL = "https://image.tmdb.org/t/p/"

    fun isConfigurationUpToDate(context: Context): Boolean {
        val lastUpdatedMs =
            PreferenceManager.getDefaultSharedPreferences(context).getLong(KEY_LAST_UPDATED, 0)
        return lastUpdatedMs + UPDATE_INTERVAL_MS >= System.currentTimeMillis()
    }

    fun setConfigurationLastUpdatedNow(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
        }
    }

    /**
     * Saves the base URL, unless it's empty or blank.
     */
    fun setImageBaseUrl(context: Context, url: String) {
        if (url.isEmpty() || url.isBlank()) return
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_TMDB_BASE_URL, url)
        }
    }

    @JvmStatic
    fun getImageBaseUrl(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_TMDB_BASE_URL, null) ?: DEFAULT_BASE_URL
    }

    fun getImageOriginalUrl(context: Context, path: String): String {
        return getImageBaseUrl(context) + IMAGE_SIZE_SPEC_ORIGINAL + path
    }

    /**
     * Returns base image URL based on screen density.
     */
    @JvmStatic
    fun getPosterBaseUrl(context: Context): String {
        return if (isVeryHighDensityScreen(context)) {
            getImageBaseUrl(context) + POSTER_SIZE_SPEC_W342
        } else {
            getImageBaseUrl(context) + POSTER_SIZE_SPEC_W154
        }
    }

    fun buildBackdropUrl(context: Context, path: String): String {
        return "${getImageBaseUrl(context)}$BACKDROP_SMALL_SIZE_SPEC$path"
    }
}
