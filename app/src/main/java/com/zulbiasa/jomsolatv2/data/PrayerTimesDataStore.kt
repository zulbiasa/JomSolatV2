package com.zulbiasa.jomsolatv2.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("prayer_times")

class PrayerTimesDataStore(private val context: Context) {

    companion object {
        val FAJR = longPreferencesKey("fajr")
        val DHUHR = longPreferencesKey("dhuhr")
        val ASR = longPreferencesKey("asr")
        val MAGHRIB = longPreferencesKey("maghrib")
        val ISHA = longPreferencesKey("isha")
        val LOCATION = stringPreferencesKey("location")
    }

    suspend fun savePrayerTimes(times: Map<String, String>) {
        context.dataStore.edit { prefs ->
            prefs[FAJR] = times["Fajr"]?.toLongOrNull() ?: 0L
            prefs[DHUHR] = times["Zohor"]?.toLongOrNull() ?: 0L
            prefs[ASR] = times["Asar"]?.toLongOrNull() ?: 0L
            prefs[MAGHRIB] = times["Maghrib"]?.toLongOrNull() ?: 0L
            prefs[ISHA] = times["Isyak"]?.toLongOrNull() ?: 0L
            prefs[LOCATION] = times["locationName"] ?: "Unknown"
        }
    }

    fun readPrayerTimes(): Flow<Map<String, String>> =
        context.dataStore.data.map { prefs ->
            mapOf(
                "Fajr" to prefs[FAJR].toString(),
                "Zohor" to prefs[DHUHR].toString(),
                "Asar" to prefs[ASR].toString(),
                "Maghrib" to prefs[MAGHRIB].toString(),
                "Isyak" to prefs[ISHA].toString(),
                "location" to (prefs[LOCATION] ?: "Unknown")
            )
        }
}