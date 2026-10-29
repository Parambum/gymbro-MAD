package com.gymbro.mad.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.data.model.FoodItem
import com.gymbro.mad.data.repo.FoodRepository
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.ui.components.EmptyState
import com.gymbro.mad.ui.components.GhostButton
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.NumericStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest
import kotlin.math.roundToInt

import androidx.compose.runtime.rememberCoroutineScope
import com.gymbro.mad.data.model.Meal
import com.gymbro.mad.data.model.MealEntry
import com.gymbro.mad.data.repo.FuelRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * UI state for food search operations.
 */
sealed interface FoodSearchUiState {
    data object Idle : FoodSearchUiState
    data object Loading : FoodSearchUiState
    data class Success(val query: String, val items: List<FoodItem>) : FoodSearchUiState
    data class Empty(val query: String) : FoodSearchUiState
    data class Error(val query: String, val message: String) : FoodSearchUiState
}

private data class SearchTrigger(val query: String, val retryCount: Int)

/**
 * Food Search Screen (Ticket J-2 & J-4).
 *
 * Implements a 300ms debounce on the search field, connects to [FoodRepository],
 * and explicitly distinguishes honest no-results from network failures.
 * Selecting a food opens the [PortionSheet] to specify grams/portion and log directly to [FuelRepository].
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
fun FoodSearchScreen(
    modifier: Modifier = Modifier,
    foodRepository: FoodRepository = Repos.food,
    fuelRepository: FuelRepository = Repos.fuel,
    targetMeal: Meal? = null,
    targetDay: LocalDate = LocalDate.now(),
    onPicked: (foodId: String) -> Unit = {},
    onLogged: (MealEntry) -> Unit = {},
) {
    val palette = LocalPalette.current
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var selectedFoodForPortion by remember { mutableStateOf<FoodItem?>(null) }

    // Ticket J-3: Barcode scanning state
    var isScanningBarcode by rememberSaveable { mutableStateOf(false) }
    var cameraPermissionDenied by rememberSaveable { mutableStateOf(false) }
    var isLookingUpBarcode by remember { mutableStateOf(false) }
    var barcodeLookupError by remember { mutableStateOf<String?>(null) }
    var unknownBarcode by remember { mutableStateOf<String?>(null) }
    var barcodeForManualEntry by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                cameraPermissionDenied = false
                isScanningBarcode = true
            } else {
                cameraPermissionDenied = true
                isScanningBarcode = false
            }
        },
    )

    fun onScanBarcodeClicked() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            cameraPermissionDenied = false
            isScanningBarcode = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val uiState by remember(foodRepository) {
        snapshotFlow { SearchTrigger(query.trim(), retryCount) }
            .debounce { trigger ->
                if (trigger.query.isEmpty()) 0L else 300L
            }
            .distinctUntilChanged()
            .transformLatest { trigger ->
                val q = trigger.query
                if (q.isEmpty()) {
                    emit(FoodSearchUiState.Idle)
                } else {
                    emit(FoodSearchUiState.Loading)
                    try {
                        val results = foodRepository.search(q)
                        if (results.isEmpty()) {
                            emit(FoodSearchUiState.Empty(q))
                        } else {
                            emit(FoodSearchUiState.Success(q, results))
                        }
                    } catch (e: Exception) {
                        val message = e.message ?: "Failed to connect to food database"
                        emit(FoodSearchUiState.Error(q, message))
                    }
                }
            }
    }.collectAsStateWithLifecycle(initialValue = FoodSearchUiState.Idle)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Food Search",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Search product database for calories and macros.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search foods (e.g. chicken, oats)...",
                    color = palette.muted,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = palette.muted,
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = palette.muted,
                            )
                        }
                    }
                    IconButton(onClick = { onScanBarcodeClicked() }) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Scan Barcode",
                            tint = palette.accent,
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.border,
                focusedTextColor = palette.ink,
                unfocusedTextColor = palette.ink,
                cursorColor = palette.accent,
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface,
            ),
            shape = RoundedCornerShape(12.dp),
        )

        if (cameraPermissionDenied) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.raised)
                    .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = palette.warn,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Camera access is needed to scan barcodes. You can still search by typing above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { cameraPermissionDenied = false },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss notice",
                        tint = palette.faint,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        if (isLookingUpBarcode) {
            Spacer(Modifier.height(12.dp))
            GymBroCard(accent = palette.accent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = palette.accent,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Looking up barcode in database...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.ink,
                    )
                }
            }
        }

        if (barcodeLookupError != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.raised)
                    .border(1.dp, palette.danger, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = barcodeLookupError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.danger,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { barcodeLookupError = null },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss error",
                        tint = palette.muted,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionHeader(title = "Results")

        Spacer(Modifier.height(12.dp))

        when (val state = uiState) {
            is FoodSearchUiState.Idle -> {
                EmptyState(
                    title = "Find a food",
                    body = "Type the name or brand of any product to search the database.",
                    icon = Icons.Default.Search,
                )
            }

            is FoodSearchUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = palette.accent,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Searching product database...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted,
                        )
                    }
                }
            }

            is FoodSearchUiState.Empty -> {
                EmptyState(
                    title = "No results for \"${state.query}\"",
                    body = "No foods found matching this search. Try a different search term or check spelling.",
                    icon = Icons.Default.SearchOff,
                )
            }

            is FoodSearchUiState.Error -> {
                GymBroCard(
                    accent = palette.danger,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = palette.danger,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Network lookup failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.danger,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Could not fetch results for \"${state.query}\":\n${state.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.muted,
                            )
                            Spacer(Modifier.height(12.dp))
                            GhostButton(
                                text = "Retry Search",
                                icon = Icons.Default.Refresh,
                                onClick = { retryCount++ },
                            )
                        }
                    }
                }
            }

            is FoodSearchUiState.Success -> {
                Text(
                    text = "${state.items.size} ${if (state.items.size == 1) "result" else "results"} found",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        FoodResultCard(
                            item = item,
                            onClick = {
                                selectedFoodForPortion = item
                                onPicked(item.id)
                            },
                        )
                    }
                }
            }
        }

        if (isScanningBarcode) {
            BarcodeScannerSheet(
                onDismiss = { isScanningBarcode = false },
                onBarcodeScanned = { code ->
                    isScanningBarcode = false
                    isLookingUpBarcode = true
                    barcodeLookupError = null
                    coroutineScope.launch {
                        try {
                            val food = foodRepository.byBarcode(code)
                            isLookingUpBarcode = false
                            if (food != null) {
                                selectedFoodForPortion = food
                                onPicked(food.id)
                            } else {
                                unknownBarcode = code
                            }
                        } catch (e: Exception) {
                            isLookingUpBarcode = false
                            barcodeLookupError = "Failed to lookup barcode: ${e.message}"
                        }
                    }
                },
            )
        }

        unknownBarcode?.let { code ->
            UnknownBarcodeDialog(
                barcode = code,
                onAddByHand = {
                    unknownBarcode = null
                    barcodeForManualEntry = code
                },
                onDismiss = { unknownBarcode = null },
            )
        }

        barcodeForManualEntry?.let { code ->
            ManualFoodEntrySheet(
                barcode = code,
                onDismiss = { barcodeForManualEntry = null },
                onSaveFood = { foodItem ->
                    barcodeForManualEntry = null
                    selectedFoodForPortion = foodItem
                    onPicked(foodItem.id)
                },
            )
        }

        selectedFoodForPortion?.let { food ->
            PortionSheet(
                foodItem = food,
                initialMeal = targetMeal,
                targetDay = targetDay,
                onDismiss = { selectedFoodForPortion = null },
                onSaveEntry = { entry ->
                    coroutineScope.launch {
                        fuelRepository.addEntry(entry)
                        selectedFoodForPortion = null
                        onLogged(entry)
                    }
                },
            )
        }
    }
}

@Composable
private fun FoodResultCard(
    item: FoodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current

    GymBroCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                if (!item.brand.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${item.per100g.kcal.roundToInt()}",
                        style = NumericStyle,
                        color = palette.ink,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "kcal",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = palette.faint,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                MicroLabel(text = "per 100g")
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MacroItem(label = "P", grams = item.per100g.proteinG, dotColor = palette.accent)
            MacroItem(label = "C", grams = item.per100g.carbsG, dotColor = palette.accent2)
            MacroItem(label = "F", grams = item.per100g.fatG, dotColor = palette.warn)
        }
    }
}

@Composable
private fun MacroItem(
    label: String,
    grams: Double,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "$label: ${grams.roundToInt()}g",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = palette.muted,
        )
    }
}
