package com.manspower.jumpmeasure.core.camera

import kotlinx.coroutines.flow.Flow

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner

data class FrameRect(val left: Int, val top: Int, val right: Int, val bottom: Int)

typealias FrameImage = ImageProxy

data class CameraFrame(
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val cropRect: FrameRect,
    val image: FrameImage
)

data class CameraConfiguration(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val targetResolution: Size = Size(1280, 720),
    val surfaceProvider: Preview.SurfaceProvider? = null,
    val lifecycleOwner: LifecycleOwner? = null
)

interface CameraFrameSource {
    val frames: Flow<CameraFrame>
    suspend fun start(configuration: CameraConfiguration)
    suspend fun stop()
}
