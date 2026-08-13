package com.example.jumpmeasure.ui.language

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.jumpmeasure.domain.model.JumpCategory
import com.example.jumpmeasure.domain.model.JumpGender

// ─────────────────────────────────────────────────────────────────────────────
// Language Enum
// ─────────────────────────────────────────────────────────────────────────────
enum class AppLanguage { INDONESIAN, ENGLISH }

// ─────────────────────────────────────────────────────────────────────────────
// Instruction item model (used in CameraScreen guidance dialog)
// ─────────────────────────────────────────────────────────────────────────────
data class InstructionStrings(val emoji: String, val title: String, val desc: String)

// ─────────────────────────────────────────────────────────────────────────────
// AppStrings — single source of truth for all UI text
// ─────────────────────────────────────────────────────────────────────────────
data class AppStrings(
    val language: AppLanguage,

    // ── MainScreen ──────────────────────────────────────────────────────────
    val appSubtitle: String,
    val verticalJumpTitle: String,
    val verticalJumpSubtitle: String,
    val verticalJumpDesc: String,
    val horizontalJumpTitle: String,
    val horizontalJumpSubtitle: String,
    val horizontalJumpDesc: String,
    val viewHistory: String,
    val langToggleLabel: String,        // "EN" or "ID"

    // ── Category names ──────────────────────────────────────────────────────
    val categorySMP: String,
    val categorySMA: String,
    val categoryMahasiswa: String,
    val categoryAtlet: String,
    val categoryAgeSMP: String,
    val categoryAgeSMA: String,
    val categoryAgeMahasiswa: String,
    val categoryAgeAtlet: String,

    // ── Gender names ─────────────────────────────────────────────────────────
    val genderMale: String,
    val genderFemale: String,

    // ── CategoryScreen ───────────────────────────────────────────────────────
    val selectCategory: String,
    val categoryScreenSubtitle: String,

    // ── GenderScreen ─────────────────────────────────────────────────────────
    val selectGender: String,
    val genderScreenSubtitle: String,
    val jumpLabelVertical: String,
    val jumpLabelHorizontal: String,

    // ── CameraScreen — permissions & guidance ────────────────────────────────
    val cameraPermissionRequired: String,
    val guidancePrefix: String,         // "Panduan" / "Guide"
    val guideUnderstoodBtn: String,
    val guideTipText: String,

    // ── CameraScreen — HUD labels ────────────────────────────────────────────
    val phaseLabel: String,
    val durationLabel: String,
    val heightLabel: String,
    val distanceLabel: String,
    val angleLabel: String,
    val confidenceLabel: String,

    // ── CameraScreen — action buttons ────────────────────────────────────────
    val startTracking: String,
    val pauseBtn: String,
    val resumeBtn: String,
    val stopBtn: String,

    // ── CameraScreen — result dialog ─────────────────────────────────────────
    val jumpDetectedTitle: String,
    val flightDurationLabel: String,
    val jumpAngleLabel: String,
    val normaAssessment: String,
    val saveBtn: String,
    val discardBtn: String,

    // ── CameraScreen — instruction content ──────────────────────────────────
    val verticalInstructions: List<InstructionStrings>,
    val horizontalInstructions: List<InstructionStrings>,

    // ── HistoryScreen ────────────────────────────────────────────────────────
    val historyTitle: String,
    val deleteAllTitle: String,
    val deleteAllText: String,
    val deleteBtn: String,
    val cancelBtn: String,
    val noDataText: String,
    val verticalHistoryLabel: String,
    val horizontalHistoryLabel: String,
    val heightHistoryLabel: String,
    val distanceHistoryLabel: String,
    val angleHistoryLabel: String,
    val exportSuccess: String,
    val exportFailed: String,

    // ── Norma label translation map (DB labels are always stored in Indonesian)
    val normaLabelMap: Map<String, String>,
) {
    /** Returns the display name for a category in the current language. */
    fun categoryName(cat: JumpCategory): String = when (cat) {
        JumpCategory.SMP       -> categorySMP
        JumpCategory.SMA       -> categorySMA
        JumpCategory.MAHASISWA -> categoryMahasiswa
        JumpCategory.ATLET     -> categoryAtlet
    }

    /** Returns the display name for a gender in the current language. */
    fun genderName(gender: JumpGender): String = when (gender) {
        JumpGender.PUTRA -> genderMale
        JumpGender.PUTRI -> genderFemale
    }

    /**
     * Translates a norma label stored in the database (always Indonesian)
     * to the current display language.
     */
    fun translateNorma(label: String): String = normaLabelMap[label] ?: label
}

