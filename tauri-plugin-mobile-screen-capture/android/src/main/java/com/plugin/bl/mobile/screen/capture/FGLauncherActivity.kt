package com.plugin.bl.mobile.screen.capture.com.plugin.bl.mobile.screen.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.plugin.bl.mobile.screen.capture.MyMediaProjectionService

class FGLauncherActivity : AppCompatActivity() {
    private val SCREEN_CAPTURE_REQUEST_CODE = 1667
    private val TAG = "pulkit"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("pulkit", "ScreenSharePermissionActivity onCreate");
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val serviceIntent =
                    Intent(
                        applicationContext,
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
                        applicationContext,
                        serviceIntent
                    )

                } catch (e: RuntimeException) {
                    Log.w(
                        TAG,
                        "Error while trying to get the MediaProjection instance: ${e.message}"
                    )

                }

            } else {
                Log.d(TAG, "Screen capture permission denied")

            }
            finish()
        }
    }
}