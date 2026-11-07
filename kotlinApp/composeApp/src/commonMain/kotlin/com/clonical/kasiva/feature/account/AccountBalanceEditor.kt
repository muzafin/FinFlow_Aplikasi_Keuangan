package com.clonical.kasiva.feature.account

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*

@Composable
fun AccountBalanceEditor(account:Account,data:FinanceData,busy:Boolean,dismiss:()->Unit,save:(Transaction)->Unit) {
    val id=remember{newId()}
    val current=data.balance(account)
    var value by remember{mutableStateOf(decimalAmount(current,account.currency))}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(MR.strings.acc_balance_adjust_title.text(),busy,dismiss,save={try{
        val desired=minorAmount(value,account.currency,allowNegative=true)
        require(desired in -FinanceRepository.MAX_MONEY..FinanceRepository.MAX_MONEY){MR.strings.acc_err_balance_out_of_range.text()}
        val delta=checkedAdd(desired,-current)
        require(delta!=0L){MR.strings.acc_err_balance_already_matches.text()}
        val tx=Transaction(id=id,type=if(delta>0)"income" else "expense",amountMinor=kotlin.math.abs(delta),currency=account.currency,accountId=account.id,
            categoryId=if(delta>0)"other_income" else "other_expense",title=MR.strings.acc_balance_adjust_tx_title.text(account.name))
        FinanceRepository.validateTransaction(tx);save(tx)
    }catch(e:Exception){error=e.message}},error=error){
        Text(account.name,style=FFType.sectionTitle)
        Text(MR.strings.acc_balance_recorded.text(displayPreciseMoney(current,account.currency)),style=FFType.bodyMain)
        Field(MR.strings.acc_balance_correct_label.text(account.currency),value,{value=it},enabled=!busy)
        Text(MR.strings.acc_balance_adjust_note.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}
