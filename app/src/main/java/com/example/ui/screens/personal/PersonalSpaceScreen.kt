package com.example.ui.screens.personal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedItem
import com.example.data.repository.PersonalSpaceRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalSpaceScreen(
    personalSpaceRepository: PersonalSpaceRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedItems by personalSpaceRepository.observeSavedItems().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    var itemTitleInput by remember { mutableStateOf("") }
    var itemContentInput by remember { mutableStateOf("") }
    var itemTypeInput by remember { mutableStateOf("note") } // "note", "link", "image", "document"

    val categories = listOf("All", "Notes", "Links", "Media", "Docs")

    val filteredItems = remember(savedItems, searchQuery, selectedCategory) {
        savedItems.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) true
            else item.title.contains(searchQuery, ignoreCase = true) ||
                 item.content.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Notes" -> item.type == "note"
                "Links" -> item.type == "link"
                "Media" -> item.type == "image" || item.type == "voice"
                "Docs" -> item.type == "document"
                else -> true
            }

            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF339AF0))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("My Personal Space", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Private notes, saved media & links", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF339AF0),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_saved_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Save item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search private notes and saved items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF339AF0)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF339AF0),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No saved items yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Save private thoughts, important links, images and drafts securely here.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredItems, key = { it.id }) { item ->
                        SavedItemCard(
                            item = item,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Saved Item", item.content))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                scope.launch {
                                    personalSpaceRepository.deleteSavedItem(item.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Saved Item Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Save to Personal Space") },
            text = {
                Column {
                    OutlinedTextField(
                        value = itemTitleInput,
                        onValueChange = { itemTitleInput = it },
                        label = { Text("Title / Topic") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = itemContentInput,
                        onValueChange = { itemContentInput = it },
                        label = { Text("Note content or URL") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("note" to "Note", "link" to "Link", "document" to "Doc", "image" to "Media").forEach { (type, label) ->
                            Button(
                                onClick = { itemTypeInput = type },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (itemTypeInput == type) Color(0xFF339AF0) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = if (itemTypeInput == type) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemTitleInput.isNotBlank() && itemContentInput.isNotBlank()) {
                            scope.launch {
                                personalSpaceRepository.addSavedItem(
                                    title = itemTitleInput.trim(),
                                    content = itemContentInput.trim(),
                                    type = itemTypeInput
                                )
                            }
                            itemTitleInput = ""
                            itemContentInput = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SavedItemCard(
    item: SavedItem,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(item.timestamp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (item.type) {
                        "link" -> Icons.Default.Link
                        "document" -> Icons.Default.Description
                        "image" -> Icons.Default.Image
                        "voice" -> Icons.Default.Mic
                        else -> Icons.Default.Note
                    }
                    Icon(icon, contentDescription = null, tint = Color(0xFF339AF0), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(text = item.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(text = dateStr, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
