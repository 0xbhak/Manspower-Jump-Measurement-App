package com.manspower.jumpmeasure.core.analytics

enum class JumpPhase {
    IDLE,          // Kerangka tidak lengkap atau belum siap
    STANDING,      // Atlet berdiri tegak, merekam baseline ground
    PREPARATION,   // Pinggul turun (berjongkok) bersiap melompat
    AIRBORNE,      // Kaki terlepas dari lantai
    LANDING        // Kaki kembali menyentuh lantai
}
