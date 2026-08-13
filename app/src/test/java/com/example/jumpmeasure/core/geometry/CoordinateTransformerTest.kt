package com.example.jumpmeasure.core.geometry

import com.example.jumpmeasure.domain.model.ImagePoint
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class CoordinateTransformerTest {

    @Test
    fun testPinholeCameraProjection() {
        // Setup kamera bayangan
        // FOV Horizontal = 90 derajat -> tan(45) = 1.0
        // FOV Vertikal = 90 derajat -> tan(45) = 1.0
        // Jarak kamera = 2.0 meter
        // Berarti W_real = 2 * 2.0 * 1.0 = 4.0 meter
        // Resolusi gambar = 1000 x 1000 piksel
        // Maka 1000 piksel = 4.0 meter -> 1 piksel = 0.004 meter
        
        val transformer = PinholeCameraTransformer(
            frameWidthPixels = 1000,
            frameHeightPixels = 1000,
            cameraDistanceMeters = 2.0,
            fovHorizontalDegrees = 90.0,
            fovVerticalDegrees = 90.0
        )

        // Verifikasi MetersPerPixel
        assertEquals(0.004, transformer.metersPerPixelX, 0.0001)
        assertEquals(0.004, transformer.metersPerPixelY, 0.0001)

        // Verifikasi Transform Point
        val imagePoint = ImagePoint(500.0, 500.0) // Di tengah gambar
        val worldPoint = transformer.transform(imagePoint)
        
        assertEquals(2.0, worldPoint.xMeters, 0.0001) // 500 * 0.004 = 2.0m
        assertEquals(2.0, worldPoint.yMeters, 0.0001)

        // Verifikasi Transform Distance
        val distMeters = transformer.transformDistance(250.0) // 1/4 layar
        assertEquals(1.0, distMeters, 0.0001) // 250 * 0.004 = 1.0m
    }
}
