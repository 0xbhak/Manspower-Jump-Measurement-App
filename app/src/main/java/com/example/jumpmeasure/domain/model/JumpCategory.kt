package com.example.jumpmeasure.domain.model

enum class JumpCategory {
    SMP, SMA, MAHASISWA, ATLET
}

enum class JumpGender {
    PUTRA, PUTRI
}

data class NormaRange(
    val label: String,
    val minCm: Double,         // inclusive (-1 means no lower bound)
    val maxCm: Double          // inclusive (Double.MAX_VALUE means no upper bound)
)

data class NormaResult(
    val label: String,          // e.g. "Sangat Baik"
    val emoji: String           // e.g. "🏆"
)

object NormaEvaluator {

    // ─────────────────────────────────────────────
    // LONCAT TEGAK (Vertical Jump, in cm)
    // ─────────────────────────────────────────────
    private val verticalNorma: Map<Pair<JumpCategory, JumpGender>, List<NormaRange>> = mapOf(

        // SMP Putra
        Pair(JumpCategory.SMP, JumpGender.PUTRA) to listOf(
            NormaRange("Sangat Baik",  55.0, Double.MAX_VALUE),
            NormaRange("Baik",         45.0, 54.99),
            NormaRange("Cukup",        35.0, 44.99),
            NormaRange("Kurang",       25.0, 34.99),
            NormaRange("Sangat Kurang",-1.0, 24.99)
        ),
        // SMP Putri
        Pair(JumpCategory.SMP, JumpGender.PUTRI) to listOf(
            NormaRange("Sangat Baik",  45.0, Double.MAX_VALUE),
            NormaRange("Baik",         35.0, 44.99),
            NormaRange("Cukup",        25.0, 34.99),
            NormaRange("Kurang",       15.0, 24.99),
            NormaRange("Sangat Kurang",-1.0, 14.99)
        ),

        // SMA Putra
        Pair(JumpCategory.SMA, JumpGender.PUTRA) to listOf(
            NormaRange("Sangat Baik",  65.0, Double.MAX_VALUE),
            NormaRange("Baik",         55.0, 64.99),
            NormaRange("Cukup",        45.0, 54.99),
            NormaRange("Kurang",       35.0, 44.99),
            NormaRange("Sangat Kurang",-1.0, 34.99)
        ),
        // SMA Putri
        Pair(JumpCategory.SMA, JumpGender.PUTRI) to listOf(
            NormaRange("Sangat Baik",  50.0, Double.MAX_VALUE),
            NormaRange("Baik",         40.0, 49.99),
            NormaRange("Cukup",        30.0, 39.99),
            NormaRange("Kurang",       20.0, 29.99),
            NormaRange("Sangat Kurang",-1.0, 19.99)
        ),

        // MAHASISWA Putra
        Pair(JumpCategory.MAHASISWA, JumpGender.PUTRA) to listOf(
            NormaRange("Sangat Baik",  75.0, Double.MAX_VALUE),
            NormaRange("Baik",         65.0, 74.99),
            NormaRange("Cukup",        55.0, 64.99),
            NormaRange("Kurang",       45.0, 54.99),
            NormaRange("Sangat Kurang",-1.0, 44.99)
        ),
        // MAHASISWA Putri
        Pair(JumpCategory.MAHASISWA, JumpGender.PUTRI) to listOf(
            NormaRange("Sangat Baik",  60.0, Double.MAX_VALUE),
            NormaRange("Baik",         50.0, 59.99),
            NormaRange("Cukup",        40.0, 49.99),
            NormaRange("Kurang",       30.0, 39.99),
            NormaRange("Sangat Kurang",-1.0, 29.99)
        ),

        // ATLET Putra
        Pair(JumpCategory.ATLET, JumpGender.PUTRA) to listOf(
            NormaRange("Elite / Internasional", 90.0, Double.MAX_VALUE),
            NormaRange("Sangat Tinggi",          80.0, 89.99),
            NormaRange("Tinggi (Nasional)",       70.0, 79.99),
            NormaRange("Baik",                   60.0, 69.99),
            NormaRange("Cukup",                  50.0, 59.99),
            NormaRange("Kurang",                 -1.0, 49.99)
        ),
        // ATLET Putri
        Pair(JumpCategory.ATLET, JumpGender.PUTRI) to listOf(
            NormaRange("Elite / Internasional", 75.0, Double.MAX_VALUE),
            NormaRange("Sangat Tinggi",          65.0, 74.99),
            NormaRange("Tinggi (Nasional)",       55.0, 64.99),
            NormaRange("Baik",                   45.0, 54.99),
            NormaRange("Cukup",                  35.0, 44.99),
            NormaRange("Kurang",                 -1.0, 34.99)
        )
    )

