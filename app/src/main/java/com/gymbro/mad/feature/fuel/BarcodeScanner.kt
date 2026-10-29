package com.gymbro.mad.feature.fuel

import android.content.Context
import kotlin.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.theme.LocalPalette
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import com.gymbro.mad.data.model.FoodItem
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.GhostButton
import com.gymbro.mad.ui.components.SectionHeader
import java.util.UUID

/**
 * Ticket J-3: Barcode Scanner using CameraX and Google ML Kit.
 *
 * Requirements:
 * - Reads EAN-13, UPC-A, EAN-8, UPC-E.
 * - Releases camera when the screen leaves composition.
 * - Non-blocking camera preview with alignment reticle and torch control.
 */
@Composable
fun BarcodeScannerSheet(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalPalette.current

    var hasTorch by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    val isScanningActive = remember { AtomicBoolean(true) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            isScanningActive.set(false)
            cameraProvider?.unbindAll()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ── Camera Preview ─────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_QR_CODE,
                        )
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)

                    val analysisExecutor = Executors.newSingleThreadExecutor()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        processImageProxy(
                            barcodeScanner = barcodeScanner,
                            imageProxy = imageProxy,
                            isScanningActive = isScanningActive,
                            onDetected = { code ->
                                executor.execute {
                                    onBarcodeScanned(code)
                                }
                            },
                        )
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        provider.unbindAll()
                        val camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis,
                        )
                        cameraControl = camera.cameraControl
                        hasTorch = camera.cameraInfo.hasFlashUnit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, executor)

                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Reticle Overlay ────────────────────────────────────────
        ScannerOverlay(accentColor = palette.accent)

        // ── Top Controls Bar ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close scanner",
                        tint = Color.White,
                    )
                }
            }

            MicroLabel(
                text = "BARCODE SCANNER",
                color = Color.White.copy(alpha = 0.8f),
            )

            if (hasTorch) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    IconButton(
                        onClick = {
                            val nextState = !isTorchOn
                            cameraControl?.enableTorch(nextState)
                            isTorchOn = nextState
                        },
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle flashlight",
                            tint = if (isTorchOn) palette.accent else Color.White,
                        )
                    }
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        // ── Bottom Instruction Pill ────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .border(1.dp, palette.border.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Align EAN-13 or UPC barcode inside frame",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    isScanningActive: AtomicBoolean,
    onDetected: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null && isScanningActive.get()) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (!isScanningActive.get()) return@addOnSuccessListener
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        if (isScanningActive.compareAndSet(true, false)) {
                            onDetected(rawValue)
                            break
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * Renders a darkened vignette with a clear rounded rectangular cutout in the center
 * and sharp accent corners to indicate the scanning zone.
 */
@Composable
private fun ScannerOverlay(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val boxWidth = (canvasWidth * 0.75f).coerceAtMost(320.dp.toPx())
        val boxHeight = boxWidth * 0.65f

        val left = (canvasWidth - boxWidth) / 2f
        val top = (canvasHeight - boxHeight) / 2.3f

        // Dark dim overlay
        drawRect(color = Color.Black.copy(alpha = 0.55f))

        // Transparent cutout inside the scanning rectangle
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            blendMode = BlendMode.Clear,
        )

        // Subdued border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx()),
        )

        // Accent corner highlights
        val cornerLength = 24.dp.toPx()
        val strokeWidth = 3.dp.toPx()

        // Top-left
        drawLine(accentColor, Offset(left, top + 12.dp.toPx()), Offset(left, top + 12.dp.toPx() + cornerLength), strokeWidth)
        drawLine(accentColor, Offset(left + 12.dp.toPx(), top), Offset(left + 12.dp.toPx() + cornerLength, top), strokeWidth)

        // Top-right
        drawLine(accentColor, Offset(left + boxWidth, top + 12.dp.toPx()), Offset(left + boxWidth, top + 12.dp.toPx() + cornerLength), strokeWidth)
        drawLine(accentColor, Offset(left + boxWidth - 12.dp.toPx(), top), Offset(left + boxWidth - 12.dp.toPx() - cornerLength, top), strokeWidth)

        // Bottom-left
        drawLine(accentColor, Offset(left, top + boxHeight - 12.dp.toPx()), Offset(left, top + boxHeight - 12.dp.toPx() - cornerLength), strokeWidth)
        drawLine(accentColor, Offset(left + 12.dp.toPx(), top + boxHeight), Offset(left + 12.dp.toPx() + cornerLength, top + boxHeight), strokeWidth)

        // Bottom-right
        drawLine(accentColor, Offset(left + boxWidth, top + boxHeight - 12.dp.toPx()), Offset(left + boxWidth, top + boxHeight - 12.dp.toPx() - cornerLength), strokeWidth)
        drawLine(accentColor, Offset(left + boxWidth - 12.dp.toPx(), top + boxHeight), Offset(left + boxWidth - 12.dp.toPx() - cornerLength, top + boxHeight), strokeWidth)
    }
}


