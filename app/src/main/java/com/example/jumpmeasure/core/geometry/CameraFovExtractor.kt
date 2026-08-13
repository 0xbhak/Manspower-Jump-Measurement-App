package com.example.jumpmeasure.core.geometry

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlin.math.atan

data class CameraFov(val horizontalDegrees: Double, val verticalDegrees: Double)

interface CameraFovExtractor {
    fun extractFov(context: Context, targetLensFacing: Int = CameraCharacteristics.LENS_FACING_BACK): CameraFov
}

object DefaultCameraFovExtractor : CameraFovExtractor {
    /**
     * Mengekstrak Field of View (FOV) dari spesifikasi hardware lensa kamera Android.
     */
    override fun extractFov(context: Context, targetLensFacing: Int): CameraFov {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                if (facing == targetLensFacing) {
                    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    
                    if (focalLengths != null && focalLengths.isNotEmpty() && sensorSize != null) {
                        val focalLength = focalLengths[0] // Biasanya lensa default berada di indeks 0
                        
                        // FOV = 2 * arctan(sensor_dimension / (2 * focal_length))
                        val horizontalFovRad = 2.0 * atan((sensorSize.width / (2.0 * focalLength)).toDouble())
                        val verticalFovRad = 2.0 * atan((sensorSize.height / (2.0 * focalLength)).toDouble())
                        
                        return CameraFov(
                            horizontalDegrees = Math.toDegrees(horizontalFovRad),
                            verticalDegrees = Math.toDegrees(verticalFovRad)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CameraFovExtractor", "Gagal mengambil FOV Kamera", e)
        }
        
        // Fallback FOV jika Camera2 API tidak merespon spesifikasi hardware
        return CameraFov(horizontalDegrees = 65.0, verticalDegrees = 45.0)
    }
}
