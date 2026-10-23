package com.manspower.jumpmeasure.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manspower.jumpmeasure.data.local.dao.JumpDao
import com.manspower.jumpmeasure.data.local.entity.JumpEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val jumpDao: JumpDao
) : ViewModel() {

    val jumpHistory: StateFlow<List<JumpEntity>> = jumpDao.getAllJumps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun generateCsvContent(historyList: List<JumpEntity>): String {
        val csvBuilder = java.lang.StringBuilder()
        // Header
        csvBuilder.append("ID,Nama Peserta,Waktu,Tipe,Kategori,Gender,Norma,Tinggi(cm),Jarak(cm),Sudut(°),FlightTime(ms),Confidence(%)\n")

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

        for (jump in historyList) {
            val dateStr     = dateFormat.format(java.util.Date(jump.timestamp))
            val heightStr   = String.format(java.util.Locale.US, "%.2f", jump.jumpHeightCm)
            val distStr     = String.format(java.util.Locale.US, "%.2f", jump.horizontalDistanceCm)
            val angleStr    = String.format(java.util.Locale.US, "%.1f", jump.jumpAngleDegrees)
            val scoreStr    = String.format(java.util.Locale.US, "%.1f", jump.confidenceScore * 100)
            val nameEscaped = (if (jump.participantName.isNotBlank()) jump.participantName else "-").replace("\"", "\"\"")

            csvBuilder.append(
                "${jump.id},\"$nameEscaped\",$dateStr,${jump.jumpType},${jump.category},${jump.gender}," +
                "\"${jump.normaLabel}\",$heightStr,$distStr,$angleStr,${jump.flightTimeMs},$scoreStr\n"
            )
        }

        return csvBuilder.toString()
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            jumpDao.deleteAllJumps()
        }
    }
}
