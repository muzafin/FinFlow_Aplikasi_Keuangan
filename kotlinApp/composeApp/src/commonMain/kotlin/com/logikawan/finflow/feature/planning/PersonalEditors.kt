package com.logikawan.finflow.feature.planning

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*

private val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)

@Composable
fun WishEditor(old: Wish?, busy: Boolean, dismiss: () -> Unit, save: (Wish) -> Unit, delete: (() -> Unit)?) {
    val id = remember { old?.id ?: newId() }
    var title by remember { mutableStateOf(old?.title.orEmpty()) }
    var amount by remember { mutableStateOf(old?.let { decimalAmount(it.targetMinor,it.currency) }.orEmpty()) }
    var currency by remember { mutableStateOf(old?.currency ?: "IDR") }
    var priority by remember { mutableStateOf(old?.priority ?: "medium") }
    var link by remember { mutableStateOf(old?.link.orEmpty()) }
    var note by remember { mutableStateOf(old?.note.orEmpty()) }
    var fulfilled by remember { mutableStateOf(old?.fulfilled ?: false) }
    var error by remember { mutableStateOf<String?>(null) }
    EditorDialog(if (old == null) "Keinginan Baru" else "Edit Keinginan", busy, dismiss, save = {
        if (!busy) try {
            val value = Wish(id,title.trim(),minorAmount(amount,currency),currency,priority,link.trim(),note.trim(),fulfilled,old?.createdAt ?: nowMillis())
            PlanningValidation.wish(value); save(value)
        } catch(e: Exception) { error = e.message }
    }, delete, error) {
        Field("Nama keinginan",title,{title=it},enabled = !busy)
        Field("Perkiraan biaya",amount,{amount=it},enabled = !busy,keyboardOptions = numberKeyboard)
        Field("Mata uang",currency,{currency=it.uppercase().take(3)},enabled = !busy)
        Choice("Prioritas",priority,listOf("high" to "Tinggi","medium" to "Sedang","low" to "Rendah"),{if(!busy) priority=it})
        Field("Tautan produk (opsional)",link,{link=it},enabled = !busy)
        Field("Catatan",note,{note=it},singleLine = false,enabled = !busy)
        Row { Checkbox(fulfilled,{fulfilled=it},enabled=!busy); Text("Sudah terpenuhi") }
        Text("Status wishlist tidak mengubah saldo. Catat pembelian melalui Tambah Transaksi.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}

@Composable
fun CardEditor(old: WalletCard?, data: FinanceData, busy: Boolean, dismiss: () -> Unit, save: (WalletCard) -> Unit, delete: (() -> Unit)?) {
    val id = remember { old?.id ?: newId() }
    var name by remember { mutableStateOf(old?.name.orEmpty()) }
    var brand by remember { mutableStateOf(old?.brand ?: "visa") }
    var digits by remember { mutableStateOf(old?.lastFour.orEmpty()) }
    var account by remember { mutableStateOf(old?.accountId.orEmpty()) }
    var month by remember { mutableStateOf(old?.expiryMonth?.takeIf{it>0}?.toString().orEmpty()) }
    var year by remember { mutableStateOf(old?.expiryYear?.takeIf{it>0}?.toString().orEmpty()) }
    var note by remember { mutableStateOf(old?.note.orEmpty()) }
    var type by remember { mutableStateOf(old?.recordType ?: "bank") }
    var number by remember { mutableStateOf(old?.accountNumber.orEmpty()) }
    var owner by remember { mutableStateOf(old?.ownerName.orEmpty()) }
    var label by remember { mutableStateOf(old?.label.orEmpty()) }
    var branch by remember { mutableStateOf(old?.branch.orEmpty()) }
    var ownership by remember { mutableStateOf(old?.ownership ?: "personal") }
    var category by remember { mutableStateOf(old?.category ?: "main") }
    var pinned by remember { mutableStateOf(old?.pinned ?: false) }
    var color by remember { mutableLongStateOf(old?.color ?: 0xFF2196F3) }
    var design by remember { mutableIntStateOf(old?.design ?: 0) }
    var error by remember { mutableStateOf<String?>(null) }
    EditorDialog(if(old==null) "Tambah Kartu / Rekening" else "Edit Kartu / Rekening",busy,dismiss,save = {
        if(!busy) try {
            val value=(old ?: WalletCard(id=id,name="",lastFour="")).copy(
                name=name.trim(),brand=if(type=="card")brand else "other",lastFour=if(type=="card")digits.trim() else "",accountId=account,
                expiryMonth=if(type!="card" || month.isBlank())0 else month.toInt(),expiryYear=if(type!="card" || year.isBlank())0 else year.toInt(),
                note=note.trim(),recordType=type,accountNumber=if(type=="card")"" else number.trim(),ownerName=owner.trim(),label=label.trim(),branch=branch.trim(),
                ownership=ownership,category=category,pinned=pinned,color=color,design=design)
            PlanningValidation.card(value); save(value)
        } catch(e:Exception) {error=e.message}
    },delete,error) {
        WalletCardPreview(WalletCard(id=id,name=name.ifBlank{"Nama bank / kartu"},lastFour=digits,brand=brand,recordType=type,accountNumber=number,ownerName=owner,label=label,color=color,design=design))
        Text("Desain",style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            (0..6).forEach { option -> DesignPill("${option+1}",design==option,{if(!busy)design=option}) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            listOf(0xFF2196F3L to "Biru",0xFF164E63L to "Teal",0xFF2D3748L to "Abu-abu",0xFF2E7D32L to "Hijau",0xFF7C3AEDL to "Ungu",0xFFB45309L to "Oranye",0xFFBE123CL to "Merah").forEach { (option,label) ->
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(16.dp)).background(Color(option))
                    .clickable(enabled=!busy,role=Role.RadioButton){color=option}.semantics{contentDescription="Warna $label";selected=color==option},contentAlignment=Alignment.Center) {
                    if(color==option)Text("✓",color=Color.White,style=FFType.sectionTitle)
                }
            }
        }
        Choice("Jenis catatan",type,listOf("bank" to "Bank","card" to "Kartu","ewallet" to "E-Wallet","other" to "Lainnya"),{if(!busy)type=it})
        Field(if(type=="card")"Nama kartu" else "Nama bank / penyedia",name,{name=it},enabled=!busy)
        if(type=="card") {
            Text("Simpan hanya empat digit terakhir. Jangan masukkan nomor kartu lengkap, CVV atau PIN.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
            Choice("Jaringan kartu",brand,listOf("visa" to "Visa","mastercard" to "Mastercard","jcb" to "JCB","amex" to "American Express","other" to "Lainnya"),{if(!busy)brand=it})
            Field("4 digit terakhir",digits,{digits=it.filter(Char::isDigit).take(19)},enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)){Field("Bulan berlaku",month,{month=it.filter(Char::isDigit).take(2)},enabled=!busy,keyboardOptions=numberKeyboard)}
                Box(Modifier.weight(1f)){Field("Tahun berlaku",year,{year=it.filter(Char::isDigit).take(4)},enabled=!busy,keyboardOptions=numberKeyboard)}
            }
        } else Field(if(type=="bank")"Nomor rekening" else "Nomor / identitas akun",number,{number=it.take(120)},enabled=!busy)
        Field("Nama pemilik",owner,{owner=it.take(100)},enabled=!busy)
        Field("Label (opsional)",label,{label=it.take(100)},enabled=!busy)
        if(type=="bank")Field("Cabang (opsional)",branch,{branch=it.take(100)},enabled=!busy)
        Choice("Kepemilikan",ownership,listOf("personal" to "Pribadi","business" to "Bisnis"),{if(!busy)ownership=it})
        Choice("Kategori",category,listOf("main" to "Utama","savings" to "Tabungan","business" to "Bisnis","other" to "Lainnya"),{if(!busy)category=it})
        WalletChoice("Dompet terkait",account,data,data.accounts.filter{!it.archived},!busy,allowNone=true){account=it}
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(pinned,{pinned=it},enabled=!busy);Text("Sematkan di atas",style=FFType.bodyMain)}
        Field("Catatan",note,{note=it},singleLine=false,enabled=!busy)
    }
}

