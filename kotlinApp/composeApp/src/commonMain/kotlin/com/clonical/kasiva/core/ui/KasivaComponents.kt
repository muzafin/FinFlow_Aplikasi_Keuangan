package com.clonical.kasiva.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.resources.*
import org.jetbrains.compose.resources.Font
import com.clonical.kasiva.core.model.LocalPreferences
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text

@Composable
fun FFIcon(name: String, tint: Color = FFColors.onSurfaceVariant, size: Dp = 24.dp, contentDescription: String? = null) {
    if (name.startsWith("hi_")) { HugeIcon(name, tint, size, contentDescription); return }
    val rounded = LocalPreferences.current.iconPack == "rounded"
    val figmaAsset = figmaIconNames[name]
    @Composable fun drawAsset(asset: String) {
        Box(Modifier.size(size).then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier.clearAndSetSemantics {}), contentAlignment = Alignment.Center) {
            Box(Modifier.then(if (name == "chevron_right_rounded") Modifier.rotate(177.94f) else Modifier)) { FigmaAsset(asset, tint = tint) }
        }
    }
    if (!rounded && figmaAsset != null) { drawAsset(figmaAsset); return }
    val base = name.removeSuffix("_outlined").removeSuffix("_rounded")
    val code = flutterIconCodes[base + if(rounded) "_rounded" else "_outlined"] ?: flutterIconCodes[name]
    if (code == null && figmaAsset != null) { drawAsset(figmaAsset); return }
    val resolved = code ?: flutterIconCodes.getValue("category")
    val glyph = if (resolved <= 0xFFFF) resolved.toChar().toString() else {
        val point = resolved - 0x10000
        "${(0xD800 + (point shr 10)).toChar()}${(0xDC00 + (point and 0x3FF)).toChar()}"
    }
    Box(Modifier.size(size).then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier.clearAndSetSemantics {}), contentAlignment = Alignment.Center) {
        Text(glyph, color = tint, style = TextStyle(fontFamily = FontFamily(Font(Res.font.material_icons)), fontSize = size.value.sp, lineHeight = size.value.sp))
    }
}

@Composable
fun FFIconButton(name: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = FFColors.onSurfaceVariant) {
    IconButton(onClick, modifier) { FFIcon(name, tint, contentDescription = when (name) {
        "add_rounded" -> MR.strings.common_add.text(); "close_rounded" -> MR.strings.common_close.text(); "delete_outline_rounded" -> MR.strings.common_delete.text(); "chevron_left_rounded" -> MR.strings.app_previous.text(); "chevron_right_rounded" -> MR.strings.app_next.text(); "hi_chevron_left" -> MR.strings.common_back.text(); "hi_chevron_right" -> MR.strings.app_next.text(); "file_download_outlined" -> MR.strings.app_export_csv.text(); else -> name.replace('_', ' ')
    }) }
}

fun categoryIcon(id: String): String = when {
    id == "food-breakfast" -> "free_breakfast"
    id == "food-lunch" -> "lunch_dining"
    id == "food-dinner" -> "dinner_dining"
    id == "food-eat-out" -> "restaurant_menu"
    id == "transport-fuel" -> "local_gas_station"
    id == "transport-public" -> "directions_bus"
    id == "transport-parking" -> "local_parking"
    id == "transport-ride" -> "local_taxi"
    id == "shopping-groceries" -> "shopping_basket"
    id == "shopping-clothing" -> "checkroom"
    id == "shopping-electronics" -> "devices"
    id == "health-medicine" -> "medication"
    id == "health-doctor" -> "medical_services"
    id == "health-sport" -> "fitness_center"
    id == "bills-electricity" -> "bolt"
    id == "bills-water" -> "water_drop"
    id == "bills-internet" -> "wifi"
    id == "bills-rent" -> "home"
    id == "education-books" -> "menu_book"
    id == "education-course" -> "workspace_premium"
    id == "education-stationery" -> "edit"
    id == "entertainment-streaming" -> "live_tv"
    id == "entertainment-cinema" -> "theaters"
    id == "entertainment-games" -> "sports_esports"
    id == "entertainment-hobby" -> "palette"
    listOf("food", "makan").any { id.contains(it, true) } -> "restaurant"
    id.contains("transport", true) -> "directions_bus"
    listOf("shopping", "belanja").any { id.contains(it, true) } -> "local_mall"
    listOf("health", "kesehatan").any { id.contains(it, true) } -> "local_hospital"
    listOf("entertainment", "hiburan").any { id.contains(it, true) } -> "movie"
    listOf("education", "pendidikan").any { id.contains(it, true) } -> "school"
    listOf("bills", "tagihan").any { id.contains(it, true) } -> "receipt_long"
    listOf("salary", "gaji").any { id.contains(it, true) } -> "attach_money"
    listOf("business", "usaha", "bisnis").any { id.contains(it, true) } -> "store"
    id == "transfer" -> "swap_horiz"
    else -> "category"
}
@Composable
fun categoryColor(id: String): Color = when (categoryIcon(id)) {
    "restaurant" -> Color(0xFFFF6B00); "directions_bus" -> Color(0xFF8B5CF6); "local_mall" -> Color(0xFF3B82F6)
    "local_hospital" -> Color(0xFFE74C3C); "movie" -> Color(0xFF8E44AD); "school" -> Color(0xFF2980B9)
    "receipt_long" -> Color(0xFFBA1A1A); "swap_horiz" -> FFColors.secondary; else -> FFColors.primary
}
