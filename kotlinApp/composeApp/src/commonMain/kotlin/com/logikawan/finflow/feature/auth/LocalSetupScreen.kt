package com.logikawan.finflow.feature.auth

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
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.minorAmount

@Composable
fun LocalSetupScreen(busy:Boolean,back:()->Unit,complete:(String,String,Long)->Unit) = EntryTheme {
    var step by rememberSaveable{mutableIntStateOf(0)}
    var currency by rememberSaveable{mutableStateOf("IDR")}
    var wallet by rememberSaveable{mutableStateOf("Kas")}
    var balance by rememberSaveable{mutableStateOf("0")}
    var all by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().background(FFColors.background).safeDrawingPadding().imePadding().padding(24.dp)) {
        TextButton(onClick={if(step==0)back()else step--},enabled=!busy){Text("Kembali")}
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(32.dp))
            Text(if(step==0)"Pilih Mata Uang" else "Dompet Pertamamu",style=FFType.appTitle.copy(fontSize=30.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold))
            Spacer(Modifier.height(10.dp))
            Text(if(step==0)"Pilih mata uang untuk mencatat pengeluaranmu." else "Beri nama dompet dan isi saldo saat ini.",style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
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
                OutlinedButton(onClick={all=true},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Lihat Semua")}
                Spacer(Modifier.height(16.dp));Text("Pengaturan ini dapat diubah kapan saja melalui Profil.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
            } else {
                EntryField("Nama dompet","Contoh: Kas",wallet,{wallet=it},busy)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(balance,{balance=it},label={Text("Saldo saat ini ($currency)")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                Spacer(Modifier.height(16.dp));Text("Data disimpan di perangkat ini. Tanpa akun dan tanpa koneksi bank. Buat cadangan dari Profil untuk menyimpannya di tempat lain.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
            }
            error?.let{Spacer(Modifier.height(12.dp));Text(it,color=MaterialTheme.colorScheme.error)}
        }
        EntryButton(if(step==0)"Lanjut" else "Mulai Sekarang",busy){
            if(step==0)step=1 else try{require(wallet.isNotBlank() && wallet.trim().length<=100){"Nama dompet wajib diisi, maksimal 100 karakter"};complete(currency,wallet.trim(),minorAmount(balance,currency))}catch(e:Exception){error=e.message}
        }
    }
    if(all)AlertDialog(onDismissRequest={all=false},title={Text("Mata Uang")},text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())){
        listOf("AED","AUD","BDT","BHD","BRL","CAD","CHF","CNY","CZK","DKK","EGP","EUR","GBP","HKD","HUF","IDR","ILS","INR","ISK","JPY","KRW","KWD","LKR","MAD","MXN","MYR","NOK","NZD","OMR","PHP","PKR","PLN","QAR","RON","RUB","SAR","SEK","SGD","THB","TRY","TWD","UAH","USD","VND","ZAR").forEach{code->TextButton(onClick={currency=code;all=false},modifier=Modifier.fillMaxWidth()){Text(code)}}
    }},confirmButton={TextButton(onClick={all=false}){Text("Tutup")}})
}
