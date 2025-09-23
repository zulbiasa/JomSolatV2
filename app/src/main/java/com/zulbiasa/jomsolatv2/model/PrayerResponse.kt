package com.zulbiasa.jomsolatv2.model

data class PrayerResponse(
    val zone: String,
    val year: Int,
    val month: String,
    val month_number: Int,
    val last_updated: String?,
    val prayers: List<PrayerDay>
)

data class PrayerDay(
    val day: Int,
    val hijri: String,
    val fajr: Long,
    val syuruk: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long
)
