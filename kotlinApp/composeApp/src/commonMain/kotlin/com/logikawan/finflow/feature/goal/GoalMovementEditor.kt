package com.logikawan.finflow.feature.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import com.logikawan.finflow.feature.planning.OlloFormSection
import com.logikawan.finflow.feature.planning.PaymentRequest
import com.logikawan.finflow.feature.planning.PlanDateField

@Composable
fun GoalMovementEditor(goal:Goal,data:FinanceData,withdraw:Boolean,busy:Boolean,dismiss:()->Unit,save:(PaymentRequest)->Unit) {
    val request=remember{newId()}
    val wallets=data.accounts.filter{!it.archived && it.type!="savings" && it.currency==goal.currency}
    val available=if(goal.holdingAccountId==null)0 else (goal.savedMinor-goal.legacySavedMinor).coerceAtLeast(0)
    val percentageBase=if(withdraw)available else (goal.targetMinor-goal.savedMinor).coerceAtLeast(0)
    var amount by remember{mutableStateOf("")}
    var account by remember{mutableStateOf(wallets.firstOrNull()?.id.orEmpty())}
    var date by remember{mutableStateOf(today().toString())}
    var error by remember{mutableStateOf<String?>(null)}
    var confirmStage by remember{mutableIntStateOf(0)}
    var pending by remember{mutableStateOf<PaymentRequest?>(null)}
    val preferences=LocalPreferences.current
    val selectedWallet=wallets.find{it.id==account}
    EditorDialog(if(withdraw)"Tarik Dana" else "Setor Dana",busy,dismiss,save={try{
        val value=minorAmount(amount,goal.currency)
        require(value in 1..FinanceRepository.MAX_MONEY){"Isi nominal positif"}
        require(date<=today().toString()){"Tanggal tidak boleh di masa depan"}
        val wallet=wallets.find{it.id==account} ?: error("Pilih dompet")
        if(withdraw)require(value<=available){"Dana setoran dompet tidak mencukupi"}
        else require(value<=data.balance(wallet)){"Saldo dompet sumber tidak cukup"}
        val payment=PaymentRequest(value,account,date,request)
        if(withdraw && goal.protection!="flexible"){pending=payment;confirmStage=1} else save(payment)
    }catch(e:Exception){error=e.message}},error=error){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(GoalAccent.copy(alpha=.12f)),contentAlignment=Alignment.Center){FFIcon(bucketIcon(goal.bucketType),GoalAccent,22.dp)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.name,style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=22.sp),fontWeight=FontWeight.SemiBold,color=FFColors.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(if(withdraw)"Penarikan dana target" else "Setoran ke target",style=FFType.metaData,color=FFColors.onSurfaceVariant)
            }
        }
        OlloFormSection(if(withdraw)"Jumlah Penarikan" else "Jumlah Setoran")
        Field("Nominal (${goal.currency})",amount,{amount=it},enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
        Text(if(withdraw)"Tersedia: ${goalMoney(available,goal.currency,preferences)}" else "Sisa target: ${goalMoney(percentageBase,goal.currency,preferences)}",style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
        OlloPercentRow(if(withdraw)listOf(25,50,100) else listOf(5,10,25,50),percentageBase,goal.currency,!busy){amount=it}
        WalletChoice(if(withdraw)"Ke Dompet" else "Dari Dompet",account,data,wallets,!busy){account=it}
        selectedWallet?.let{Text("Saldo ${goalMoney(data.balance(it),it.currency,preferences)}",style=FFType.metaData,color=FFColors.onSurfaceVariant)}
        PlanDateField("Tanggal",date,{date=it},!busy)
        Text("Perpindahan dana disimpan sebagai transfer internal. Total saldo tidak berubah dan statistik pemasukan/pengeluaran tidak bertambah.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
    if(confirmStage>0)AlertDialog(onDismissRequest={confirmStage=0},title={Text(if(confirmStage==1)"Tarik dana target?" else "Konfirmasi terakhir")},text={Text(if(confirmStage==1)"Dana ini disisihkan untuk ${goal.name}. Lanjutkan penarikan?" else "${goalMoney(pending?.amountMinor ?: 0,goal.currency,preferences)} akan dipindahkan ke dompet pilihan Anda.")},confirmButton={TextButton(onClick={if(goal.protection=="hard" && confirmStage==1)confirmStage=2 else {confirmStage=0;pending?.let(save)}}){Text(if(goal.protection=="hard" && confirmStage==1)"Lanjutkan" else "Ya, Tarik Dana")}},dismissButton={TextButton(onClick={confirmStage=0}){Text("Batal")}})
}

@Composable
private fun OlloPercentRow(percentages:List<Int>,base:Long,currency:String,enabled:Boolean,select:(String)->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        percentages.forEach{percentage->
            Surface(onClick={select(decimalAmount(base*percentage/100,currency))},enabled=enabled,modifier=Modifier.weight(1f).height(40.dp),shape=RoundedCornerShape(100.dp),color=FFColors.surface) {
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("$percentage%",style=FFType.bodyMain,color=FFColors.onSurface)}
            }
        }
    }
}
