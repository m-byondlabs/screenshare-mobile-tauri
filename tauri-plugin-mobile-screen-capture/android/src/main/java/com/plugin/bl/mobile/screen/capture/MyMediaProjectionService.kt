package com.plugin.bl.mobile.screen.capture

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.plugin.bl.mobile.screen.capture.com.plugin.bl.mobile.screen.capture.ScreenCaptureManager
import java.lang.ref.WeakReference

class MyMediaProjectionService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private var screenCaptureManager: ScreenCaptureManager? = null
    private val binder = ScreenCaptureBinder()

    inner class ScreenCaptureBinder : Binder() {
        private val serviceRef = WeakReference(this@MyMediaProjectionService)

        fun getService(): MyMediaProjectionService? = serviceRef.get()
        fun getManager(): ScreenCaptureManager? = serviceRef.get()?.screenCaptureManager
    }

    companion object {
        private const val TAG = "MediaProjectionService"
        const val ACTION_STOP_FOREGROUND_SERVICE =
            "ACTION_STOP_FOREGROUND_SERVICE"
        const val ACTION_START_CAPTURE = "ACTION_START_CAPTURE"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MediaProjectionService onCreate")
        initializeService()
    }

    private fun initializeService() {
        initializeWakeLock()
        initializeNotification()
        screenCaptureManager =
            ScreenCaptureManager.createForService(applicationContext)
    }

    private fun initializeWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ExampleApp:WakeLockTag"
        ).apply {
            if (!isHeld) {
                acquire(60 * 60 * 1000L) // 60 minutes timeout
            }
        }
    }

    private fun initializeNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val channelId = "MediaProjectionService"
            val channel = android.app.NotificationChannel(
                channelId,
                "Screen Capture Service",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)

            val notification = android.app.Notification.Builder(this, channelId)
                .setContentTitle("Screen Capture Active")
                .setContentText("Recording screen content")
                .build()

            startForeground(ScreenCaptureManager.SERVICE_ID, notification)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.d(TAG, "onStartCommand action: ${intent?.action}")

        when (intent?.action) {
            ACTION_STOP_FOREGROUND_SERVICE -> stopService()
            ACTION_START_CAPTURE -> handleCaptureStart(intent)
        }

        return START_NOT_STICKY
    }

    private fun handleCaptureStart(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val data: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)

        screenCaptureManager?.startCapture(resultCode, data)
    }

    private fun stopService() {
        screenCaptureManager?.stopCapture()
        releaseWakeLock()
        stopForeground(true)
        stopSelf()
    }

    private fun releaseWakeLock() {
        try {
            if (::wakeLock.isInitialized && wakeLock.isHeld) {
                wakeLock.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wakelock", e)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }


    override fun onDestroy() {
        super.onDestroy()
        screenCaptureManager?.cleanup()
        screenCaptureManager = null
    }
}