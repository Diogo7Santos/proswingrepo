package com.example.proswing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.viewmodel.MyBagViewModel
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBagScreen(viewModel: MyBagViewModel = viewModel()) {
    val clubs by viewModel.clubs.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (clubs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No clubs yet. Tap + to add one.", fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(clubs, key = { it.id }) { club ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart ||
                                    value == SwipeToDismissBoxValue.StartToEnd
                                ) {
                                    // Delete and show undo option
                                    viewModel.deleteClub(club)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Removed ${club.brand} ${club.model}",
                                            actionLabel = "Undo",
                                            withDismissAction = true
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            // Undo pressed — reinsert
                                            viewModel.addClub(
                                                club.type,
                                                club.variant,
                                                club.brand,
                                                club.model
                                            )
                                        }
                                    }
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd,
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Deleting...",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true
                        ) {
                            ClubCard(
                                type = club.type,
                                variant = club.variant,
                                brand = club.brand,
                                model = club.model
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddClubDialog(
            onDismiss = { showDialog = false },
            onAdd = { newClub ->
                viewModel.addClub(newClub.type, newClub.variant, newClub.brand, newClub.model)
                showDialog = false
            }
        )
    }
}

@Composable
fun ClubCard(type: String, variant: String?, brand: String, model: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$type ${variant ?: ""}".trim(), style = MaterialTheme.typography.titleMedium)
            Text("Brand: $brand")
            Text("Model: $model")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClubDialog(onDismiss: () -> Unit, onAdd: (GolfClub) -> Unit) {
    var selectedType by remember { mutableStateOf("Driver") }
    var selectedVariant by remember { mutableStateOf<String?>(null) }
    var selectedBrand by remember { mutableStateOf("") }
    var expandedBrand by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf("") }

    val clubTypes = listOf("Driver", "Wood", "Hybrid", "Iron", "Wedge", "Putter")
    val brands = listOf(
        "Adams Golf", "Ben Hogan Golf", "Callaway", "Cleveland Golf", "Cobra",
        "Honma", "Mizuno", "Odyssey", "Ping", "PXG (Parsons Xtreme Golf)", "Srixon",
        "Takomo Golf", "TaylorMade", "Titleist", "Tour Edge", "Slazenger",
        "Wilson Staff", "XXIO", "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a Club") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select Club Type:")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    clubTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                selectedVariant = null
                            },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                when (selectedType) {
                    "Wood" -> VariantSelector((2..9).map { "${it}w" }) { selectedVariant = it }
                    "Hybrid" -> VariantSelector((1..9).map { "${it}h" }) { selectedVariant = it }
                    "Iron" -> VariantSelector((1..9).map { "${it}i" }) { selectedVariant = it }
                    "Wedge" -> VariantSelector(
                        listOf("PW", "SW", "LW", "50°", "52°", "54°", "56°", "58°", "60°")
                    ) { selectedVariant = it }
                }

                Spacer(Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedBrand,
                    onExpandedChange = { expandedBrand = !expandedBrand }
                ) {
                    TextField(
                        readOnly = true,
                        value = selectedBrand,
                        onValueChange = {},
                        label = { Text("Select Brand") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBrand,
                        onDismissRequest = { expandedBrand = false }
                    ) {
                        brands.forEach { brand ->
                            DropdownMenuItem(
                                text = { Text(brand) },
                                onClick = {
                                    selectedBrand = brand
                                    expandedBrand = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedBrand.isNotEmpty() && model.isNotEmpty()) {
                        onAdd(GolfClub(selectedType, selectedVariant, selectedBrand, model))
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VariantSelector(variants: List<String>, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        variants.forEach { label ->
            AssistChip(
                onClick = { onSelect(label) },
                label = { Text(label) }
            )
        }
    }
}

data class GolfClub(
    val type: String,
    val variant: String?,
    val brand: String,
    val model: String
)