// ─────────────────────────────────────────────────────────────────────────────
// Indonesian Strings Instance
// ─────────────────────────────────────────────────────────────────────────────
val IndonesianStrings = AppStrings(
    language                 = AppLanguage.INDONESIAN,
    appSubtitle              = "AI-Powered Jump Analytics",
    verticalJumpTitle        = "Lompat Tegak",
    verticalJumpSubtitle     = "Loncat Tegak / Vertical Jump",
    verticalJumpDesc         = "Ukur tinggi lompatan berdasarkan\nwaktu melayang di udara",
    horizontalJumpTitle      = "Lompat Jauh",
    horizontalJumpSubtitle   = "Standing Long Jump / Horizontal Jump",
    horizontalJumpDesc       = "Ukur jarak lompatan horizontal\ndengan kalibrasi sudut & posisi",
    viewHistory              = "Lihat Riwayat Lompatan",
    langToggleLabel          = "EN",
    categorySMP              = "SMP",
    categorySMA              = "SMA / MA",
    categoryMahasiswa        = "Mahasiswa",
    categoryAtlet            = "Atlet",
    categoryAgeSMP           = "Usia ±12–15 Tahun",
    categoryAgeSMA           = "Usia ±16–18 Tahun",
    categoryAgeMahasiswa     = "Usia ±18–22 Tahun",
    categoryAgeAtlet         = "Atlet Profesional",
    genderMale               = "Putra",
    genderFemale             = "Putri",
    selectCategory           = "Pilih Kategori",
    categoryScreenSubtitle   = "Norma penilaian akan disesuaikan\ndengan kategori yang dipilih",
    selectGender             = "Pilih Jenis Kelamin",
    genderScreenSubtitle     = "Standar norma berbeda antara\nputra dan putri",
    jumpLabelVertical        = "Lompat Tegak",
    jumpLabelHorizontal      = "Lompat Jauh",
    cameraPermissionRequired = "Izin Kamera Dibutuhkan",
    guidancePrefix           = "Panduan",
    guideUnderstoodBtn       = "✓  Mengerti, Mulai!",
    guideTipText             = "💡 Tip: Pastikan skala kamera sesuai dan atlet berpakaian kontras dengan latar belakang untuk deteksi AI yang lebih akurat.",
    phaseLabel               = "FASE:",
    durationLabel            = "⏱ Durasi:",
    heightLabel              = "⬆ Tinggi:",
    distanceLabel            = "➡ Jarak:",
    angleLabel               = "📐 Sudut:",
    confidenceLabel          = "Confidence:",
    startTracking            = "MULAI TRACKING",
    pauseBtn                 = "PAUSE",
    resumeBtn                = "LANJUT",
    stopBtn                  = "STOP",
    jumpDetectedTitle        = "🏃 Lompatan Terdeteksi",
    flightDurationLabel      = "⏱ Durasi Melayang:",
    jumpAngleLabel           = "📐 Sudut Lompatan:",
    normaAssessment          = "Norma Penilaian",
    saveBtn                  = "💾 Simpan",
    discardBtn               = "Buang",
    verticalInstructions     = listOf(
        InstructionStrings(
            "📱", "Orientasi Kamera",
            "Gunakan mode PORTRAIT (HP berdiri tegak). Pastikan seluruh tubuh atlet dari kepala hingga kaki terlihat jelas di layar."
        ),
        InstructionStrings(
            "🧍", "Posisi Atlet & Sudut Pandang",
            "Atlet berdiri DEPAN KAMERA (menghadap ke layar). Kedua kaki dan pinggul harus terlihat jelas untuk akurasi terbaik."
        ),
        InstructionStrings(
            "📏", "Jarak & Tinggi Kamera",
            "Letakkan kamera sejauh 2–3 meter di depan atlet, setinggi dada/pinggang. Pastikan kamera stabil/menggunakan tripod (tidak goyang)."
        ),
        InstructionStrings(
            "⚡", "Prosedur Pengukuran",
            "Tekan MULAI TRACKING → Tunggu countdown 3 detik → Atlet melakukan lompat tegak (vertical jump) setinggi-tingginya → Hasil tinggi & waktu melayang terdeteksi otomatis saat mendarat."
        ),
        InstructionStrings(
            "💡", "Pencahayaan",
            "Gunakan ruangan/area yang terang dan rata. Hindari posisi membelakangi cahaya terang (backlight)."
        )
    ),
    horizontalInstructions   = listOf(
        InstructionStrings(
            "📱", "Orientasi Kamera",
            "Gunakan mode LANDSCAPE (HP tidur/miring). Posisikan HP tegak lurus (90°) tepat di SISI/SAMPING lintasan lompatan."
        ),
        InstructionStrings(
            "🧍", "Posisi Atlet & Deteksi Samping (1 Kaki)",
            "Atlet berada di posisi SAMPING. ⚠️ BISA TERDETEKSI: Model AI 3D Pose MediaPipe & sistem Mansport dapat mendeteksi kerangka 3D dari tampak samping, sehingga meski 1 kaki menutupi kaki lainnya saat bersiap, titik kaki tumpu (takeoff) dan mendarat (landing) tetap terdeteksi akurat!"
        ),
        InstructionStrings(
            "📏", "Jarak & Area Kamera",
            "Letakkan kamera sejauh 3–5 meter di samping atlet. Seluruh lintasan (titik awal lompat hingga tempat mendarat) HARUS muat di dalam frame layar."
        ),
        InstructionStrings(
            "📐", "Sudut Lompatan",
            "Sistem akan otomatis menghitung Sudut Lompatan (Takeoff Angle) dalam derajat (°). Sudut ideal lompat jauh adalah sekitar 35°–45°."
        ),
        InstructionStrings(
            "⚡", "Prosedur Pengukuran",
            "Tekan MULAI TRACKING → Countdown 3 detik → Atlet melompat ke arah depan sejauh-jauhnya → Sistem mencatat titik Tolakan (Takeoff) & Pendaratan (Landing)."
        ),
        InstructionStrings(
            "💡", "Kestabilan Kamera",
            "Gunakan tripod atau sandarkan HP agar tidak goyang sama sekali saat atlet melompat."
        )
    ),
    historyTitle             = "Riwayat Lompatan",
    deleteAllTitle           = "Hapus Semua Riwayat?",
    deleteAllText            = "Semua data riwayat lompatan akan dihapus secara permanen.",
    deleteBtn                = "Hapus",
    cancelBtn                = "Batal",
    noDataText               = "Belum ada data lompatan",
    verticalHistoryLabel     = "⬆ Lompat Tegak",
    horizontalHistoryLabel   = "➡ Lompat Jauh",
    heightHistoryLabel       = "Tinggi (Flight)",
    distanceHistoryLabel     = "Jarak",
    angleHistoryLabel        = "Sudut",
    exportSuccess            = "Berhasil mengekspor data CSV",
    exportFailed             = "Gagal mengekspor data:",
    normaLabelMap            = mapOf(
        "Sangat Baik"        to "Sangat Baik",
        "Baik"               to "Baik",
        "Cukup"              to "Cukup",
        "Sedang"             to "Sedang",
        "Kurang"             to "Kurang",
        "Sangat Kurang"      to "Sangat Kurang",
        "Elite / Internasional" to "Elite / Internasional",
        "Sangat Tinggi"      to "Sangat Tinggi",
        "Tinggi (Nasional)"  to "Tinggi (Nasional)",
        "Tidak Diketahui"    to "Tidak Diketahui"
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// English Strings Instance
// ─────────────────────────────────────────────────────────────────────────────
val EnglishStrings = AppStrings(
    language                 = AppLanguage.ENGLISH,
    appSubtitle              = "AI-Powered Jump Analytics",
    verticalJumpTitle        = "Vertical Jump",
    verticalJumpSubtitle     = "Standing Vertical Jump",
    verticalJumpDesc         = "Measure jump height based on\nairborne flight time",
    horizontalJumpTitle      = "Long Jump",
    horizontalJumpSubtitle   = "Standing Long Jump / Horizontal Jump",
    horizontalJumpDesc       = "Measure horizontal jump distance\nwith angle & position calibration",
    viewHistory              = "View Jump History",
    langToggleLabel          = "ID",
    categorySMP              = "Junior High",
    categorySMA              = "Senior High",
    categoryMahasiswa        = "University",
    categoryAtlet            = "Athlete",
    categoryAgeSMP           = "Age ±12–15 Years",
    categoryAgeSMA           = "Age ±16–18 Years",
    categoryAgeMahasiswa     = "Age ±18–22 Years",
    categoryAgeAtlet         = "Professional Athlete",
    genderMale               = "Male",
    genderFemale             = "Female",
    selectCategory           = "Select Category",
    categoryScreenSubtitle   = "Assessment norms will be adjusted\nto the selected category",
    selectGender             = "Select Gender",
    genderScreenSubtitle     = "Performance standards differ\nbetween male and female",
    jumpLabelVertical        = "Vertical Jump",
    jumpLabelHorizontal      = "Long Jump",
    cameraPermissionRequired = "Camera Permission Required",
    guidancePrefix           = "Guide",
    guideUnderstoodBtn       = "✓  Got it, Start!",
    guideTipText             = "💡 Tip: Ensure proper camera framing and the athlete wears clothing that contrasts with the background for better AI detection.",
    phaseLabel               = "PHASE:",
    durationLabel            = "⏱ Duration:",
    heightLabel              = "⬆ Height:",
    distanceLabel            = "➡ Distance:",
    angleLabel               = "📐 Angle:",
    confidenceLabel          = "Confidence:",
    startTracking            = "START TRACKING",
    pauseBtn                 = "PAUSE",
    resumeBtn                = "CONTINUE",
    stopBtn                  = "STOP",
    jumpDetectedTitle        = "🏃 Jump Detected",
    flightDurationLabel      = "⏱ Flight Duration:",
    jumpAngleLabel           = "📐 Jump Angle:",
    normaAssessment          = "Performance Grade",
    saveBtn                  = "💾 Save",
    discardBtn               = "Discard",
    verticalInstructions     = listOf(
        InstructionStrings(
            "📱", "Camera Orientation",
            "Use PORTRAIT mode (phone standing upright). Ensure the athlete's full body from head to toe is clearly visible on screen."
        ),
        InstructionStrings(
            "🧍", "Athlete Position & Angle",
            "The athlete stands FACING THE CAMERA. Both feet and hips must be clearly visible for best accuracy."
        ),
        InstructionStrings(
            "📏", "Camera Distance & Height",
            "Place the camera 2–3 meters in front of the athlete, at chest/waist height. Ensure the camera is stable/on a tripod (no shaking)."
        ),
        InstructionStrings(
            "⚡", "Measurement Procedure",
            "Press START TRACKING → Wait for 3-second countdown → Athlete performs a vertical jump as high as possible → Height & flight time are automatically detected upon landing."
        ),
        InstructionStrings(
            "💡", "Lighting",
            "Use a well-lit and evenly lit room/area. Avoid positioning with bright light behind the athlete (backlight)."
        )
    ),
    horizontalInstructions   = listOf(
        InstructionStrings(
            "📱", "Camera Orientation",
            "Use LANDSCAPE mode (phone lying sideways). Position the phone perpendicular (90°) directly to the SIDE of the jump runway."
        ),
        InstructionStrings(
            "🧍", "Athlete Position & Side Detection",
            "The athlete is in a SIDE-ON position. ⚠️ DETECTABLE: MediaPipe's 3D Pose AI & Mansport system can detect a 3D skeleton from the side, so even when one foot occludes the other during preparation, the takeoff and landing points are still accurately detected!"
        ),
        InstructionStrings(
            "📏", "Camera Distance & Area",
            "Place the camera 3–5 meters to the side of the athlete. The entire runway (from takeoff point to landing spot) MUST fit within the screen frame."
        ),
        InstructionStrings(
            "📐", "Jump Angle",
            "The system will automatically calculate the Takeoff Angle in degrees (°). The ideal long jump angle is around 35°–45°."
        ),
        InstructionStrings(
            "⚡", "Measurement Procedure",
            "Press START TRACKING → 3-second countdown → Athlete jumps forward as far as possible → System records the Takeoff & Landing points."
        ),
        InstructionStrings(
            "💡", "Camera Stability",
            "Use a tripod or prop the phone against something so it doesn't shake at all when the athlete jumps."
        )
    ),
    historyTitle             = "Jump History",
    deleteAllTitle           = "Delete All History?",
    deleteAllText            = "All jump history data will be permanently deleted.",
    deleteBtn                = "Delete",
    cancelBtn                = "Cancel",
    noDataText               = "No jump data yet",
    verticalHistoryLabel     = "⬆ Vertical Jump",
    horizontalHistoryLabel   = "➡ Long Jump",
    heightHistoryLabel       = "Height (Flight)",
    distanceHistoryLabel     = "Distance",
    angleHistoryLabel        = "Angle",
    exportSuccess            = "CSV data exported successfully",
    exportFailed             = "Failed to export data:",
    normaLabelMap            = mapOf(
        "Sangat Baik"        to "Very Good",
        "Baik"               to "Good",
        "Cukup"              to "Fair",
        "Sedang"             to "Average",
        "Kurang"             to "Poor",
        "Sangat Kurang"      to "Very Poor",
        "Elite / Internasional" to "Elite / International",
        "Sangat Tinggi"      to "Very High",
        "Tinggi (Nasional)"  to "High (National)",
        "Tidak Diketahui"    to "Unknown"
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal — provided from MainActivity, consumed in all screens
// ─────────────────────────────────────────────────────────────────────────────
val LocalAppStrings = staticCompositionLocalOf<AppStrings> { IndonesianStrings }