@Composable
internal fun WalletCardPreview(card:WalletCard,accountName:String="",onClick:(()->Unit)?=null) {
    val base=Color(card.color)
    val light=Color.White.copy(alpha=.12f+(card.design%3)*.06f)
    val colors=when(card.design) {
        1->listOf(base,base.copy(alpha=.65f),base)
        2->listOf(Color(0xFF111827),base)
        3->listOf(base,Color(0xFF101820))
        4->listOf(base,base,Color(0xFF3F3F46))
        5->listOf(Color(0xFF1F2937),base,Color(0xFF1F2937))
        6->listOf(base,Color(0xFF374151),base)
        else->listOf(base,Color(0xFF164E63))
    }
    val gradient=if(card.design%2==0)Brush.horizontalGradient(colors) else Brush.verticalGradient(colors)
    Column(Modifier.fillMaxWidth().heightIn(min=210.dp).clip(RoundedCornerShape(24.dp)).background(gradient)
        .then(if(onClick!=null)Modifier.clickable(role=Role.Button,onClick=onClick) else Modifier)
        .padding(24.dp).testTag("wallet-card-${card.id}"),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(card.name,style=FFType.sectionTitle,color=Color.White)
                if(card.label.isNotBlank())Text(card.label,style=FFType.metaData,color=Color.White.copy(alpha=.8f))
            }
            if(card.pinned)Text("★",color=Color.White)
            FFIcon(if(card.recordType=="bank")"account_balance" else if(card.recordType=="card")"credit_card_outlined" else "account_balance_wallet",Color.White)
        }
        Text(if(card.recordType=="card")"••••  ••••  ••••  ${card.lastFour.padEnd(4,'•')}" else card.accountNumber.ifBlank{"Nomor / identitas akun"},
            style=FFType.amountMd,color=Color.White)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(card.ownerName.ifBlank{if(card.recordType=="card")card.brand.uppercase() else "PRIBADI"},style=FFType.bodyMain,color=Color.White)
            if(card.recordType=="card" && card.expiryMonth>0)Text("${card.expiryMonth.toString().padStart(2,'0')}/${card.expiryYear.toString().takeLast(2)}",style=FFType.metaData,color=Color.White)
        }
        if(accountName.isNotBlank())Text("Dompet: $accountName",style=FFType.metaData,color=Color.White.copy(alpha=.8f))
        Box(Modifier.fillMaxWidth().height(3.dp).background(light,RoundedCornerShape(2.dp)))
    }
}

