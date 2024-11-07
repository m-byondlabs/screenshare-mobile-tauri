package com.plugin.bl.mobile.screen.capture.com.plugin.bl.mobile.screen.capture

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import app.tauri.plugin.Channel
import app.tauri.plugin.JSObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream

class ScreenCaptureManager private constructor(
    context: Context,
    private val isServiceBased: Boolean
) {
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isScreenCaptureRunning = AtomicBoolean(false)
    private val coroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var frameRate = 330
    private var lastFrameTime = System.currentTimeMillis()
    private val contextRef = WeakReference(context.applicationContext)

    // Channel for communication
    private var captureChannel: Channel? = null

    companion object {
        private const val TAG = "ScreenCaptureManager"
        private const val VIRTUAL_DISPLAY_WIDTH = 720
        private const val VIRTUAL_DISPLAY_HEIGHT = 1080
        const val SERVICE_ID = 1667

        @Volatile
        private var INSTANCE: ScreenCaptureManager? = null

        fun createForService(context: Context): ScreenCaptureManager {
            return ScreenCaptureManager(context.applicationContext, true)
        }
    }

    fun startCapture(resultCode: Int, data: Intent?) {
        // Direct start without broadcast when running in service
        if (isServiceBased) {
            setupMediaProjection(resultCode, data)
        }
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent?) {
        contextRef.get()?.let { ctx ->
            mediaProjectionManager =
                ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = data?.let {
                mediaProjectionManager?.getMediaProjection(resultCode, it)
                    ?.apply {
                        registerCallback(projectionCallback, handler)
                    }
            }
            mediaProjection?.let {
                startScreenCapture()
            }
        }
    }


    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d(TAG, "MediaProjection stopped")
            cleanup()
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            /*captureChannel?.send(JSObject().apply {
                put("captured_content_visibility", isVisible)
            })*/
        }
    }

    fun setChannel(channel: Channel) {
        captureChannel = channel
    }

    fun updateFPS(fps: Int) {
        frameRate = 1000 / fps
    }

    private fun startScreenCapture() {
        isScreenCaptureRunning.set(true)

        imageReader = ImageReader.newInstance(
            VIRTUAL_DISPLAY_WIDTH,
            VIRTUAL_DISPLAY_HEIGHT,
            PixelFormat.RGBA_8888,
            2
        ).apply {
            setOnImageAvailableListener({ reader ->
                if (!isScreenCaptureRunning.get()) return@setOnImageAvailableListener

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFrameTime < frameRate) {
                    reader.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }
                lastFrameTime = currentTime

                try {
                    reader.acquireLatestImage()?.use { image ->
                        processImage(image)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing screen capture", e)
                }
            }, handler)
        }

        contextRef.get()?.let { ctx ->
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                VIRTUAL_DISPLAY_WIDTH,
                VIRTUAL_DISPLAY_HEIGHT,
                ctx.resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )
        }
    }

    private fun processImage(image: android.media.Image) {
        val width = image.width
        val height = image.height
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val rowPadding = rowStride - width * 4

        val bytes = ByteArray(width * height * 4)
        var position = 0

        if (rowPadding == 0) {
            buffer.get(bytes)
        } else {
            val rowBytes = ByteArray(rowStride)
            for (row in 0 until height) {
                buffer.get(rowBytes)
                System.arraycopy(rowBytes, 0, bytes, position, width * 4)
                position += width * 4
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            sendFrame(bytes, width, height)
        }
    }

    private fun sendFrame(frameData: ByteArray, width: Int, height: Int) {
        val compressedFrame = compressFrame(frameData)
        captureChannel?.send(JSObject().apply {
            put("frame", Base64.encodeToString(compressedFrame, Base64.NO_WRAP))
            put("width", width)
            put("height", height)
        })
    }

    private fun compressFrame(frameData: ByteArray): ByteArray {
        return ByteArrayOutputStream().use { byteStream ->
            GZIPOutputStream(byteStream).use { gzipStream ->
                gzipStream.write(frameData)
            }
            byteStream.toByteArray()
        }
    }

    fun stopCapture() {
        cleanup()
    }

    fun cleanup() {
        isScreenCaptureRunning.set(false)
        lastFrameTime = 0L

        handler.removeCallbacksAndMessages(null)

        imageReader?.acquireLatestImage()?.close()
        imageReader?.close()
        imageReader = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
        captureChannel = null
    }

}