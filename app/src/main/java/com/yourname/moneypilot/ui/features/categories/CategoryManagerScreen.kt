package com.yourname.moneypilot.ui.features.categories

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import com.yourname.moneypilot.ui.common.ScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    onPopBackStack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var subcategoryTargetId by remember { mutableStateOf<Long?>(null) }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, icon, type ->
                viewModel.addCategory(name, type, 0, icon)
                showAddCategoryDialog = false
            }
        )
    }

    if (subcategoryTargetId != null) {
        AddSubcategoryDialog(
            onDismiss = { subcategoryTargetId = null },
            onConfirm = { name ->
                viewModel.addSubcategory(subcategoryTargetId!!, name)
                subcategoryTargetId = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories & Subcategories") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        items(state.data.categories) { category ->
                            CategoryItem(
                                category = category,
                                subcategories = state.data.subcategoriesMap[category.id] ?: emptyList(),
                                onDeleteCategory = { viewModel.deleteCategory(category) },
                                onAddSubcategory = { subcategoryTargetId = category.id },
                                onDeleteSubcategory = { viewModel.deleteSubcategory(it) }
                            )
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No categories found. Tap + to add one.")
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: CategoryEntity,
    subcategories: List<SubcategoryEntity>,
    onDeleteCategory: () -> Unit,
    onAddSubcategory: () -> Unit,
    onDeleteSubcategory: (SubcategoryEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(category.icon, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.name, fontWeight = FontWeight.Bold)
                    Text(category.type, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDeleteCategory) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    subcategories.forEach { sub ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sub.name, style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onDeleteSubcategory(sub) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Sub", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    TextButton(
                        onClick = onAddSubcategory,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Subcategory")
                    }
                }
            }
        }
    }
}

@Composable
fun AddSubcategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subcategory") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Subcategory Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("💰") }
    var type by remember { mutableStateOf("EXPENSE") }

    val emojis = listOf(
        "💰", "🍔", "🚗", "🏠", "🎁", "💊", "🎓", "✈️", "👔", "🍕", "⛽", "🛒",
        "💡", "📱", "🎮", "🎬", "🍿", "☕", "🍺", "🍷", "🍎", "🍗", "🚲", "🚆",
        "🚌", "🚢", "🚿", "🪑", "💄", "💅", "✂️", "🧺", "🧹", "🧤", "🧥", "👟",
        "👜", "🌂", "🧶", "🧵", "🪡", "🧘", "🏋️", "🏀", "⚽", "🎾", "🎨", "🎻",
        "🎸", "📷", "🔋", "💻", "⌨️", "🖱️", "💿", "💾", "📠", "🔌", "📺", "⏰",
        "🧭", "🔭", "🔬", "💉", "🩸", "🧺", "🚽", "🧴", "🧻", "🧼", "🧹", "🧽"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Pick an Emoji", style = MaterialTheme.typography.titleSmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (icon == emoji) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { icon = emoji }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE" }, label = { Text("Expense") })
                    FilterChip(selected = type == "INCOME", onClick = { type = "INCOME" }, label = { Text("Income") })
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, icon, type) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
