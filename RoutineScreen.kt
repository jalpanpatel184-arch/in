package com.nova.assistant.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nova.assistant.domain.model.Routine
import com.nova.assistant.ui.theme.*
import com.nova.assistant.ui.viewmodel.RoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    onBack: () -> Unit,
    viewModel: RoutineViewModel = hiltViewModel()
) {
    val routines by viewModel.routines.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routines & Macros", color = NovaTextPrimary, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = NovaCyan) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovaDarkSurface)
            )
        },
        containerColor = NovaBlack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = NovaCyan, contentColor = NovaBlack,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New Routine") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Preset routines header
            item {
                Text("PRESETS", style = MaterialTheme.typography.labelSmall,
                    color = NovaCyan, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            }
            items(PRESET_ROUTINES) { preset ->
                PresetRoutineCard(preset = preset, onEnable = { viewModel.enablePreset(preset) })
            }

            if (routines.isNotEmpty()) {
                item {
                    Text("MY ROUTINES", style = MaterialTheme.typography.labelSmall,
                        color = NovaCyan, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp))
                }
                items(routines, key = { it.id }) { routine ->
                    RoutineCard(
                        routine = routine,
                        onToggle = { viewModel.toggleRoutine(routine.id, it) },
                        onDelete = { viewModel.deleteRoutine(routine.id) },
                        onRun    = { viewModel.runRoutine(routine) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCreateDialog) {
        CreateRoutineDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, trigger ->
                viewModel.createRoutine(name, trigger)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun RoutineCard(routine: Routine, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onRun: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NovaDarkSurface,
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = NovaViolet, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(routine.name, style = MaterialTheme.typography.bodyLarge, color = NovaTextPrimary)
                    Text("\"${routine.triggerPhrase}\"", style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
                }
                Switch(checked = routine.isEnabled, onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = NovaCyan, checkedThumbColor = NovaBlack))
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Text("${routine.steps.size} steps", style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
                routine.steps.forEachIndexed { i, step ->
                    Text("${i + 1}. ${step.action}", style = MaterialTheme.typography.bodyMedium, color = NovaTextMuted,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRun, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(NovaCyan.copy(alpha = 0.5f)))) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Run Now")
                    }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null, tint = NovaRed.copy(alpha = 0.7f)) }
                }
            }
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.End)) {
                Text(if (expanded) "Less" else "Details", color = NovaTextMuted, style = MaterialTheme.typography.labelSmall)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = NovaTextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun PresetRoutineCard(preset: PresetRoutine, onEnable: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NovaCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(preset.icon, null, tint = preset.color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.bodyLarge, color = NovaTextPrimary)
                Text(preset.description, style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
            }
            OutlinedButton(
                onClick = onEnable,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = preset.color),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(preset.color.copy(alpha = 0.4f)))
            ) { Text("Add", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
fun CreateRoutineDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var trigger by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NovaDarkSurface,
        title = { Text("Create Routine", color = NovaCyan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NovaTextField(label = "Routine name", value = name, onValueChange = { name = it })
                NovaTextField(label = "Trigger phrase (e.g. 'morning routine')", value = trigger, onValueChange = { trigger = it })
                Text("Note: Add specific steps by telling Nova to record a macro.",
                    style = MaterialTheme.typography.bodyMedium, color = NovaTextMuted)
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && trigger.isNotBlank()) onCreate(name, trigger) },
                colors = ButtonDefaults.buttonColors(containerColor = NovaCyan, contentColor = NovaBlack)) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = NovaTextSecondary) } }
    )
}

// ── Preset routines data ──────────────────────────────
data class PresetRoutine(val name: String, val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color)

val PRESET_ROUTINES = listOf(
    PresetRoutine("Morning Routine", "Turn off alarm, max brightness, play news", Icons.Default.WbSunny, NovaAmber),
    PresetRoutine("Commute Mode", "Open Maps, start Spotify, DND off", Icons.Default.DirectionsCar, NovaCyan),
    PresetRoutine("Focus Mode", "DND on, Spotify focus playlist, dim screen", Icons.Default.SelfImprovement, NovaViolet),
    PresetRoutine("Sleep Mode", "Alarm set, DND on, dim brightness, Spotify off", Icons.Default.Bedtime, Color(0xFF6B7FFF)),
    PresetRoutine("Gym Mode", "Open Spotify, volume max, fitness tracker", Icons.Default.FitnessCenter, NovaGreen)
)

private val Color = androidx.compose.ui.graphics.Color
