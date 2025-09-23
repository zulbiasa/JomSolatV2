package com.zulbiasa.jomsolatv2.watchface

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
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

    private var stepCount = 0
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    stepCount = it.values[0].toInt()
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {

        // Initialize sensor manager for step counter
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let {
            sensorManager?.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_UI)
        }

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

        // New paints for battery and steps
        val batteryPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            textSize = 24f
        }
        val batteryIconPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val batteryFillPaint = Paint().apply {
            color = Color.GREEN
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val stepsPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            textSize = 24f
        }
        val stepsIconPaint = Paint().apply {
            color = Color.parseColor("#FF9800") // Orange color for steps
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            textSize = 20f
        }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
        val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")

        // --- SharedAssets ---
        class PrayerSharedAssets : Renderer.SharedAssets {
            var prayerTimes: Map<String, Long> = mapOf()
            var locationName: String = "Unknown"

            override fun onDestroy() {}

            suspend fun loadFromDataStore() {
                try {
                    val stored = prayerStore.readPrayerTimes().first()
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

                val currentPrayer = prayerMinutes.lastOrNull { it.second <= nowMinutes }?.first ?: order.last()
                val nextPrayer = prayerMinutes.firstOrNull { it.second > nowMinutes }?.first ?: order.first()

                val nextTime = Instant.ofEpochSecond(prayers[nextPrayer] ?: 0L)
                    .atZone(malaysiaZone)
                    .format(timeFormatter)

                return Triple(currentPrayer, nextPrayer, nextTime)
            }

            private fun getBatteryLevel(): Int {
                val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            }

            private fun drawBatteryIcon(canvas: Canvas, x: Float, y: Float, level: Int) {
                // Draw battery outline
                val batteryRect = RectF(x, y - 15, x + 40, y + 5)
                canvas.drawRoundRect(batteryRect, 2f, 2f, batteryIconPaint)

                // Draw battery terminal
                canvas.drawRect(x + 40, y - 8, x + 43, y - 2, batteryIconPaint)

                // Draw battery fill based on level
                val fillWidth = (36 * level / 100).toFloat()
                if (fillWidth > 0) {
                    val fillColor = when {
                        level <= 20 -> Color.RED
                        level <= 50 -> Color.YELLOW
                        else -> Color.GREEN
                    }
                    batteryFillPaint.color = fillColor
                    canvas.drawRect(x + 2, y - 13, x + 2 + fillWidth, y + 3, batteryFillPaint)
                }

                // Draw percentage text
                batteryPaint.textAlign = Paint.Align.LEFT
                batteryPaint.textSize = 20f
                batteryPaint.isFakeBoldText = true
                canvas.drawText("$level%", x + 5, y + 32, batteryPaint)
            }

            private fun drawStepsIcon(canvas: Canvas, x: Float, y: Float) {
                /*// Draw simple footstep icon using path
                val path = Path()
                val iconX = x - 100

                // Simple footstep shape
                path.moveTo(iconX, y - 10)
                path.lineTo(iconX + 5, y - 5)
                path.lineTo(iconX + 5, y + 5)
                path.lineTo(iconX - 5, y + 5)
                path.lineTo(iconX - 5, y - 5)
                path.close()

                val iconPaint = Paint().apply {
                    color = Color.parseColor("#FF9800")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawPath(path, iconPaint)*/

                // Draw step count
                val formattedSteps = when {
                    stepCount >= 10000 -> String.format("%.1fk", stepCount / 1000.0)
                    stepCount >= 1000 -> String.format("%dk", stepCount / 1000)
                    else -> stepCount.toString()
                }
                stepsPaint.textAlign = Paint.Align.RIGHT
                stepsPaint.textSize = 30f
                canvas.drawText(formattedSteps, x - 20, y - 10, stepsPaint)

                // Draw "steps" label
                stepsIconPaint.textAlign = Paint.Align.RIGHT
                stepsIconPaint.textSize = 20f
                canvas.drawText("steps", x - 20, y + 10, stepsIconPaint)
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

                // Battery indicator on the left
                val batteryLevel = getBatteryLevel()
                drawBatteryIcon(canvas, 20f, cy - 55f, batteryLevel)

                // Steps indicator on the right
                drawStepsIcon(canvas, bounds.width() - 20f, cy - 40f)

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
                sensorManager?.unregisterListener(stepListener)
            }
        }

        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(stepListener)
    }
}