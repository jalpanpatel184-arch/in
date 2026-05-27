package com.nova.assistant.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nova.assistant.domain.model.Memory
import com.nova.assistant.ui.theme.*
import com.nova.assistant.ui.viewmodel.MemoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val memories by viewModel.memories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Bank", color = NovaTextPrimary, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = NovaCyan) }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add memory", tint = NovaCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovaDarkSurface)
            )
        },
        containerColor = NovaBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NovaCyan, contentColor = NovaBlack
            ) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search memories...", color = NovaTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = NovaTextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search("") }) {
                            Icon(Icons.Default.Clear, null, tint = NovaTextMuted)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaCyan,
                    unfocusedBorderColor = NovaTextMuted.copy(alpha = 0.3f),
                    focusedTextColor = NovaTextPrimary,
                    unfocusedTextColor = NovaTextPrimary,
                    cursorColor = NovaCyan
                )
            )

            if (memories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Memory, null, tint = NovaTextMuted, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No memories yet", style = MaterialTheme.typography.bodyLarge, color = NovaTextMuted)
                        Text("Nova will remember things as you talk", style = MaterialTheme.typography.bodyMedium, color = NovaTextMuted)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memories, key = { it.id }) { memory ->
                        MemoryCard(
                            memory = memory,
                            onDelete = { viewModel.deleteMemory(memory) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { key, value, category ->
                viewModel.addMemory(key, value, category)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MemoryCard(memory: Memory, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val categoryColor = when (memory.category) {
        "person"     -> NovaViolet
        "preference" -> NovaCyan
        "schedule"   -> NovaAmber
        "fact"       -> NovaGreen
        else         -> NovaTextSecondary
    }
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(memory.timestamp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NovaDarkSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category dot
            Box(
                modifier = Modifier.size(10.dp)
                    .background(categoryColor, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(memory.key, style = MaterialTheme.typography.bodyLarge,
                    color = NovaTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(memory.value, style = MaterialTheme.typography.bodyMedium,
                    color = NovaTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(memory.category.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = categoryColor)
                    Text("·", color = NovaTextMuted)
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = NovaTextMuted)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, null, tint = NovaRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AddMemoryDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("general") }
    val categories = listOf("general", "person", "preference", "schedule", "fact")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NovaDarkSurface,
        title = { Text("Add Memory", color = NovaCyan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NovaTextField(label = "Key (e.g. 'Mom's birthday')", value = key, onValueChange = { key = it })
                NovaTextField(label = "Value (e.g. 'March 15th')", value = value, onValueChange = { value = it })
                Text("Category", style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NovaCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NovaCyan
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (key.isNotBlank() && value.isNotBlank()) onAdd(key, value, selectedCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = NovaCyan, contentColor = NovaBlack)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = NovaTextSecondary) }
        }
    )
}

@Composable
fun NovaTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = NovaTextMuted) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NovaCyan,
            unfocusedBorderColor = NovaTextMuted.copy(alpha = 0.3f),
            focusedTextColor = NovaTextPrimary,
            unfocusedTextColor = NovaTextPrimary,
            cursorColor = NovaCyan
        )
    )
}
