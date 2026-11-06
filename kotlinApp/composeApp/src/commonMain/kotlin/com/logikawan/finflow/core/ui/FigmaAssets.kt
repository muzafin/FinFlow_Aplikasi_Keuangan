package com.logikawan.finflow.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private data class DesignAsset(val resource: DrawableResource, val width: Float, val height: Float)
private val designAssets = mapOf(
    "fig_3_25_illustration_image" to DesignAsset(Res.drawable.fig_3_25_illustration_image, 1152.0f, 928.0f),
    "fig_3_25_arrow_right" to DesignAsset(Res.drawable.fig_3_25_arrow_right, 16.0f, 16.0f),
    "fig_3_52_illustration_image" to DesignAsset(Res.drawable.fig_3_52_illustration_image, 1152.0f, 928.0f),
    "fig_3_79_illustration_image" to DesignAsset(Res.drawable.fig_3_79_illustration_image, 1152.0f, 928.0f),
    "fig_3_106_illustration_image" to DesignAsset(Res.drawable.fig_3_106_illustration_image, 1152.0f, 928.0f),
    "fig_117_347_untitled_design1" to DesignAsset(Res.drawable.fig_117_347_untitled_design1, 1024.0f, 1024.0f),
    "fig_117_354_fino_peeking_over_the_sill1" to DesignAsset(Res.drawable.fig_117_354_fino_peeking_over_the_sill1, 1254.0f, 1254.0f),
    "fig_117_354_logo_icon" to DesignAsset(Res.drawable.fig_117_354_logo_icon, 1024.0f, 1024.0f),
    "fig_117_354_ekor_gelembung_menuju_anak_kecil" to DesignAsset(Res.drawable.fig_117_354_ekor_gelembung_menuju_anak_kecil, 14.0f, 25.0f),
    "fig_117_354_group" to DesignAsset(Res.drawable.fig_117_354_group, 20.0f, 16.0f),
    "fig_117_354_google" to DesignAsset(Res.drawable.fig_117_354_google, 24.0f, 24.0f),
    "fig_171_310_logo_fin_flow" to DesignAsset(Res.drawable.fig_171_310_logo_fin_flow, 1024.0f, 1024.0f),
    "fig_171_310_arrow_left" to DesignAsset(Res.drawable.fig_171_310_arrow_left, 20.0f, 20.0f),
    "fig_171_310_tampilkan_password" to DesignAsset(Res.drawable.fig_171_310_tampilkan_password, 20.0f, 20.0f),
    "fig_15_4_arrow_down_left" to DesignAsset(Res.drawable.fig_15_4_arrow_down_left, 16.0f, 16.0f),
    "fig_15_4_arrow_up_right" to DesignAsset(Res.drawable.fig_15_4_arrow_up_right, 16.0f, 16.0f),
    "fig_15_4_more_horizontal" to DesignAsset(Res.drawable.fig_15_4_more_horizontal, 20.0f, 20.0f),
    "fig_15_4_pie_chart" to DesignAsset(Res.drawable.fig_15_4_pie_chart, 22.0f, 22.0f),
    "fig_15_4_rotate_cw" to DesignAsset(Res.drawable.fig_15_4_rotate_cw, 22.0f, 22.0f),
    "fig_15_4_piggy_bank" to DesignAsset(Res.drawable.fig_15_4_piggy_bank, 22.0f, 22.0f),
    "fig_15_4_receipt" to DesignAsset(Res.drawable.fig_15_4_receipt, 22.0f, 22.0f),
    "fig_15_4_scale" to DesignAsset(Res.drawable.fig_15_4_scale, 22.0f, 22.0f),
    "fig_15_4_heart" to DesignAsset(Res.drawable.fig_15_4_heart, 22.0f, 22.0f),
    "fig_15_4_credit_card" to DesignAsset(Res.drawable.fig_15_4_credit_card, 22.0f, 22.0f),
    "fig_15_4_book_open" to DesignAsset(Res.drawable.fig_15_4_book_open, 22.0f, 22.0f),
    "fig_15_4_coins" to DesignAsset(Res.drawable.fig_15_4_coins, 22.0f, 22.0f),
    "fig_15_4_folder_kanban" to DesignAsset(Res.drawable.fig_15_4_folder_kanban, 22.0f, 22.0f),
    "fig_15_4_plus_circle" to DesignAsset(Res.drawable.fig_15_4_plus_circle, 20.0f, 20.0f),
    "fig_15_4_wallet" to DesignAsset(Res.drawable.fig_15_4_wallet, 18.0f, 18.0f),
    "fig_15_4_home" to DesignAsset(Res.drawable.fig_15_4_home, 22.0f, 22.0f),
    "fig_15_4_bar_chart2" to DesignAsset(Res.drawable.fig_15_4_bar_chart2, 22.0f, 22.0f),
    "fig_15_4_wallet1" to DesignAsset(Res.drawable.fig_15_4_wallet1, 22.0f, 22.0f),
    "fig_15_4_user" to DesignAsset(Res.drawable.fig_15_4_user, 22.0f, 22.0f),
    "fig_15_4_plus" to DesignAsset(Res.drawable.fig_15_4_plus, 24.0f, 24.0f),
    "fig_86_6_utensils" to DesignAsset(Res.drawable.fig_86_6_utensils, 20.0f, 20.0f),
    "fig_86_6_car" to DesignAsset(Res.drawable.fig_86_6_car, 20.0f, 20.0f),
    "fig_86_6_shopping_bag" to DesignAsset(Res.drawable.fig_86_6_shopping_bag, 20.0f, 20.0f),
    "fig_86_6_clapperboard" to DesignAsset(Res.drawable.fig_86_6_clapperboard, 20.0f, 20.0f),
    "fig_86_6_heart_pulse" to DesignAsset(Res.drawable.fig_86_6_heart_pulse, 20.0f, 20.0f),
    "fig_86_6_graduation_cap" to DesignAsset(Res.drawable.fig_86_6_graduation_cap, 20.0f, 20.0f),
    "fig_86_6_text_cursor" to DesignAsset(Res.drawable.fig_86_6_text_cursor, 18.0f, 18.0f),
    "fig_86_6_pencil" to DesignAsset(Res.drawable.fig_86_6_pencil, 18.0f, 18.0f),
    "fig_86_6_delete" to DesignAsset(Res.drawable.fig_86_6_delete, 20.0f, 20.0f),
    "fig_86_6_circle_x" to DesignAsset(Res.drawable.fig_86_6_circle_x, 18.0f, 18.0f),
    "fig_86_6_check" to DesignAsset(Res.drawable.fig_86_6_check, 24.0f, 24.0f),
    "fig_86_157_dollar_sign" to DesignAsset(Res.drawable.fig_86_157_dollar_sign, 20.0f, 20.0f),
    "fig_86_157_store" to DesignAsset(Res.drawable.fig_86_157_store, 20.0f, 20.0f),
    "fig_86_157_trending_up" to DesignAsset(Res.drawable.fig_86_157_trending_up, 20.0f, 20.0f),
    "fig_149_2_container" to DesignAsset(Res.drawable.fig_149_2_container, 24.0f, 24.0f),
    "fig_149_2_container1" to DesignAsset(Res.drawable.fig_149_2_container1, 12.0f, 12.0f),
    "fig_149_2_container2" to DesignAsset(Res.drawable.fig_149_2_container2, 20.0f, 19.0f),
    "fig_149_2_container3" to DesignAsset(Res.drawable.fig_149_2_container3, 7.4f, 12.0f),
    "fig_149_2_container4" to DesignAsset(Res.drawable.fig_149_2_container4, 20.0f, 16.0f),
    "fig_149_2_container5" to DesignAsset(Res.drawable.fig_149_2_container5, 7.4f, 12.0f),
    "fig_149_2_container6" to DesignAsset(Res.drawable.fig_149_2_container6, 16.0f, 16.0f),
    "fig_149_2_container7" to DesignAsset(Res.drawable.fig_149_2_container7, 16.0f, 16.0f),
    "fig_149_2_container8" to DesignAsset(Res.drawable.fig_149_2_container8, 18.0f, 20.0f),
    "fig_149_2_container9" to DesignAsset(Res.drawable.fig_149_2_container9, 19.0f, 20.0f),
    "fig_149_2_container10" to DesignAsset(Res.drawable.fig_149_2_container10, 19.0f, 18.0f),
    "fig_149_2_container11" to DesignAsset(Res.drawable.fig_149_2_container11, 16.0f, 18.0f),
    "fig_149_2_container12" to DesignAsset(Res.drawable.fig_149_2_container12, 20.0f, 21.0f),
    "fig_149_2_container13" to DesignAsset(Res.drawable.fig_149_2_container13, 16.0f, 16.0f),
    "fig_149_2_container14" to DesignAsset(Res.drawable.fig_149_2_container14, 17.5f, 17.5f),
    "fig_149_116_container" to DesignAsset(Res.drawable.fig_149_116_container, 16.9565f, 18.55f),
    "fig_149_116_container1" to DesignAsset(Res.drawable.fig_149_116_container1, 15.0f, 15.0f),
    "fig_149_116_container3" to DesignAsset(Res.drawable.fig_149_116_container3, 22.6146f, 22.6146f),
    "fig_92_6_layout_grid" to DesignAsset(Res.drawable.fig_92_6_layout_grid, 20.0f, 20.0f),
    "fig_92_6_plus" to DesignAsset(Res.drawable.fig_92_6_plus, 20.0f, 20.0f),
    "fig_92_6_arrow_left_right" to DesignAsset(Res.drawable.fig_92_6_arrow_left_right, 12.0f, 12.0f),
    "fig_92_6_wallet" to DesignAsset(Res.drawable.fig_92_6_wallet, 20.0f, 20.0f),
    "fig_92_6_building" to DesignAsset(Res.drawable.fig_92_6_building, 20.0f, 20.0f),
    "fig_92_6_credit_card" to DesignAsset(Res.drawable.fig_92_6_credit_card, 20.0f, 20.0f),
    "fig_27_7_chevron_left" to DesignAsset(Res.drawable.fig_27_7_chevron_left, 16.0f, 16.0f),
    "fig_27_7_chevron_left2" to DesignAsset(Res.drawable.fig_27_7_chevron_left2, 16.0f, 16.0f),
    "fig_27_7_chevron_down" to DesignAsset(Res.drawable.fig_27_7_chevron_down, 14.0f, 14.0f),
    "fig_49_8_piggy_bank" to DesignAsset(Res.drawable.fig_49_8_piggy_bank, 20.0f, 20.0f),
    "fig_49_8_laptop" to DesignAsset(Res.drawable.fig_49_8_laptop, 18.0f, 18.0f),
    "fig_49_96_laptop" to DesignAsset(Res.drawable.fig_49_96_laptop, 22.0f, 22.0f),
    "fig_49_96_edit2" to DesignAsset(Res.drawable.fig_49_96_edit2, 16.0f, 16.0f),
    "fig_49_96_sparkles" to DesignAsset(Res.drawable.fig_49_96_sparkles, 16.0f, 16.0f),
    "fig_49_96_settings" to DesignAsset(Res.drawable.fig_49_96_settings, 20.0f, 20.0f),
    "fig_49_96_plus_circle" to DesignAsset(Res.drawable.fig_49_96_plus_circle, 16.0f, 16.0f),
    "fig_80_2_grid1" to DesignAsset(Res.drawable.fig_80_2_grid1, 620.0f, 2.0f),
)

@Composable
fun FigmaAsset(name: String, modifier: Modifier = Modifier, description: String? = null, crop: Boolean = false, tint: Color? = null) {
    val asset = designAssets.getValue(name)
    Image(painterResource(asset.resource), description,
        modifier.then(if (modifier == Modifier) Modifier.size(asset.width.dp, asset.height.dp) else Modifier),
        contentScale = if (crop) ContentScale.Crop else ContentScale.Fit, colorFilter = tint?.let { ColorFilter.tint(it) })
}
