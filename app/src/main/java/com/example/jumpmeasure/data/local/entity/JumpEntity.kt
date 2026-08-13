package com.example.jumpmeasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jumps")
data class JumpEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jumpType: String,           // "VERTICAL" or "HORIZONTAL"
    val jumpHeightCm: Double,
    val horizontalDistanceCm: Double,
    val jumpAngleDegrees: Double,
    val flightTimeMs: Long,
    val confidenceScore: Double,
    val category: String,           // "SMP", "SMA", "MAHASISWA", "ATLET"
    val gender: String,             // "PUTRA", "PUTRI"
    val normaLabel: String,         // e.g. "Sangat Baik"
    val timestamp: Long = System.currentTimeMillis()
)

