package com.example.water_level_data_fetch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var waterLevelIndicatorView: WaterLevelIndicatorView
    private lateinit var label: TextView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var dataFetchRunnable: Runnable

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val apiService: WaterLevelApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.31.164:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WaterLevelApiService::class.java)
    }

    private companion object {
        private const val TANK_HEIGHT_METERS = 2.0
        private const val UPDATE_INTERVAL_MS = 5000L
        private const val NUMBER_OF_BARS = 10
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "OverlayServiceChannel"
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d("OverlayService", "onBind")
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("OverlayService", "onCreate: Service creating.")
        try {
            createNotificationChannel()
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)

            Log.d("OverlayService", "Service started in foreground.")

            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            Log.d("OverlayService", "WindowManager obtained.")

            // --- UI Setup ---
            val rootContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }

            waterLevelIndicatorView = WaterLevelIndicatorView(this, NUMBER_OF_BARS)

            label = TextView(this).apply {
                text = "Water Level: --%"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(10, 5, 10, 5)
            }

            rootContainer.addView(label)
            rootContainer.addView(waterLevelIndicatorView)

            overlayView = rootContainer
            Log.d("OverlayService", "Overlay view created.")

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.END
            params.y = 10
            params.x = 10
            Log.d("OverlayService", "WindowManager.LayoutParams created.")

            windowManager.addView(overlayView, params)
            Log.d("OverlayService", "View added to WindowManager.")

            setupDataFetching()

        } catch (e: Exception) {
            Log.e("OverlayService", "Fatal Error in onCreate", e)
            stopSelf()
        }
    }

    private fun setupDataFetching() {
        dataFetchRunnable = Runnable {
            fetchWaterLevel()
            handler.postDelayed(dataFetchRunnable, UPDATE_INTERVAL_MS)
        }
        handler.post(dataFetchRunnable)
    }

    private fun fetchWaterLevel() {
        serviceScope.launch {
            try {
                val waterLevelData = apiService.getWaterLevelData()
                Log.d("OverlayService", "Fetched data: $waterLevelData")
                updateIndicator(waterLevelData.height_m)
            } catch (e: Exception) {
                Log.e("OverlayService", "Failed to fetch water level", e)
            }
        }
    }

    private fun updateIndicator(waterHeight: Double) {
        val fillPercentage = (waterHeight / TANK_HEIGHT_METERS).coerceIn(0.0, 1.0)
        val level = (fillPercentage * NUMBER_OF_BARS).roundToInt().coerceIn(0, NUMBER_OF_BARS)
        val percentageString = "%.0f".format(fillPercentage * 100)
        Log.d("OverlayService", "Calculated level: $level, Percentage: $percentageString%")

        handler.post {
            label.text = "Water Level: $percentageString%"
            waterLevelIndicatorView.updateWaterLevel(level)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Overlay Service Channel", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            Log.d("OverlayService", "Notification channel created.")
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Water Level Service")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(dataFetchRunnable)
        serviceJob.cancel()
        Log.d("OverlayService", "onDestroy: Service is being destroyed.")
        try {
            if (::overlayView.isInitialized) {
                windowManager.removeView(overlayView)
                Log.d("OverlayService", "Overlay view removed from WindowManager.")
            }
        } catch (e: Exception) {
            Log.e("OverlayService", "Error in onDestroy while removing view", e)
        }
    }
}

private class WaterLevelIndicatorView(context: Context, private val bars: Int) : View(context) {
    private val paint = Paint()

    // --- CUSTOMIZATION: Adjust the width and height of the bars here ---
    private val barWidth = 30f // The width of each bar in pixels
    private val barHeight = 15f // The height of each bar in pixels

    private val barSpacing = 5f
    private val totalHeight = (bars * barHeight) + ((bars - 1) * barSpacing)
    private var waterLevel = 0 // Level from 0 to `bars`

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun updateWaterLevel(level: Int) {
        if (level in 0..bars) {
            waterLevel = level
            invalidate() // Request a redraw
        }    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(barWidth.toInt(), totalHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars <= 0) return
        val startBar = bars - waterLevel
        for (i in startBar until bars) {
            val top = i * (barHeight + barSpacing)
            val rect = RectF(0f, top, barWidth, top + barHeight)

            val fraction = if (bars > 1) i / (bars - 1).toFloat() else 1f
            val red = (255 * fraction).toInt()
            val green = (255 * (1 - fraction)).toInt()
            paint.color = Color.rgb(red, green, 0)

            canvas.drawRect(rect, paint)
        }
    }
}