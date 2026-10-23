package com.manspower.jumpmeasure.core.geometry

import android.content.Context
import com.manspower.jumpmeasure.domain.model.MeasurementError
import com.manspower.jumpmeasure.domain.model.Meters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class GeometryCalibratorTest {

    @Test
    fun testSuccessfulCalibration() {
        val mockContext = mock<Context>()
        val mockFovExtractor = object : CameraFovExtractor {
            override fun extractFov(context: Context, targetLensFacing: Int): CameraFov {
                return CameraFov(90.0, 90.0) // 1000px = 4m -> 1px = 0.004m
            }
        }

        val calibrator = GeometryCalibrator(mockContext, mockFovExtractor)
        
        val result = calibrator.calibrate(
            cameraDistanceMeters = Meters(2.0),
            athleteHeightMeters = Meters(1.8),
            frameWidthPx = 1000,
            frameHeightPx = 1000,
            measuredHeightInPixels = null // tanpa sanity check
        )

        assertTrue(result is CalibrationResult.Success)
        val success = result as CalibrationResult.Success
        
        assertEquals(0.004, success.transformer.metersPerPixelY, 0.0001)
    }
    
    @Test
    fun testFailedCalibrationSanityCheck() {
        val mockContext = mock<Context>()
        val mockFovExtractor = object : CameraFovExtractor {
            override fun extractFov(context: Context, targetLensFacing: Int): CameraFov {
                return CameraFov(90.0, 90.0) 
            }
        }

        val calibrator = GeometryCalibrator(mockContext, mockFovExtractor)
        
        // Memaksa Sanity Check gagal
        val result = calibrator.calibrate(
            cameraDistanceMeters = Meters(2.0),
            athleteHeightMeters = Meters(1.8),
            frameWidthPx = 1000,
            frameHeightPx = 1000,
            measuredHeightInPixels = 800.0 // 800px * 0.004m = 3.2m! (Jauh dari 1.8m)
        )

        assertTrue(result is CalibrationResult.Error)
        val error = result as CalibrationResult.Error
        assertEquals(MeasurementError.CalibrationInvalid, error.reason)
    }
}
