package com.manspower.jumpmeasure.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manspower.jumpmeasure.data.local.entity.JumpEntity
import com.manspower.jumpmeasure.theme.NeonCyan
import com.manspower.jumpmeasure.theme.NeonPurple
import com.manspower.jumpmeasure.theme.SkeletonGreen
import com.manspower.jumpmeasure.ui.language.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    BackHandler(enabled = true) {
        onBack()
    }

    val s       = LocalAppStrings.current
    val history by viewModel.jumpHistory.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredHistory = remember(history, searchQuery) {
        if (searchQuery.isBlank()) {
            history
        } else {
            val q = searchQuery.trim()
            history.filter {
                it.participantName.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true) ||
                it.normaLabel.contains(q, ignoreCase = true)
            }
        }
    }

    // Capture current strings for use inside the launcher callback
    val currentStrings by rememberUpdatedState(s)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val writer = OutputStreamWriter(outputStream)
                            val dataToExport = if (searchQuery.isNotBlank()) filteredHistory else history
                            writer.write(viewModel.generateCsvContent(dataToExport))
                            writer.flush()
                        }
                    }
                    snackbarHostState.showSnackbar(currentStrings.exportSuccess)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("${currentStrings.exportFailed} ${e.localizedMessage}")
                }
            }
        }
    }

    com.manspower.jumpmeasure.ui.components.AthleticBackground {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(s.historyTitle, color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        if (history.isNotEmpty()) {
                            IconButton(onClick = { exportLauncher.launch("DataLompatan.csv") }) {
                                Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = Color.White)
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete History", tint = Color.Red)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(s.deleteAllTitle) },
                text  = { Text(s.deleteAllText) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearHistory()
                        showDeleteDialog = false
                    }) {
                        Text(s.deleteBtn, color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(s.cancelBtn)
                    }
                }
            )
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(s.noDataText, color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search bar for participant name / category
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(s.searchParticipantHint, color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        cursorColor = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada hasil untuk \"$searchQuery\"",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                    ) {
                        items(filteredHistory) { jump ->
                            JumpHistoryCard(jump)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun JumpHistoryCard(jump: JumpEntity) {
    val s           = LocalAppStrings.current
    val isVertical  = jump.jumpType == "VERTICAL"
    val accentColor = if (isVertical) NeonPurple else NeonCyan
    val formatter   = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString  = formatter.format(Date(jump.timestamp))
    val nameDisplay = if (jump.participantName.isNotBlank()) jump.participantName else s.defaultParticipantName

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Participant row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "👤 $nameDisplay",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text  = dateString,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))

            // Sub-header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = if (isVertical) s.verticalHistoryLabel else s.horizontalHistoryLabel,
                    color      = accentColor,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = "${jump.category} · ${jump.gender}",
                    color = accentColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(12.dp))

            // Main metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(s.heightHistoryLabel, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text  = String.format(Locale.US, "%.2f cm", jump.jumpHeightCm),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (!isVertical) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.distanceHistoryLabel, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text  = String.format(Locale.US, "%.2f cm", jump.horizontalDistanceCm),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (jump.jumpAngleDegrees > 0.0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(s.angleHistoryLabel, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text  = String.format(Locale.US, "%.1f°", jump.jumpAngleDegrees),
                                color = Color.Yellow,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Norma label — translated from DB Indonesian label to current language
            if (jump.normaLabel.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text  = "📊 ${s.translateNorma(jump.normaLabel)}",
                        color = accentColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "Flight: ${jump.flightTimeMs} ms",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
                val confidencePercent = jump.confidenceScore * 100
                Text(
                    text  = String.format(Locale.US, "Akurasi: %.1f%%", confidencePercent),
                    color = if (confidencePercent > 80.0) SkeletonGreen else Color.Yellow,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
