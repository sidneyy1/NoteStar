package com.riguel.notestar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riguel.notestar.data.ExportOptions
import com.riguel.notestar.data.ExportPageChoice
import com.riguel.notestar.data.ExportQuality
import com.riguel.notestar.data.parsePageRange

/**
 * The "Export as PDF" bottom sheet: annotations toggle, page selection,
 * quality and file name. Pure UI — it hands the chosen [ExportOptions] back
 * through [onExport].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportOptionsSheet(
    docTitle: String,
    pageCount: Int,
    hasCurrentPage: Boolean,
    onDismiss: () -> Unit,
    onExport: (ExportOptions) -> Unit
) {
    var includeAnnotations by remember { mutableStateOf(true) }
    var pageChoice by remember {
        mutableStateOf(ExportPageChoice.ALL)
    }
    var customRange by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf(ExportQuality.STANDARD) }
    var fileName by remember { mutableStateOf("$docTitle-annotated") }

    val rangeValid = pageChoice != ExportPageChoice.CUSTOM ||
        parsePageRange(customRange, pageCount) != null
    val canExport = fileName.isNotBlank() && rangeValid

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Export as PDF",
                style = MaterialTheme.typography.titleLarge,
                color = DesignTokens.TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Include annotations",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DesignTokens.TextPrimary
                    )
                    Text(
                        text = "Off exports the clean original pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignTokens.TextSecondary
                    )
                }
                Switch(
                    checked = includeAnnotations,
                    onCheckedChange = { includeAnnotations = it }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pages",
                style = MaterialTheme.typography.titleSmall,
                color = DesignTokens.TextPrimary
            )
            RadioRow(
                label = "All pages",
                selected = pageChoice == ExportPageChoice.ALL,
                onClick = { pageChoice = ExportPageChoice.ALL }
            )
            if (hasCurrentPage) {
                RadioRow(
                    label = "Current page",
                    selected = pageChoice == ExportPageChoice.CURRENT,
                    onClick = { pageChoice = ExportPageChoice.CURRENT }
                )
            }
            RadioRow(
                label = "Custom range",
                selected = pageChoice == ExportPageChoice.CUSTOM,
                onClick = { pageChoice = ExportPageChoice.CUSTOM }
            )
            if (pageChoice == ExportPageChoice.CUSTOM) {
                OutlinedTextField(
                    value = customRange,
                    onValueChange = { customRange = it },
                    label = { Text("e.g. 1-3, 5") },
                    singleLine = true,
                    isError = !rangeValid,
                    supportingText = {
                        if (!rangeValid) {
                            Text("Enter pages like 1-3, 5 (this note has $pageCount)")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Quality",
                style = MaterialTheme.typography.titleSmall,
                color = DesignTokens.TextPrimary
            )
            RadioRow(
                label = "Standard (150 dpi)",
                selected = quality == ExportQuality.STANDARD,
                onClick = { quality = ExportQuality.STANDARD }
            )
            RadioRow(
                label = "High (300 dpi)",
                selected = quality == ExportQuality.HIGH,
                onClick = { quality = ExportQuality.HIGH }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("File name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onExport(
                            ExportOptions(
                                includeAnnotations = includeAnnotations,
                                pageChoice = pageChoice,
                                customRange = customRange,
                                quality = quality,
                                fileName = fileName.trim()
                            )
                        )
                    },
                    enabled = canExport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignTokens.Accent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) { Text("Export") }
            }
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = DesignTokens.TextPrimary
        )
    }
}

/** Progress while exporting, with a working Cancel. */
@Composable
fun ExportProgressDialog(done: Int, total: Int, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* must use Cancel */ },
        title = { Text("Exporting PDF") },
        text = {
            Column {
                Text("Exporting page $done of $total")
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) done.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

/** Shown when the export finished: Share / Open / Save to device. */
@Composable
fun ExportDoneDialog(
    fileName: String,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export ready") },
        text = { Text("\"$fileName\" was created.") },
        confirmButton = {
            TextButton(onClick = onShare) { Text("Share") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onOpen) { Text("Open") }
                TextButton(onClick = onSaveToDevice) { Text("Save to device") }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    )
}
