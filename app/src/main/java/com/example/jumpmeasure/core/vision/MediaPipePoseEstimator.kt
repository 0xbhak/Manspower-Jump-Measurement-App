package com.example.jumpmeasure.core.vision

import android.content.Context
import android.graphics.Matrix
import android.os.SystemClock
import com.example.jumpmeasure.core.camera.CameraFrame
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPipePoseEstimator @Inject constructor(
    @ApplicationContext private val context: Context
) : PoseEstimator {

    private var poseLandmarker: PoseLandmarker? = null

    override suspend fun initialize(configuration: PoseEstimatorConfiguration) {
        withContext(Dispatchers.IO) {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(configuration.modelAssetPath)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinPoseDetectionConfidence(configuration.minPoseDetectionConfidence)
                .setMinPosePresenceConfidence(configuration.minPosePresenceConfidence)
                .setMinTrackingConfidence(configuration.minTrackingConfidence)
                .setRunningMode(RunningMode.VIDEO) // We use VIDEO mode for sequential frames
                .build()

            try {
                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            } catch (e: Throwable) {
                e.printStackTrace()
                // Provide fallback/null behavior for emulators without x86_64 libraries
                poseLandmarker = null
            }
        }
    }

    override suspend fun estimate(frame: CameraFrame): PoseFrameResult = withContext(Dispatchers.Default) {
        val landmarker = poseLandmarker ?: return@withContext emptyResult(frame, PoseFrameStatus.ERROR)

        val imageProxy = frame.image
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            return@withContext emptyResult(frame, PoseFrameStatus.ERROR)
        }

        val startTime = SystemClock.uptimeMillis()

        return@withContext try {
            val mpImage = MediaImageBuilder(mediaImage)
                .build()
            
            val imageProcessingOptions = ImageProcessingOptions.builder()
                .setRotationDegrees(frame.rotationDegrees)
                .build()
            
            val result = landmarker.detectForVideo(mpImage, imageProcessingOptions, frame.timestampNs / 1000000)
            
            val inferenceTimeMs = (SystemClock.uptimeMillis() - startTime).toDouble()
            
            mapResult(result, frame, inferenceTimeMs)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyResult(frame, PoseFrameStatus.ERROR)
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            poseLandmarker?.close()
            poseLandmarker = null
        }
    }

    private fun emptyResult(frame: CameraFrame, status: PoseFrameStatus): PoseFrameResult {
        val isRotated = frame.rotationDegrees == 90 || frame.rotationDegrees == 270
        val logicalWidth = if (isRotated) frame.height else frame.width
        val logicalHeight = if (isRotated) frame.width else frame.height

        return PoseFrameResult(
            timestampNs = frame.timestampNs,
            landmarks = emptyMap(),
            poseConfidence = 0.0,
            inferenceDurationMs = 0.0,
            segmentationMask = null,
            status = status,
            imageWidth = logicalWidth,
            imageHeight = logicalHeight
        )
    }

    private fun mapResult(result: PoseLandmarkerResult, frame: CameraFrame, inferenceTimeMs: Double): PoseFrameResult {
        if (result.landmarks().isEmpty()) {
            return emptyResult(frame, PoseFrameStatus.NO_POSE_DETECTED)
        }

        // We only take the first person detected
        val landmarks = result.landmarks()[0]
        val mappedLandmarks = mutableMapOf<BodyLandmark, PoseLandmarkSample>()

        // MediaPipe indices mapping
        val indexMap = mapOf(
            27 to BodyLandmark.LEFT_ANKLE,
            28 to BodyLandmark.RIGHT_ANKLE,
            29 to BodyLandmark.LEFT_HEEL,
            30 to BodyLandmark.RIGHT_HEEL,
            31 to BodyLandmark.LEFT_FOOT_INDEX,
            32 to BodyLandmark.RIGHT_FOOT_INDEX,
            25 to BodyLandmark.LEFT_KNEE,
            26 to BodyLandmark.RIGHT_KNEE,
            23 to BodyLandmark.LEFT_HIP,
            24 to BodyLandmark.RIGHT_HIP,
            11 to BodyLandmark.LEFT_SHOULDER,
            12 to BodyLandmark.RIGHT_SHOULDER
        )

        val isRotated = frame.rotationDegrees == 90 || frame.rotationDegrees == 270
        val logicalWidth = if (isRotated) frame.height else frame.width
        val logicalHeight = if (isRotated) frame.width else frame.height

        for ((index, bodyLandmark) in indexMap) {
            if (index < landmarks.size) {
                val lm = landmarks[index]
                
                var nx = lm.x().toDouble()
                var ny = lm.y().toDouble()
                
                // MediaPipe Tasks returns landmarks in the original unrotated image coordinates.
                // We need to rotate them to match the logical display orientation.
                when (frame.rotationDegrees) {
                    90 -> {
                        val temp = nx
                        nx = 1.0 - ny
                        ny = temp
                    }
                    180 -> {
                        nx = 1.0 - nx
                        ny = 1.0 - ny
                    }
                    270 -> {
                        val temp = nx
                        nx = ny
                        ny = 1.0 - temp
                    }
                }

                mappedLandmarks[bodyLandmark] = PoseLandmarkSample(
                    type = bodyLandmark,
                    imageX = nx * logicalWidth,
                    imageY = ny * logicalHeight,
                    normalizedX = nx,
                    normalizedY = ny,
                    relativeZ = lm.z().toDouble(),
                    visibility = lm.visibility().orElse(0f).toDouble(),
                    presence = lm.presence().orElse(0f).toDouble()
                )
            }
        }

        return PoseFrameResult(
            timestampNs = frame.timestampNs,
            landmarks = mappedLandmarks,
            poseConfidence = 1.0, // MediaPipe doesn't give overall confidence in output directly, we use 1.0 if found
            inferenceDurationMs = inferenceTimeMs,
            segmentationMask = null,
            status = PoseFrameStatus.SUCCESS,
            imageWidth = logicalWidth,
            imageHeight = logicalHeight
        )
    }
}
