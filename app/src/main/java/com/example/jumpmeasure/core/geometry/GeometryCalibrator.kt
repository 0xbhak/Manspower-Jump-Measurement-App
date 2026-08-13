package com.example.jumpmeasure.core.geometry

import android.content.Context
import com.example.jumpmeasure.domain.model.MeasurementError
import com.example.jumpmeasure.domain.model.Meters

sealed interface CalibrationResult {
    data class Success(val transformer: CoordinateTransformer) : CalibrationResult
    data class Error(val reason: MeasurementError) : CalibrationResult
}

/**
 * Bertanggung jawab membuat CoordinateTransformer (objek final untuk kalibrasi).
 * Input menggunakan kombinasi metode Pinhole (menggunakan jarak + FOV) dengan 
 * opsi double-check dari Tinggi Tubuh referensi.
 */
class GeometryCalibrator(
    private val context: Context,
    private val fovExtractor: CameraFovExtractor = DefaultCameraFovExtractor
) {

    /**
     * Membentuk transformer matriks baru.
     * 
     * @param cameraDistanceMeters Jarak horizontal lensa ke bidang gerak atlet.
     * @param athleteHeightMeters Tinggi sebenarnya atlet (sebagai _sanity check_).
     * @param frameWidthPx Lebar dari *ImageProxy* (misal 1280).
     * @param frameHeightPx Tinggi dari *ImageProxy* (misal 720).
     * @param measuredHeightInPixels Tinggi atlet yang terpindai di sensor piksel saat pose kalibrasi.
     */
    fun calibrate(
        cameraDistanceMeters: Meters,
        athleteHeightMeters: Meters,
        frameWidthPx: Int,
        frameHeightPx: Int,
        measuredHeightInPixels: Double? = null
    ): CalibrationResult {
        if (cameraDistanceMeters.value <= 0.0 || athleteHeightMeters.value <= 0.0) {
            return CalibrationResult.Error(MeasurementError.CalibrationInvalid)
        }

        val fov = fovExtractor.extractFov(context)
        
        val transformer = PinholeCameraTransformer(
            frameWidthPixels = frameWidthPx,
            frameHeightPixels = frameHeightPx,
            cameraDistanceMeters = cameraDistanceMeters.value,
            fovHorizontalDegrees = fov.horizontalDegrees,
            fovVerticalDegrees = fov.verticalDegrees
        )
        
        // [FIX v3] Sanity check dipertahankan HANYA jika measuredHeightInPixels disediakan.
        // Toleransi diperlebar dari 40cm → 60cm karena postur atlet bervariasi dan
        // kalibrasi cukup presisi untuk keperluan olahraga (bukan medis).
        if (measuredHeightInPixels != null && measuredHeightInPixels > 0.0) {
            val estimatedRealHeight = transformer.transformDistance(measuredHeightInPixels)
            val diff = Math.abs(estimatedRealHeight - athleteHeightMeters.value)
            
            if (diff > 0.60) {
                return CalibrationResult.Error(MeasurementError.CalibrationInvalid)
            }
        }

        return CalibrationResult.Success(transformer)
    }
}

