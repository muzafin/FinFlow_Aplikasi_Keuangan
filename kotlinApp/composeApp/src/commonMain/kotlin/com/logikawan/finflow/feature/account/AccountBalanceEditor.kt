package com.logikawan.finflow.feature.account

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*

@Composable
fun AccountBalanceEditor(account:Account,data:FinanceData,busy:Boolean,dismiss:()->Unit,save:(Transaction)->Unit) {
    val id=remember{newId()}
    val current=data.balance(account)
    var value by remember{mutableStateOf(decimalAmount(current,account.currency))}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog("Sesuaikan Saldo",busy,dismiss,save={try{
        val desired=minorAmount(value,account.currency,allowNegative=true)
        require(desired in -FinanceRepository.MAX_MONEY..FinanceRepository.MAX_MONEY){"Saldo di luar batas"}
        val delta=checkedAdd(desired,-current)
        require(delta!=0L){"Saldo sudah sesuai"}
        val tx=Transaction(id=id,type=if(delta>0)"income" else "expense",amountMinor=kotlin.math.abs(delta),currency=account.currency,accountId=account.id,
            categoryId=if(delta>0)"other_income" else "other_expense",title="Penyesuaian saldo: ${account.name}")
        FinanceRepository.validateTransaction(tx);save(tx)
    }catch(e:Exception){error=e.message}},error=error){
        Text(account.name,style=FFType.sectionTitle)
        Text("Saldo tercatat: ${displayPreciseMoney(current,account.currency)}",style=FFType.bodyMain)
        Field("Saldo yang sesuai (${account.currency})",value,{value=it},enabled=!busy)
        Text("Selisih dibuat sebagai transaksi penyesuaian. Riwayat transaksi dan saldo awal tetap dapat ditelusuri.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}