private data class BundleDraftItem(val id:String=newId(),val name:String="",val amount:String="",val checked:Boolean=false,val paymentId:String?=null)

@Composable
fun NoteEditor(old: FinanceNote?,busy:Boolean,dismiss:()->Unit,save:(FinanceNote)->Unit,delete:(()->Unit)?,data:FinanceData=FinanceData()) {
    val id=remember{old?.id ?: newId()}
    var title by remember{mutableStateOf(old?.title.orEmpty())}
    var body by remember{mutableStateOf(old?.body.orEmpty())}
    var pinned by remember{mutableStateOf(old?.pinned ?: false)}
    var kind by remember{mutableStateOf(old?.kind ?: "bundle")}
    var currency by remember{mutableStateOf(old?.currency ?: "IDR")}
    var account by remember{mutableStateOf(old?.accountId ?: data.accounts.firstOrNull{!it.archived && it.type!="savings" && it.currency==currency}?.id.orEmpty())}
    var recordAs by remember{mutableStateOf(old?.recordAs ?: "single")}
    var archived by remember{mutableStateOf(old?.archived ?: false)}
    val items=remember { mutableStateListOf<BundleDraftItem>().apply {
        addAll(old?.items?.map{BundleDraftItem(it.id,it.name,decimalAmount(it.amountMinor,old.currency),it.checked,it.paymentId)} ?: listOf(BundleDraftItem()))
    } }
    val paid=old?.items?.any{it.paymentId!=null}==true
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(if(kind=="bundle")if(old==null)"Bundle Belanja Baru" else "Edit Bundle Belanja" else if(old==null)"Catatan Baru" else "Edit Catatan",busy,dismiss,save={
        if(!busy)try {
            val value=(old ?: FinanceNote(id=id,title="")).copy(title=title.trim(),body=body,pinned=pinned,kind=kind,currency=currency,
                accountId=if(kind=="bundle")account else null,recordAs=recordAs,archived=archived,
                items=if(kind=="bundle")items.map { BundleItem(it.id,it.name.trim(),minorAmount(it.amount,currency),it.checked,it.paymentId) } else emptyList())
            PlanningValidation.note(value);save(value)
        }catch(e:Exception){error=e.message}
    },delete,error) {
        if(old==null)Choice("Jenis catatan",kind,listOf("bundle" to "Bundle belanja","note" to "Catatan teks"),{if(!busy)kind=it})
        Field(if(kind=="bundle")"Nama bundle" else "Judul",title,{title=it.take(100)},enabled=!busy)
        if(kind=="bundle") {
            Choice("Catat sebagai",recordAs,listOf("single" to "Satu transaksi","items" to "Transaksi per item"),{if(!busy && !paid)recordAs=it})
            Field("Mata uang",currency,{if(!paid){currency=it.uppercase().take(3);account=""}},enabled=!busy && !paid)
            WalletChoice("Dompet pembayaran",account,data,data.accounts.filter{!it.archived && it.type!="savings" && it.currency==currency},!busy){account=it}
            Text("Item belanja",style=FFType.sectionTitle)
            items.forEachIndexed { index,item -> key(item.id) {
                DesignCard {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("Item ${index+1}",Modifier.weight(1f),style=FFType.metaData,fontWeight=FontWeight.SemiBold)
                        if(item.paymentId!=null)Text("Dibayar",style=FFType.metaData,color=FFColors.income)
                        else TextButton(onClick={items.removeAll{it.id==item.id}},enabled=!busy && items.size>1){Text("Hapus",color=FFColors.expense)}
                    }
                    Field("Nama item ${index+1}",item.name,{name->items[index]=item.copy(name=name.take(100))},enabled=!busy)
                    Spacer(Modifier.height(12.dp))
                    Field("Harga item ${index+1} ($currency)",item.amount,{amount->items[index]=item.copy(amount=amount)},enabled=!busy && item.paymentId==null,keyboardOptions=numberKeyboard)
                }
            } }
            OutlinedButton(onClick={items.add(BundleDraftItem())},enabled=!busy && items.size<MAX_BUNDLE_ITEMS,modifier=Modifier.fillMaxWidth()){Text("+ Tambah item (${items.size}/$MAX_BUNDLE_ITEMS)")}
            Text("Pisahkan daftar panjang menjadi beberapa bundle.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
            val total=runCatching{items.fold(0L){sum,item->checkedAdd(sum,if(item.amount.isBlank())0 else minorAmount(item.amount,currency))}}.getOrNull()
            DesignCard {
                Text("Total estimasi",style=FFType.metaData,color=FFColors.onSurfaceVariant)
                Text(total?.let{displayMoney(it,currency)} ?: "Periksa nominal item",style=FFType.amountMd)
                Text("Saldo berubah setelah Anda mengonfirmasi pembayaran item yang dicentang.",style=FFType.metaData,color=FFColors.outline)
            }
            if(old!=null)Row(verticalAlignment=Alignment.CenterVertically){Checkbox(archived,{archived=it},enabled=!busy);Text("Arsipkan bundle",style=FFType.bodyMain)}
        }
        Field(if(kind=="bundle")"Catatan tambahan" else "Isi catatan",body,{body=it.take(10000)},singleLine=false,enabled=!busy)
        Text("${body.length}/10000",style=FFType.metaData,color=FFColors.outline)
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(pinned,{pinned=it},enabled=!busy);Text("Sematkan di atas",style=FFType.bodyMain)}
    }
}

