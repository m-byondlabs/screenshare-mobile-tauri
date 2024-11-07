package com.plugin.bl.mobile.screen.capture

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.core.content.ContextCompat
import app.tauri.annotation.ActivityCallback
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Channel
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import com.plugin.bl.mobile.screen.capture.com.plugin.bl.mobile.screen.capture.FGLauncherActivity

@InvokeArg
class StartScreenCaptureArgs {
    lateinit var channel: Channel
}

@InvokeArg
class UpdateFPSArgs {
    var fps: Int = 15
}

@TauriPlugin
class ScreenCapturePlugin(private val activity: Activity) : Plugin(activity),
    ActivityLifecycleCallbacks {
    private var pendingInvoke: Invoke? = null
    private val TAG = "ScreenCapturePlugin"


    init {
        Log.d(
            "pulkit",
            "ScreenCapturePlugin initialized with activity $activity and plugin $this "
        )
    }

    @ActivityCallback
    private fun handleScreenCastRequestResult(
        invoke: Invoke,
        result: ActivityResult
    ) {
        handleActivityResult(result.resultCode, result.data)
        invoke.resolve()
    }

    @Command
    fun updateFPS(invoke: Invoke) {
        val fps = invoke.parseArgs(UpdateFPSArgs::class.java).fps
        Log.d(TAG, "Updating fps to $fps")
        //screenCaptureManager.updateFPS(fps) TODO implement this

        val ret = JSObject()
        ret.put("success", true)
        invoke.resolve(ret)
    }

    @Command
    fun startCapture(invoke: Invoke) {
        Log.d(TAG, "start capture command called")
        val intent = Intent(activity, FGLauncherActivity::class.java)
        activity.startActivity(intent)
        invoke.resolve()
    }

    @Command
    fun stopCapture(invoke: Invoke) {
        Log.d(TAG, "Stopping screen capture")
        val serviceIntent =
            Intent(activity.applicationContext, MyMediaProjectionService::class.java).apply {
                action = MyMediaProjectionService.ACTION_STOP_FOREGROUND_SERVICE
            }
        activity.stopService(serviceIntent)

        val ret = JSObject()
        ret.put("success", true)
        invoke.resolve(ret)
    }

    private fun handleActivityResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "Screen capture permission result: $resultCode")
        if (resultCode == Activity.RESULT_OK) {
            val serviceIntent =
                Intent(
                    activity.applicationContext,
                    MyMediaProjectionService::class.java
                ).apply {
                    action = MyMediaProjectionService.ACTION_START_CAPTURE
                    putExtra(
                        MyMediaProjectionService.EXTRA_RESULT_CODE,
                        resultCode
                    )
                    putExtra(MyMediaProjectionService.EXTRA_RESULT_DATA, data)
                }
            try {
                ContextCompat.startForegroundService(
                    activity.applicationContext,
                    serviceIntent
                )
                pendingInvoke?.resolve()
                pendingInvoke = null
            } catch (e: RuntimeException) {
                Log.w(
                    TAG,
                    "Error while trying to get the MediaProjection instance: ${e.message}"
                )
                pendingInvoke?.reject("Failed to start screen capture: ${e.message}")
                pendingInvoke = null
            }

        } else {
            Log.d(TAG, "Screen capture permission denied")
            pendingInvoke?.reject("Screen capture permission denied")
            pendingInvoke = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        Log.d(TAG, "Activity created")
    }

    override fun onActivityStarted(p0: Activity) {
        Log.d(TAG, "Activity started")
    }

    override fun onActivityResumed(p0: Activity) {
        Log.d(TAG, "Activity resumed")
    }

    override fun onActivityPaused(p0: Activity) {
        Log.d(TAG, "Activity paused")
    }

    override fun onActivityStopped(p0: Activity) {
        Log.d(TAG, "Activity stopped")
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        Log.d(TAG, "Activity save instance state")
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        Log.d(TAG, "Activity pre destroyed")
        super.onActivityPreDestroyed(activity)
        pendingInvoke = null
    }

    override fun onActivityDestroyed(p0: Activity) {
        Log.d(TAG, "Activity destroyed")
    }
}