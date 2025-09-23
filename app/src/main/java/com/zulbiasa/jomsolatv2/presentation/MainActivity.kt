package com.zulbiasa.jomsolatv2.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.zulbiasa.jomsolatv2.R
import com.zulbiasa.jomsolatv2.data.PrayerTimesDataStore
import com.zulbiasa.jomsolatv2.data.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private val prayerStore by lazy { PrayerTimesDataStore(this) }
    val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur") // timezone Malaysia
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) getLocationAndFetchPrayerTimes()
            else textView.text = "Permission denied. Cannot fetch location."
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> getLocationAndFetchPrayerTimes()
            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun epochToHHmm(seconds: Long): String {
        return Instant.ofEpochSecond(seconds)
            .atZone(malaysiaZone)
            .format(formatter)
    }

    private fun getLocationAndFetchPrayerTimes() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                textView.text = "Failed to get location."
                return@addOnSuccessListener
            }

            val lat = location.latitude
            val lon = location.longitude

            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val locationName = if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val city = addr.locality ?: addr.subAdminArea ?: "Unknown"
                val state = addr.adminArea ?: "Malaysia"
                "$city, $state"
            } else "Unknown location"

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.getPrayerTimesByGps(lat, lon)
                    val today = response.prayers.firstOrNull { it.day == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
                    today?.let {
                        val prayerTimes = mapOf(
                            "Fajr" to it.fajr.toString(),
                            "Zohor" to it.dhuhr.toString(),
                            "Asar" to it.asr.toString(),
                            "Maghrib" to it.maghrib.toString(),
                            "Isyak" to it.isha.toString(),
                            "locationName" to locationName
                        )
                        Log.d("MainActivity", it.toString())
                        prayerStore.savePrayerTimes(prayerTimes)

                        textView.text = """
                            Location:
                            $locationName
                            
                            Fajr: ${epochToHHmm(prayerTimes["Fajr"]!!.toLong())}
                            Zohor: ${epochToHHmm(prayerTimes["Zohor"]!!.toLong())}
                            Asar: ${epochToHHmm(prayerTimes["Asar"]!!.toLong())}
                            Maghrib: ${epochToHHmm(prayerTimes["Maghrib"]!!.toLong())}
                            Isyak: ${epochToHHmm(prayerTimes["Isyak"]!!.toLong())}
                        """.trimIndent()
                    }
                } catch (e: Exception) {
                    textView.text = "API Error: ${e.message}"
                }
            }
        }
    }
}