@Composable
fun ReimbursementEditor(old: Reimbursement?,data:FinanceData,busy:Boolean,dismiss:()->Unit,save:(Reimbursement)->Unit,delete:(()->Unit)?) {
    val expenses=data.transactions.filter{it.type=="expense" && !it.scheduled && (it.id==old?.expenseId || data.reimbursements.none{r->r.expenseId==it.id})}
    var expenseId by remember{mutableStateOf(old?.expenseId ?: expenses.firstOrNull()?.id.orEmpty())}
    val expense=expenses.find{it.id==expenseId}
    var title by remember{mutableStateOf(old?.title.orEmpty())}
    var party by remember{mutableStateOf(old?.party.orEmpty())}
    var amount by remember{mutableStateOf(old?.let{decimalAmount(it.amountMinor,it.currency)}.orEmpty())}
    var note by remember{mutableStateOf(old?.note.orEmpty())}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(if(old==null) "Klaim Reimburse" else "Edit Klaim",busy,dismiss,save={
        if(!busy)try {
            val tx=requireNotNull(expense){"Buat transaksi pengeluaran sebelum membuat klaim"}
            val value=Reimbursement(tx.id,tx.id,title.trim().ifBlank{tx.title.take(100)},party.trim(),
                minorAmount(amount.ifBlank{decimalAmount(tx.amountMinor,tx.currency)},tx.currency),tx.currency,tx.accountId,note.trim(),
                old?.receivedAt ?: 0,old?.receivedAccountId,old?.revision ?: 0,old?.createdAt ?: nowMillis())
            PlanningValidation.reimbursement(value);save(value)
        }catch(e:Exception){error=e.message}
    },delete,error) {
        Text("Klaim menggunakan pengeluaran yang sudah tercatat. Penerimaan reimburse akan menambah saldo dompet pilihan.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Choice("Pengeluaran asal",expenseId,expenses.map{it.id to "${it.title} · ${displayMoney(it.amountMinor,it.currency)}"},{if(!busy && old==null){expenseId=it;amount="";title=""}})
        Field("Judul klaim",title,{title=it},enabled=!busy)
        Field("Ditagihkan kepada",party,{party=it},enabled=!busy)
        Field("Nominal klaim (${expense?.currency ?: "IDR"})",amount,{amount=it},enabled=!busy,keyboardOptions=numberKeyboard)
        Text("Kosongkan nominal untuk seluruh pengeluaran asal.",style=FFType.metaData,color=FFColors.outline)
        Field("Catatan",note,{note=it},singleLine=false,enabled=!busy)
    }
}

