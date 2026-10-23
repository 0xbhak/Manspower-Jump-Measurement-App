package com.manspower.jumpmeasure.core.vision

import com.manspower.jumpmeasure.core.camera.CameraFrame

enum class BodyLandmark {
    LEFT_ANKLE,
    RIGHT_ANKLE,
    LEFT_HEEL,
    RIGHT_HEEL,
    LEFT_FOOT_INDEX,
    RIGHT_FOOT_INDEX,
    LEFT_KNEE,
    RIGHT_KNEE,
    LEFT_HIP,
    RIGHT_HIP,
    LEFT_SHOULDER,
    RIGHT_SHOULDER
}

data class PoseLandmarkSample(
    val type: BodyLandmark,
    val imageX: Double,
    val imageY: Double,
    val normalizedX: Double,
    val normalizedY: Double,
    val relativeZ: Double?,
    val visibility: Double?,
    val presence: Double?
)

class SegmentationMask

enum class PoseFrameStatus {
    SUCCESS,
    NO_POSE_DETECTED,
    ERROR
}

data class PoseFrameResult(
    val timestampNs: Long,
    val landmarks: Map<BodyLandmark, PoseLandmarkSample>,
    val poseConfidence: Double,
    val inferenceDurationMs: Double,
    val segmentationMask: SegmentationMask?,
    val status: PoseFrameStatus,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)

data class PoseEstimatorConfiguration(
    // Diturunkan dari 0.5 → 0.4 agar MediaPipe tetap mendeteksi pose
    // saat atlet sedang bergerak cepat (lompatan), di mana confidence
    // cenderung lebih rendah dibanding pose diam.
    val minPoseDetectionConfidence: Float = 0.4f,
    val minPosePresenceConfidence: Float = 0.4f,
    // Tracking confidence sedikit lebih tinggi untuk stabilitas antar frame
    val minTrackingConfidence: Float = 0.45f,
    val modelAssetPath: String = "pose_landmarker_full.task"
)

interface PoseEstimator {
    suspend fun initialize(configuration: PoseEstimatorConfiguration)
    suspend fun estimate(frame: CameraFrame): PoseFrameResult
    suspend fun close()
}
