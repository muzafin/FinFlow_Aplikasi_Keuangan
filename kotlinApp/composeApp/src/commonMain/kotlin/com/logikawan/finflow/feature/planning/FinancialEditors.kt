package com.logikawan.finflow.feature.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import kotlinx.datetime.LocalDate

private val decimalKeyboard=KeyboardOptions(keyboardType=KeyboardType.Decimal)
private val frequencies=listOf("daily" to "Harian","weekly" to "Mingguan","monthly" to "Bulanan","yearly" to "Tahunan")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDateField(label:String,value:String,change:(String)->Unit,enabled:Boolean=true) {
    var open by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label,style=FFType.metaData,fontWeight=FontWeight.SemiBold,color=FFColors.onSurfaceVariant)
        OutlinedButton(onClick={open=true},enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(16.dp),
            colors=ButtonDefaults.outlinedButtonColors(containerColor=FFColors.surface,contentColor=FFColors.onSurface)) {
            Text(shortDateLabel(value),Modifier.weight(1f),style=FFType.bodyMain);FFIcon("expand_more_rounded",size=18.dp)
        }
    }
    if(open) {
        val state=rememberDatePickerState(initialSelectedDateMillis=runCatching{LocalDate.parse(value).toEpochDays().toLong()*86400000L}.getOrNull())
        DatePickerDialog(onDismissRequest={open=false},confirmButton={TextButton(onClick={
            state.selectedDateMillis?.let{change(LocalDate.fromEpochDays((it/86400000L).toInt()).toString())};open=false
        }){Text("Pilih")}},dismissButton={TextButton(onClick={open=false}){Text("Batal")}}) { DatePicker(state) }
    }
}

