package com.clonical.kasiva.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.util.minorAmount

@Composable
fun LocalSetupScreen(busy:Boolean,back:()->Unit,complete:(String,String,Long)->Unit) = EntryTheme {
    var step by rememberSaveable{mutableIntStateOf(0)}
    var currency by rememberSaveable{mutableStateOf("IDR")}
    var wallet by rememberSaveable{mutableStateOf("Kas")}
    var balance by rememberSaveable{mutableStateOf("0")}
    var all by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().background(FFColors.background).safeDrawingPadding().imePadding().padding(24.dp)) {
        TextButton(onClick={if(step==0)back()else step--},enabled=!busy){Text(MR.strings.common_back.text())}
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(32.dp))
            Text(if(step==0)MR.strings.auth_currency_title.text() else MR.strings.auth_local_wallet_title.text(),style=FFType.appTitle.copy(fontSize=30.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold))
            Spacer(Modifier.height(10.dp))
            Text(if(step==0)MR.strings.auth_currency_subtitle.text() else MR.strings.auth_local_wallet_subtitle.text(),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            if(step==0) {
                listOf("USD" to "US Dollar","EUR" to "Euro","IDR" to "Indonesian Rupiah","MYR" to "Malaysian Ringgit","JPY" to "Japanese Yen","CNY" to "Chinese Yuan").chunked(2).forEach{row->
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { row.forEach{(code,label)->
                        Surface(onClick={currency=code},modifier=Modifier.weight(1f),shape=RoundedCornerShape(20.dp),
                            color=if(code==currency)entryAccent.copy(alpha=.1f)else FFColors.surface,border=androidx.compose.foundation.BorderStroke(1.dp,if(code==currency)entryAccent else FFColors.outlineVariant)) {
                            Column(Modifier.padding(18.dp)){Text(code,style=FFType.sectionTitle,color=if(code==currency)entryAccent else FFColors.onSurface);Text(label,style=FFType.metaData,color=FFColors.onSurfaceVariant)}
                        }
                    }}
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedButton(onClick={all=true},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(MR.strings.auth_view_all.text())}
                Spacer(Modifier.height(16.dp));Text(MR.strings.auth_local_currency_note.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
            } else {
                EntryField(MR.strings.auth_wallet_name.text(),MR.strings.auth_wallet_name_example.text(),wallet,{wallet=it},busy,tag="Nama dompet")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(balance,{balance=it},label={Text(MR.strings.auth_current_balance.text(currency))},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                Spacer(Modifier.height(16.dp));Text(MR.strings.auth_local_data_note.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
            }
            error?.let{Spacer(Modifier.height(12.dp));Text(it,color=MaterialTheme.colorScheme.error)}
        }
        EntryButton(if(step==0)MR.strings.common_next.text() else MR.strings.auth_start_now.text(),busy){
            if(step==0)step=1 else try{require(wallet.isNotBlank() && wallet.trim().length<=100){MR.strings.auth_error_wallet_name.text()};complete(currency,wallet.trim(),minorAmount(balance,currency))}catch(e:Exception){error=e.message}
        }
    }
    if(all)AlertDialog(onDismissRequest={all=false},title={Text(MR.strings.auth_currency_dialog_title.text())},text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())){
        listOf("AED","AUD","BDT","BHD","BRL","CAD","CHF","CNY","CZK","DKK","EGP","EUR","GBP","HKD","HUF","IDR","ILS","INR","ISK","JPY","KRW","KWD","LKR","MAD","MXN","MYR","NOK","NZD","OMR","PHP","PKR","PLN","QAR","RON","RUB","SAR","SEK","SGD","THB","TRY","TWD","UAH","USD","VND","ZAR").forEach{code->TextButton(onClick={currency=code;all=false},modifier=Modifier.fillMaxWidth()){Text(code)}}
    }},confirmButton={TextButton(onClick={all=false}){Text(MR.strings.common_close.text())}})
}
