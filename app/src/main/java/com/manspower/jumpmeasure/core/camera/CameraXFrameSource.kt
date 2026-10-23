package com.manspower.jumpmeasure.core.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class CameraXFrameSource @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraFrameSource {

    private val _frames = MutableSharedFlow<CameraFrame>(extraBufferCapacity = 2)
    override val frames: Flow<CameraFrame> = _frames

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var lifecycleOwner: LifecycleOwner? = null

    // Call this before start if you want to bind to a specific lifecycle
    fun bindLifecycle(owner: LifecycleOwner) {
        this.lifecycleOwner = owner
    }

    override suspend fun start(configuration: CameraConfiguration) {
        val provider = getCameraProvider(context)
        cameraProvider = provider
        
        val owner = configuration.lifecycleOwner ?: (context as LifecycleOwner)

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    configuration.targetResolution,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(imageProxy)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(configuration.lensFacing)
            .build()

        var preview: Preview? = null
        if (configuration.surfaceProvider != null) {
            preview = Preview.Builder().build()
            preview.setSurfaceProvider(configuration.surfaceProvider)
        }

        try {
            provider.unbindAll()
            
            if (preview != null) {
                provider.bindToLifecycle(
                    owner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } else {
                provider.bindToLifecycle(
                    owner,
                    cameraSelector,
                    imageAnalysis
                )
            }
            Log.d("CameraXFrameSource", "Camera started successfully")
        } catch (exc: Exception) {
            Log.e("CameraXFrameSource", "Use case binding failed", exc)
        }
    }

    override suspend fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
        Log.d("CameraXFrameSource", "Camera stopped")
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val frame = CameraFrame(
            timestampNs = imageProxy.imageInfo.timestamp,
            width = imageProxy.width,
            height = imageProxy.height,
            rotationDegrees = imageProxy.imageInfo.rotationDegrees,
            cropRect = FrameRect(
                left = imageProxy.cropRect.left,
                top = imageProxy.cropRect.top,
                right = imageProxy.cropRect.right,
                bottom = imageProxy.cropRect.bottom
            ),
            image = imageProxy
        )
        
        val emitted = _frames.tryEmit(frame)
        if (!emitted) {
            imageProxy.close() // Close if flow couldn't take it
        }
    }

    private suspend fun getCameraProvider(context: Context): ProcessCameraProvider = suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                continuation.resume(cameraProviderFuture.get())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
