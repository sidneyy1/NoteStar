package com.riguel.notestar.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.riguel.notestar.data.PageFormat
import com.riguel.notestar.data.PageFormats
import com.riguel.notestar.data.SizeUnit
import org.json.JSONArray
import org.json.JSONObject

/** A user-saved page size, stored in SharedPreferences as JSON. */
private data class CustomPreset(val name: String, val widthPt: Float, val heightPt: Float)

private fun loadCustomPresets(context: Context): List<CustomPreset> {
    val raw = context.getSharedPreferences("page_sizes", Context.MODE_PRIVATE)
        .getString("custom", null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            CustomPreset(
                name = o.getString("name"),
                widthPt = o.getDouble("w").toFloat(),
                heightPt = o.getDouble("h").toFloat()
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveCustomPresets(context: Context, list: List<CustomPreset>) {
    val arr = JSONArray()
    for (p in list) {
        arr.put(
            JSONObject()
                .put("name", p.name)
                .put("w", p.widthPt.toDouble())
                .put("h", p.heightPt.toDouble())
        )
    }
    context.getSharedPreferences("page_sizes", Context.MODE_PRIVATE)
        .edit().putString("custom", arr.toString()).apply()
}

private fun fmt(v: Float): String =
    if (v == v.toInt().toFloat()) {
        v.toInt().toString()
    } else {
        (Math.round(v * 10f) / 10f).toString()
    }

private fun unitLabel(unit: SizeUnit): String = when (unit) {
    SizeUnit.PX -> "px"
    SizeUnit.MM -> "mm"
    SizeUnit.CM -> "cm"
    SizeUnit.IN -> "in"
}

/**
 * Page-size picker for normal notes: built-in presets, portrait/landscape,
 * and a custom editor (width/height in px/mm/cm/in, aspect lock, live
 * preview, save-as-preset). [clipsContent] lets the editor warn when the
 * candidate size would hide existing content.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageSizePickerDialog(
    current: PageFormat,
    clipsContent: (PageFormat) -> Boolean = { false },
    onDismiss: () -> Unit,
    onPick: (PageFormat) -> Unit
) {
    val context = LocalContext.current
    var customs by remember { mutableStateOf(loadCustomPresets(context)) }

    var selW by remember { mutableStateOf(current.widthPt) }
    var selH by remember { mutableStateOf(current.heightPt) }
    var presetId by remember { mutableStateOf(current.presetId) }
    var unit by remember { mutableStateOf(SizeUnit.PX) }
    var widthText by remember {
        mutableStateOf(fmt(PageFormats.fromPt(current.widthPt, SizeUnit.PX)))
    }
    var heightText by remember {
        mutableStateOf(fmt(PageFormats.fromPt(current.heightPt, SizeUnit.PX)))
    }
    var lockAspect by remember { mutableStateOf(false) }
    var aspect by remember { mutableStateOf(current.widthPt / current.heightPt) }
    var editingName by remember { mutableStateOf<String?>(null) }
    var nameText by remember { mutableStateOf("") }
    var showUnitMenu by remember { mutableStateOf(false) }

    fun refreshFields() {
        widthText = fmt(PageFormats.fromPt(selW, unit))
        heightText = fmt(PageFormats.fromPt(selH, unit))
    }

    fun select(w: Float, h: Float, id: String) {
        selW = w
        selH = h
        presetId = id
        aspect = w / h
        refreshFields()
    }

    fun onWidthChanged(text: String) {
        widthText = text
        val v = text.toFloatOrNull() ?: return
        val pt = PageFormats.toPt(v, unit)
        selW = pt
        presetId = "custom"
        if (lockAspect) {
            selH = pt / aspect
            heightText = fmt(PageFormats.fromPt(selH, unit))
        }
    }

    fun onHeightChanged(text: String) {
        heightText = text
        val v = text.toFloatOrNull() ?: return
        val pt = PageFormats.toPt(v, unit)
        selH = pt
        presetId = "custom"
        if (lockAspect) {
            selW = pt * aspect
            widthText = fmt(PageFormats.fromPt(selW, unit))
        }
    }

    val widthPx = PageFormats.toPx(selW)
    val heightPx = PageFormats.toPx(selH)
    val fieldsValid = widthText.toFloatOrNull() != null &&
        heightText.toFloatOrNull() != null
    val rangeValid = widthPx in 100f..5000f && heightPx in 100f..5000f
    val valid = fieldsValid && rangeValid
    val candidate = PageFormat(selW, selH, presetId)
    val clips = valid && clipsContent(candidate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Page size") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Live preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DesignTokens.CanvasBackground, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .aspectRatio(
                                if (selW > 0f && selH > 0f) selW / selH else 1f
                            )
                            .background(Color.White, RoundedCornerShape(2.dp))
                            .border(1.dp, DesignTokens.Divider, RoundedCornerShape(2.dp))
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${PageFormats.label(presetId)} · " +
                        "${fmt(PageFormats.fromPt(selW, SizeUnit.MM))} × " +
                        "${fmt(PageFormats.fromPt(selH, SizeUnit.MM))} mm · " +
                        "${fmt(selW)} × ${fmt(selH)} pt",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignTokens.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (preset in PageFormats.PRESETS) {
                        val selected = presetId == preset.presetId &&
                            selW == preset.widthPt && selH == preset.heightPt ||
                            presetId == preset.presetId &&
                            selW == preset.heightPt && selH == preset.widthPt
                        FilterChip(
                            selected = selected,
                            onClick = {
                                editingName = null
                                nameText = ""
                                select(preset.widthPt, preset.heightPt, preset.presetId)
                            },
                            label = { Text(PageFormats.label(preset.presetId)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DesignTokens.AccentTint,
                                selectedLabelColor = DesignTokens.TextPrimary
                            )
                        )
                    }
                }
                if (customs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    for (preset in customs) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = editingName == preset.name,
                                onClick = {
                                    editingName = preset.name
                                    nameText = preset.name
                                    select(preset.widthPt, preset.heightPt, "custom")
                                },
                                label = {
                                    Text(
                                        "${preset.name} (${fmt(preset.widthPt)} × " +
                                            "${fmt(preset.heightPt)} pt)"
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignTokens.AccentTint,
                                    selectedLabelColor = DesignTokens.TextPrimary
                                )
                            )
                            IconButton(
                                onClick = {
                                    customs = customs.filterNot { it.name == preset.name }
                                    saveCustomPresets(context, customs)
                                    if (editingName == preset.name) editingName = null
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Delete preset ${preset.name}"
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Orientation
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selH >= selW,
                        onClick = { if (selW > selH) select(selH, selW, presetId) },
                        label = { Text("Portrait") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DesignTokens.AccentTint,
                            selectedLabelColor = DesignTokens.TextPrimary
                        )
                    )
                    FilterChip(
                        selected = selW > selH,
                        onClick = { if (selH > selW) select(selH, selW, presetId) },
                        label = { Text("Landscape") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DesignTokens.AccentTint,
                            selectedLabelColor = DesignTokens.TextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Custom size fields
                Text(
                    text = "Custom size",
                    style = MaterialTheme.typography.titleSmall,
                    color = DesignTokens.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { onWidthChanged(it) },
                        label = { Text("Width") },
                        singleLine = true,
                        isError = !valid,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            lockAspect = !lockAspect
                            if (lockAspect && selH > 0f) aspect = selW / selH
                        }
                    ) {
                        Icon(
                            if (lockAspect) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = "Keep aspect ratio",
                            tint = if (lockAspect) {
                                DesignTokens.Accent
                            } else {
                                DesignTokens.IconGray
                            }
                        )
                    }
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { onHeightChanged(it) },
                        label = { Text("Height") },
                        singleLine = true,
                        isError = !valid,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        TextButton(onClick = { showUnitMenu = true }) {
                            Text(unitLabel(unit))
                        }
                        DropdownMenu(
                            expanded = showUnitMenu,
                            onDismissRequest = { showUnitMenu = false }
                        ) {
                            for (u in SizeUnit.entries) {
                                DropdownMenuItem(
                                    text = { Text(unitLabel(u)) },
                                    onClick = {
                                        unit = u
                                        showUnitMenu = false
                                        refreshFields()
                                    }
                                )
                            }
                        }
                    }
                }
                if (!fieldsValid) {
                    Text(
                        text = "Enter a width and a height.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (!rangeValid) {
                    Text(
                        text = "Each side must be between 100 and 5000 px " +
                            "(now ${fmt(widthPx)} × ${fmt(heightPx)} px).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (clips) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Some content is outside the new page and " +
                            "will be hidden. It isn't deleted and comes " +
                            "back if you enlarge the page again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Save as preset
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Preset name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val name = nameText.trim()
                            if (name.isNotEmpty()) {
                                var next = customs.filterNot {
                                    it.name == name || it.name == editingName
                                }
                                next = next + CustomPreset(name, selW, selH)
                                customs = next
                                saveCustomPresets(context, next)
                                editingName = name
                                presetId = "custom"
                            }
                        },
                        enabled = nameText.isNotBlank() && valid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DesignTokens.Accent,
                            contentColor = Color.White
                        )
                    ) { Text("Save") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPick(candidate) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesignTokens.Accent,
                    contentColor = Color.White
                )
            ) { Text("Use this size") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
