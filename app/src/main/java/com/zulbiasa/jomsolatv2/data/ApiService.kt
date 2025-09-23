package com.zulbiasa.jomsolatv2.data

import com.zulbiasa.jomsolatv2.model.PrayerResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("v2/solat/gps/{lat}/{long}")
    suspend fun getPrayerTimesByGps(
        @Path("lat") lat: Double,
        @Path("long") long: Double
    ): PrayerResponse
}