/**
 * Dialog shown when a scanned barcode is not in the database.
 * Requirement: An unknown barcode offers to add the food by hand instead of dead-ending.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnknownBarcodeDialog(
    barcode: String,
    onAddByHand: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = {
            Text(
                text = "Barcode Not Found",
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
            )
        },
        text = {
            Column {
                Text(
                    text = "Barcode $barcode was not found in the food database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Would you like to add its name and nutrition by hand?",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.faint,
                )
            }
        },
        confirmButton = {
            AccentButton(
                text = "Add by Hand",
                onClick = onAddByHand,
            )
        },
        dismissButton = {
            GhostButton(
                text = "Cancel",
                onClick = onDismiss,
            )
        },
    )
}

/**
 * Bottom sheet to manually add an unknown food item by entering name and macros per 100g.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualFoodEntrySheet(
    barcode: String,
    onDismiss: () -> Unit,
    onSaveFood: (FoodItem) -> Unit,
) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable { mutableStateOf("") }
    var kcalInput by rememberSaveable { mutableStateOf("") }
    var proteinInput by rememberSaveable { mutableStateOf("") }
    var carbsInput by rememberSaveable { mutableStateOf("") }
    var fatInput by rememberSaveable { mutableStateOf("") }

    val kcal = kcalInput.toDoubleOrNull() ?: 0.0
    val protein = proteinInput.toDoubleOrNull() ?: 0.0
    val carbs = carbsInput.toDoubleOrNull() ?: 0.0
    val fat = fatInput.toDoubleOrNull() ?: 0.0
    val isValid = name.isNotBlank() && (kcal > 0 || protein > 0 || carbs > 0 || fat > 0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Add Food by Hand",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.ink,
                    )
                    MicroLabel(
                        text = "BARCODE: $barcode",
                        color = palette.muted,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = palette.muted,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food or Brand Name") },
                placeholder = { Text("e.g. Protein Bar") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.ink,
                    unfocusedTextColor = palette.ink,
                ),
            )

            Spacer(Modifier.height(14.dp))
            SectionHeader(title = "Nutrition per 100g")
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = kcalInput,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) kcalInput = it },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.ink,
                        unfocusedTextColor = palette.ink,
                    ),
                )
                OutlinedTextField(
                    value = proteinInput,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) proteinInput = it },
                    label = { Text("Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.ink,
                        unfocusedTextColor = palette.ink,
                    ),
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = carbsInput,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) carbsInput = it },
                    label = { Text("Carbs (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.info,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.ink,
                        unfocusedTextColor = palette.ink,
                    ),
                )
                OutlinedTextField(
                    value = fatInput,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) fatInput = it },
                    label = { Text("Fat (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.warn,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.ink,
                        unfocusedTextColor = palette.ink,
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))

            AccentButton(
                text = "Continue to Portion",
                enabled = isValid,
                onClick = {
                    val foodItem = FoodItem(
                        id = "custom_${UUID.randomUUID().toString().take(8)}",
                        name = name.trim(),
                        per100g = Macros(
                            kcal = kcal,
                            proteinG = protein,
                            carbsG = carbs,
                            fatG = fat,
                        ),
                        barcode = barcode,
                    )
                    onSaveFood(foodItem)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
