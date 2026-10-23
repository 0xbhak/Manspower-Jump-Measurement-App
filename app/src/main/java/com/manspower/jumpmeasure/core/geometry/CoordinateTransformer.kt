package com.manspower.jumpmeasure.core.geometry

import com.manspower.jumpmeasure.domain.model.ImagePoint
import com.manspower.jumpmeasure.domain.model.WorldPoint
import kotlin.math.tan

/**
 * Mengubah koordinat piksel mentah ke dalam skala dunia nyata (Meters).
 */
interface CoordinateTransformer {
    fun transform(imagePoint: ImagePoint): WorldPoint
    /** Jarak vertikal (Y-axis) dalam meter */
    fun transformDistance(pixels: Double): Double
    /** Jarak horizontal (X-axis) dalam meter — dipakai untuk Lompat Jauh */
    fun transformHorizontalDistance(pixels: Double): Double
    val metersPerPixelX: Double
    val metersPerPixelY: Double
}

/**
 * Implementasi pinhole camera model sederhana.
 * Asumsi: Bidang sensor sejajar dengan bidang gerak atlet (fronto-parallel).
 */
class PinholeCameraTransformer(
    private val frameWidthPixels: Int,
    private val frameHeightPixels: Int,
    private val cameraDistanceMeters: Double,
    private val fovHorizontalDegrees: Double,
    private val fovVerticalDegrees: Double
) : CoordinateTransformer {

    override val metersPerPixelX: Double
    override val metersPerPixelY: Double

    init {
        // W_real = 2 * Distance * tan(FOV/2)
        val fovHorizontalRad = Math.toRadians(fovHorizontalDegrees)
        val fovVerticalRad = Math.toRadians(fovVerticalDegrees)

        val realWidthMeters = 2.0 * cameraDistanceMeters * tan(fovHorizontalRad / 2.0)
        val realHeightMeters = 2.0 * cameraDistanceMeters * tan(fovVerticalRad / 2.0)

        metersPerPixelX = realWidthMeters / frameWidthPixels.toDouble()
        metersPerPixelY = realHeightMeters / frameHeightPixels.toDouble()
    }

    override fun transform(imagePoint: ImagePoint): WorldPoint {
        // Memetakan linear dari piksel ke meter. Titik awal (0,0) tetap di kiri-atas.
        return WorldPoint(
            xMeters = imagePoint.xPx * metersPerPixelX,
            yMeters = imagePoint.yPx * metersPerPixelY
        )
    }

    /** Dipakai untuk vertical jump — konversi piksel Y ke meter */
    override fun transformDistance(pixels: Double): Double {
        return pixels * metersPerPixelY
    }

    /** Dipakai untuk horizontal jump (lompat jauh) — konversi piksel X ke meter */
    override fun transformHorizontalDistance(pixels: Double): Double {
        return pixels * metersPerPixelX
    }
}