    // ─────────────────────────────────────────────
    // LOMPAT JAUH (Horizontal Jump, in cm)
    // ─────────────────────────────────────────────
    private val horizontalNorma: Map<Pair<JumpCategory, JumpGender>, List<NormaRange>> = mapOf(

        // SMP Putra
        Pair(JumpCategory.SMP, JumpGender.PUTRA) to listOf(
            NormaRange("Sangat Baik",  225.0, Double.MAX_VALUE),
            NormaRange("Baik",         214.0, 224.99),
            NormaRange("Sedang",       203.0, 213.99),
            NormaRange("Kurang",       170.0, 202.99),
            NormaRange("Sangat Kurang",-1.0,  169.99)
        ),
        // SMP Putri
        Pair(JumpCategory.SMP, JumpGender.PUTRI) to listOf(
            NormaRange("Sangat Baik",  210.0, Double.MAX_VALUE),
            NormaRange("Baik",         190.0, 209.99),
            NormaRange("Sedang",       180.0, 189.99),
            NormaRange("Kurang",       151.0, 179.99),
            NormaRange("Sangat Kurang",-1.0,  150.99)
        ),

        // SMA Putra (same as SMP per document)
        Pair(JumpCategory.SMA, JumpGender.PUTRA) to listOf(
            NormaRange("Sangat Baik",  225.0, Double.MAX_VALUE),
            NormaRange("Baik",         214.0, 224.99),
            NormaRange("Sedang",       203.0, 213.99),
            NormaRange("Kurang",       170.0, 202.99),
            NormaRange("Sangat Kurang",-1.0,  169.99)
        ),
        // SMA Putri (same as SMP per document)
        Pair(JumpCategory.SMA, JumpGender.PUTRI) to listOf(
            NormaRange("Sangat Baik",  210.0, Double.MAX_VALUE),
            NormaRange("Baik",         190.0, 209.99),
            NormaRange("Sedang",       180.0, 189.99),
            NormaRange("Kurang",       151.0, 179.99),
            NormaRange("Sangat Kurang",-1.0,  150.99)
        ),

        // MAHASISWA Putra
        Pair(JumpCategory.MAHASISWA, JumpGender.PUTRA) to listOf(
            NormaRange("Sangat Baik",  300.0, Double.MAX_VALUE),
            NormaRange("Baik",         270.0, 299.99),
            NormaRange("Sedang",       250.0, 269.99),
            NormaRange("Kurang",       230.0, 249.99),
            // [FIX v3] Bug: sebelumnya maxCm = 199.99 sehingga nilai 200-229 cm
            // tidak masuk kategori apapun dan hasilnya "Tidak Diketahui".
            NormaRange("Sangat Kurang",-1.0,  229.99)
        ),

        // MAHASISWA Putri
        Pair(JumpCategory.MAHASISWA, JumpGender.PUTRI) to listOf(
            NormaRange("Sangat Baik",  280.0, Double.MAX_VALUE),
            NormaRange("Baik",         250.0, 279.99),
            NormaRange("Sedang",       220.0, 249.99),
            NormaRange("Kurang",       190.0, 219.99),
            // [FIX v3] Bug: sebelumnya maxCm = 169.99 sehingga nilai 170-189 cm
            // tidak masuk kategori apapun dan hasilnya "Tidak Diketahui".
            NormaRange("Sangat Kurang",-1.0,  189.99)
        ),

        // ATLET Putra
        Pair(JumpCategory.ATLET, JumpGender.PUTRA) to listOf(
            NormaRange("Sangat Baik",  340.0, Double.MAX_VALUE),
            NormaRange("Baik",         310.0, 339.99),
            NormaRange("Sedang",       280.0, 309.99),
            NormaRange("Kurang",       250.0, 279.99),
            NormaRange("Sangat Kurang",-1.0,  249.99)
        ),
        // ATLET Putri
        Pair(JumpCategory.ATLET, JumpGender.PUTRI) to listOf(
            NormaRange("Sangat Baik",  294.0, Double.MAX_VALUE),
            NormaRange("Baik",         265.0, 293.99),
            NormaRange("Sedang",       235.0, 264.99),
            NormaRange("Kurang",       205.0, 234.99),
            NormaRange("Sangat Kurang",-1.0,  204.99)
        )
    )

    fun evaluate(
        valueCm: Double,
        jumpType: String,         // "VERTICAL" or "HORIZONTAL"
        category: JumpCategory,
        gender: JumpGender
    ): NormaResult {
        val key = Pair(category, gender)
        val normaMap = if (jumpType == "VERTICAL") verticalNorma else horizontalNorma
        val ranges = normaMap[key] ?: return NormaResult("Tidak Diketahui", "❓")

        for (range in ranges) {
            if (valueCm >= range.minCm && valueCm <= range.maxCm) {
                val emoji = when (range.label) {
                    "Sangat Baik", "Elite / Internasional" -> "🏆"
                    "Baik", "Sangat Tinggi"               -> "⭐"
                    "Tinggi (Nasional)"                    -> "🥇"
                    "Cukup", "Sedang"                      -> "✅"
                    "Kurang"                               -> "⚠️"
                    "Sangat Kurang"                        -> "❌"
                    else                                   -> "📊"
                }
                return NormaResult(range.label, emoji)
            }
        }
        return NormaResult("Tidak Diketahui", "❓")
    }

    fun displayName(category: JumpCategory): String = when (category) {
        JumpCategory.SMP       -> "SMP"
        JumpCategory.SMA       -> "SMA / MA"
        JumpCategory.MAHASISWA -> "Mahasiswa"
        JumpCategory.ATLET     -> "Atlet"
    }

    fun displayName(gender: JumpGender): String = when (gender) {
        JumpGender.PUTRA -> "Putra"
        JumpGender.PUTRI -> "Putri"
    }
}
