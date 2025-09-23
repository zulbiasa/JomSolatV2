package com.zulbiasa.jomsolatv2.watchface

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.zulbiasa.jomsolatv2.data.PrayerTimesDataStore
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MyWatchFaceService : WatchFaceService() {

    override fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())
    private val prayerStore by lazy { PrayerTimesDataStore(applicationContext) }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {

        // --- Paints ---
        val timePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 86f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
        val currentPrayerPaint = Paint().apply {
            color = Color.LTGRAY
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 28f
        }
        val nextPrayerPaint = Paint().apply {
            color = Color.parseColor("#4DD0E1")
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 30f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
        val locationPaint = Paint().apply {
            color = Color.YELLOW
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 20f
        }
        val datePaint = Paint().apply {
            color = Color.GREEN
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 24f
        }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
        val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")

        // --- SharedAssets ---
        class PrayerSharedAssets : Renderer.SharedAssets {
            var prayerTimes: Map<String, Long> = mapOf() // simpan epoch time
            var locationName: String = "Unknown"

            override fun onDestroy() {}

            suspend fun loadFromDataStore() {
                try {
                    val stored = prayerStore.readPrayerTimes().first()
                    // convert string epoch ke Long
                    prayerTimes = stored.mapNotNull {
                        val value = it.value.toLongOrNull()
                        if (it.key != "location" && value != null) it.key to value else null
                    }.toMap()
                    locationName = stored["location"] ?: "Unknown"
                } catch (e: Exception) {
                    Log.e("MyWatchFace", "Error loading prayer times: ${e.message}")
                    prayerTimes = mapOf(
                        "Fajr" to 0L,
                        "Zohor" to 0L,
                        "Asar" to 0L,
                        "Maghrib" to 0L,
                        "Isyak" to 0L
                    )
                    locationName = "Unknown"
                }
            }
        }

        val renderer = object : Renderer.CanvasRenderer2<PrayerSharedAssets>(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            CanvasType.HARDWARE,
            interactiveDrawModeUpdateDelayMillis = 1000L,
            clearWithBackgroundTintBeforeRenderingHighlightLayer = false
        ) {
            override suspend fun createSharedAssets(): PrayerSharedAssets {
                val shared = PrayerSharedAssets()
                shared.loadFromDataStore()
                return shared
            }

            private fun computeCurrentAndNext(
                now: ZonedDateTime,
                prayers: Map<String, Long>
            ): Triple<String, String, String> {
                val order = listOf("Fajr", "Zohor", "Asar", "Maghrib", "Isyak")
                val prayerMinutes = order.map { name ->
                    val epoch = prayers[name] ?: 0L
                    val zdt = Instant.ofEpochSecond(epoch).atZone(malaysiaZone)
                    name to (zdt.hour * 60 + zdt.minute)
                }

                val nowMinutes = now.hour * 60 + now.minute

                // Current = solat terakhir yang dah berlalu
                val currentPrayer = prayerMinutes.lastOrNull { it.second <= nowMinutes }?.first ?: order.last()

                // Next = solat pertama yang akan datang
                val nextPrayer = prayerMinutes.firstOrNull { it.second > nowMinutes }?.first ?: order.first()

                val nextTime = Instant.ofEpochSecond(prayers[nextPrayer] ?: 0L)
                    .atZone(malaysiaZone)
                    .format(timeFormatter)

                return Triple(currentPrayer, nextPrayer, nextTime)
            }

            override fun render(
                canvas: Canvas,
                bounds: Rect,
                zonedDateTime: ZonedDateTime,
                sharedAssets: PrayerSharedAssets
            ) {
                canvas.drawColor(Color.BLACK)
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()

                val (currentPrayer, nextPrayer, nextTime) =
                    computeCurrentAndNext(zonedDateTime, sharedAssets.prayerTimes)

                Log.d("MyWatchFace", "Current time: ${zonedDateTime.format(timeFormatter)}")
                Log.d("MyWatchFace", "Current prayer: $currentPrayer")
                Log.d("MyWatchFace", "Next prayer: $nextPrayer at $nextTime")

                // Current prayer
                canvas.drawText(currentPrayer, cx, cy - 120f, currentPrayerPaint)

                // Time
                canvas.drawText(zonedDateTime.format(timeFormatter), cx, cy - 20f, timePaint)

                // Date
                canvas.drawText(zonedDateTime.format(dateFormatter), cx, cy + 20f, datePaint)

                // Location
                canvas.drawText(sharedAssets.locationName, cx, cy + 60f, locationPaint)

                // Next prayer
                canvas.drawText("$nextPrayer  $nextTime", cx, cy + 100f, nextPrayerPaint)
            }

            override fun renderHighlightLayer(
                canvas: Canvas,
                bounds: Rect,
                zonedDateTime: ZonedDateTime,
                sharedAssets: PrayerSharedAssets
            ) {}

            override fun onDestroy() {
                super.onDestroy()
            }
        }

        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }
}