@Composable
fun BillEditor(old:Bill?,data:FinanceData,busy:Boolean,dismiss:()->Unit,save:(Bill)->Unit,archive:(()->Unit)?) {
    val id=remember{old?.id ?: newId()}
    var title by remember{mutableStateOf(old?.title.orEmpty())}
    var accountId by remember{mutableStateOf(old?.accountId ?: data.accounts.firstOrNull{!it.archived && it.type!="savings"}?.id.orEmpty())}
    val account=data.accounts.find{it.id==accountId}
    val currency=account?.currency ?: old?.currency ?: "IDR"
    var amount by remember{mutableStateOf(old?.let{decimalAmount(it.amountMinor,it.currency)}.orEmpty())}
    var category by remember{mutableStateOf(old?.categoryId ?: "bills")}
    var date by remember{mutableStateOf(old?.dueDate ?: today().toString())}
    var frequency by remember{mutableStateOf(old?.frequency ?: "once")}
    var note by remember{mutableStateOf(old?.note.orEmpty())}
    var error by remember{mutableStateOf<String?>(null)}
    val paid=old?.paidAt?.let{it>0} ?: false
    var archived by remember{mutableStateOf(old?.archived ?: false)}
    EditorDialog(if(old==null)"Tambah Tagihan" else "Edit Tagihan",busy,dismiss,save={
        if(!busy)try {
            val value=Bill(id,title.trim(),minorAmount(amount,currency),currency,accountId,category,date,frequency,LocalDate.parse(date).dayOfMonth,note.trim(),
                old?.paidAt ?: 0,old?.lastPaymentId,archived,old?.revision ?: 0,old?.createdAt ?: nowMillis())
            PlanningValidation.bill(value);save(value)
        }catch(e:Exception){error=e.message}
    },archive,error) {
        OlloFormSection("Detail Tagihan")
        Field("Nama tagihan",title,{title=it},enabled=!busy)
        Field("Jumlah tagihan ($currency)",amount,{amount=it},enabled=!busy,readOnly=paid,keyboardOptions=decimalKeyboard)
        PlanDateField("Jatuh tempo",date,{date=it},enabled=!busy && !paid)
        if(!paid) {
            OlloFormSection("Kategori & Dompet")
            Choice("Kategori",category,data.categories.filter{it.type=="expense"}.map{it.id to it.name},{if(!busy)category=it})
            Choice("Pengulangan",frequency,listOf("once" to "Sekali") + frequencies.filter{it.first!="daily"},{if(!busy)frequency=it})
            WalletChoice("Dompet pembayaran",accountId,data,data.accounts.filter{!it.archived && it.type!="savings"},!busy){accountId=it}
        } else Text("Tagihan sudah lunas. Batalkan pembayarannya untuk mengubah nominal atau jadwal.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Field("Catatan",note,{note=it},singleLine=false,enabled=!busy)
        if(old!=null) Row(verticalAlignment=Alignment.CenterVertically) { Switch(archived,{archived=it},enabled=!busy);Spacer(Modifier.width(12.dp));Text("Diarsipkan",style=FFType.bodyMain) }
        Text("Saldo berkurang ketika Anda mengonfirmasi pembayaran, bukan saat membuat tagihan.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}

@Composable
fun DebtEditor(old:Debt?,data:FinanceData,busy:Boolean,dismiss:()->Unit,save:(Debt)->Unit,delete:(()->Unit)?) {
    val id=remember{old?.id ?: newId()}
    var person by remember{mutableStateOf(old?.person.orEmpty())}
    var direction by remember{mutableStateOf(old?.direction ?: "borrowed")}
    var accountId by remember{mutableStateOf(old?.accountId ?: data.accounts.firstOrNull{!it.archived && it.type!="savings"}?.id.orEmpty())}
    val currency=data.accounts.find{it.id==accountId}?.currency ?: old?.currency ?: "IDR"
    var amount by remember{mutableStateOf(old?.let{decimalAmount(it.amountMinor,it.currency)}.orEmpty())}
    var issueDate by remember{mutableStateOf(old?.issueDate ?: today().toString())}
    var dueDate by remember{mutableStateOf(old?.dueDate ?: today().toString())}
    var include by remember{mutableStateOf(old?.includeBalance ?: true)}
    var archived by remember{mutableStateOf(old?.archived ?: false)}
    var note by remember{mutableStateOf(old?.note.orEmpty())}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(if(old==null)"Tambah Hutang / Piutang" else "Edit Hutang / Piutang",busy,dismiss,save={
        if(!busy)try {
            val value=Debt(id,person.trim(),direction,minorAmount(amount,currency),old?.paidMinor ?: 0,currency,accountId,issueDate,dueDate,
                include,note.trim(),archived,old?.lastEventId,old?.revision ?: 0,old?.createdAt ?: nowMillis())
            PlanningValidation.debt(value);save(value)
        }catch(e:Exception){error=e.message}
    },delete,error) {
        if(old==null) Choice("Jenis",direction,listOf("borrowed" to "Saya berhutang","lent" to "Saya meminjamkan"),{if(!busy)direction=it})
        OlloFormSection("Detail Hutang / Piutang")
        Field("Nama debitur / kreditur",person,{person=it},enabled=!busy)
        Field("Jumlah ($currency)",amount,{amount=it},enabled=!busy,readOnly=old!=null,keyboardOptions=decimalKeyboard)
        if(old==null) PlanDateField("Tanggal pinjam",issueDate,{issueDate=it},!busy)
        PlanDateField("Jatuh tempo",dueDate,{dueDate=it},!busy)
        OlloFormSection("Dompet & Pencatatan")
        if(old==null) {
            WalletChoice("Dompet",accountId,data,data.accounts.filter{!it.archived && it.type!="savings"},!busy){accountId=it}
            Row(verticalAlignment=Alignment.CenterVertically) { Switch(include,{include=it},enabled=!busy);Spacer(Modifier.width(12.dp));Column { Text("Catat ke saldo dompet",style=FFType.bodyMain);Text("Aktif: pinjaman dan cicilan masuk jurnal.",style=FFType.metaData,color=FFColors.outline) } }
        } else Text("Nominal awal, jenis dan dompet dikunci setelah pencatatan. Cicilan dicatat melalui tombol Bayar / Terima.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Field("Catatan",note,{note=it},singleLine=false,enabled=!busy)
        if(old!=null) Row(verticalAlignment=Alignment.CenterVertically) { Switch(archived,{archived=it},enabled=!busy);Spacer(Modifier.width(12.dp));Text("Diarsipkan",style=FFType.bodyMain) }
        Text(if(old==null) (if(include) "Saat disimpan, saldo ${if(direction=="borrowed")"bertambah" else "berkurang"} sesuai nominal pinjaman." else "Hanya melacak hutang dan cicilan; saldo dan statistik tidak berubah.") else "Riwayat cicilan tetap tersimpan saat data diarsipkan.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}

@Composable
fun RecurringEditor(old:RecurringTransaction?,data:FinanceData,busy:Boolean,dismiss:()->Unit,save:(RecurringTransaction)->Unit,archive:(()->Unit)?) {
    val id=remember{old?.id ?: newId()}
    var title by remember{mutableStateOf(old?.title.orEmpty())}
    var type by remember{mutableStateOf(old?.type ?: "expense")}
    var accountId by remember{mutableStateOf(old?.accountId ?: data.accounts.firstOrNull{!it.archived && it.type!="savings"}?.id.orEmpty())}
    val currency=data.accounts.find{it.id==accountId}?.currency ?: old?.currency ?: "IDR"
    var category by remember{mutableStateOf(old?.categoryId ?: data.categories.firstOrNull{it.type==type}?.id.orEmpty())}
    var amount by remember{mutableStateOf(old?.let{decimalAmount(it.amountMinor,it.currency)}.orEmpty())}
    var frequency by remember{mutableStateOf(old?.frequency ?: "monthly")}
    var interval by remember{mutableStateOf((old?.interval ?: 1).toString())}
    var date by remember{mutableStateOf(old?.nextDate ?: today().toString())}
    var hasEnd by remember{mutableStateOf(old?.endDate!=null)}
    var endDate by remember{mutableStateOf(old?.endDate ?: today().toString())}
    var active by remember{mutableStateOf(old?.active ?: true)}
    var note by remember{mutableStateOf(old?.note.orEmpty())}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(if(old==null)"Transaksi Berulang Baru" else "Edit Transaksi Berulang",busy,dismiss,save={
        if(!busy)try {
            val value=RecurringTransaction(id,title.trim(),type,minorAmount(amount,currency),currency,accountId,category,frequency,
                interval.toInt(),LocalDate.parse(date).dayOfMonth,old?.startDate ?: date,date,if(hasEnd)endDate else null,active,note.trim(),
                old?.lastEventId,old?.revision ?: 0,old?.createdAt ?: nowMillis())
            PlanningValidation.recurring(value);save(value)
        }catch(e:Exception){error=e.message}
    },archive,error) {
        Field("Jumlah ($currency)",amount,{amount=it},enabled=!busy,keyboardOptions=decimalKeyboard)
        SegmentedTabs(listOf("expense" to "Pengeluaran","income" to "Pemasukan"),type,{if(!busy){type=it;category=data.categories.firstOrNull{c->c.type==it}?.id.orEmpty()}})
        Choice("Kategori",category,data.categories.filter{it.type==type}.map{it.id to it.name},{if(!busy)category=it})
        Field("Judul",title,{title=it},enabled=!busy)
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)){Choice("Frekuensi",frequency,frequencies,{if(!busy)frequency=it})}
            Column(Modifier.weight(1f)){PlanDateField(if(old==null)"Tanggal mulai" else "Jadwal berikutnya",date,{date=it},!busy)}
        }
        Field("Ulangi setiap (${when(frequency){"daily"->"hari";"weekly"->"minggu";"yearly"->"tahun";else->"bulan"}})",interval,{interval=it},enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        Row { Checkbox(hasEnd,{hasEnd=it},enabled=!busy);Text("Gunakan tanggal akhir") }
        if(hasEnd)PlanDateField("Tanggal akhir",endDate,{endDate=it},!busy)
        WalletChoice("Bayar dengan Dompet",accountId,data,data.accounts.filter{!it.archived && it.type!="savings"},!busy){accountId=it}
        Row { Switch(active,{active=it},enabled=!busy);Spacer(Modifier.width(12.dp));Text("Jadwal aktif") }
        Field("Catatan",note,{note=it},singleLine=false,enabled=!busy)
        Text("Jadwal yang jatuh tempo dicatat otomatis saat aplikasi dibuka. Pemeriksaan latar belakang mengikuti dukungan perangkat dan pembatasan sistem.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}

data class PaymentRequest(val amountMinor:Long,val accountId:String,val date:String,val requestId:String)

@Composable
fun PaymentEditor(title:String,currency:String,amountMinor:Long,data:FinanceData,defaultAccountId:String,busy:Boolean,
    dismiss:()->Unit,confirm:(PaymentRequest)->Unit,partial:Boolean=false,selectAccount:Boolean=true,changesBalance:Boolean=true) {
    val requestId=remember{newId()}
    var amount by remember{mutableStateOf(decimalAmount(amountMinor,currency))}
    val accounts=data.accounts.filter{it.type!="savings" && it.currency==currency && (!changesBalance || !it.archived)}
    var account by remember{mutableStateOf(defaultAccountId.takeIf{id->accounts.any{it.id==id}} ?: accounts.firstOrNull()?.id.orEmpty())}
    var date by remember{mutableStateOf(today().toString())}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(title,busy,dismiss,save={
        if(!busy)try {
            val minor=minorAmount(amount,currency)
            require(minor in 1..amountMinor){"Nominal melebihi sisa pembayaran"}
            require(date<=today().toString()){"Pembayaran tidak boleh di masa depan"}
            require(account.isNotBlank() || !changesBalance){"Pilih dompet"}
            confirm(PaymentRequest(minor,if(selectAccount)account else defaultAccountId,date,requestId))
        }catch(e:Exception){error=e.message}
    },error=error) {
        Field("Nominal ($currency)",amount,{amount=it},enabled=!busy,readOnly=!partial,keyboardOptions=decimalKeyboard)
        if(selectAccount) WalletChoice("Dompet",account,data,accounts,!busy){account=it}
        else Text("Dompet: ${data.accountName(defaultAccountId)}",style=FFType.bodyMain)
        PlanDateField("Tanggal pembayaran",date,{date=it},!busy)
        Text(if(changesBalance)"Konfirmasi akan menyimpan transaksi dan memperbarui saldo dompet."
            else "Pembayaran ini hanya memperbarui status hutang; tidak mengubah saldo.",style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
    }
}

