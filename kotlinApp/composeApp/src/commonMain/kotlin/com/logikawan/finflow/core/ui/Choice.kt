package com.logikawan.finflow.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Choice(label: String, value: String, options: List<Pair<String, String>>, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(expanded) { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = options.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = FFColors.surface, contentColor = FFColors.onSurface), border = BorderStroke(1.dp, FFColors.outlineVariant)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(options.find { it.first == value }?.second ?: "Pilih…", Modifier.weight(1f), style = FFType.bodyMain, fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
                    FFIcon("expand_more_rounded", size = 20.dp)
                }
            }
        }
    }
    if(expanded) ModalBottomSheet(onDismissRequest={expanded=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=FFColors.canvas) {
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),style=FFType.sectionTitle);FFIconButton("close",{expanded=false})}
            if(options.size>6)Field("Cari $label",query,{query=it})
            LazyColumn(Modifier.heightIn(max=460.dp),contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                val matches=options.filter{it.second.contains(query,true)}
                if(matches.isEmpty())item{Text("Tidak ditemukan",style=FFType.bodyMain)}
                items(matches){(key,text)->
                    Surface(onClick={change(key);expanded=false},shape=RoundedCornerShape(16.dp),color=if(key==value)FFColors.primaryContainer else FFColors.surface,
                        border=BorderStroke(1.dp,if(key==value)FFColors.primary else FFColors.outlineVariant)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(text,Modifier.weight(1f),style=FFType.bodyMain);if(key==value)FFIcon("check")}
                    }
                }
            }
        }
    }
}

