package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.Field

@Composable
fun DeleteAccountDialog(native: NativeServices, google: Boolean, busy: Boolean, dismiss: () -> Unit,
    confirm: (password: String?, googleToken: String?) -> Unit,dataOnly:Boolean=false,localMode:Boolean=false) {
    val phrase=if(dataOnly)"HAPUS DATA" else "HAPUS"
    var confirmation by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var choosingAccount by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val waiting = busy || choosingAccount
    AlertDialog(onDismissRequest = { if (!waiting) dismiss() }, title = { Text(if(dataOnly)"Hapus Semua Data?" else "Hapus Akun dan Data?") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if(dataOnly)"Dompet, transaksi, perencanaan dan foto akan dihapus permanen. Nama dan akun tetap dipertahankan. Ekspor cadangan terlebih dahulu jika diperlukan." else "Seluruh dompet, transaksi, perencanaan dan foto profil akun ini akan dihapus permanen. Ekspor cadangan terlebih dahulu jika diperlukan. Penghapusan tetap diproses setelah Anda keluar.")
            Field("Ketik $phrase",confirmation,{confirmation=it},enabled=!waiting)
            if (!google && !localMode) OutlinedTextField(password,{password=it},label={Text("Password akun")},singleLine=true,
                visualTransformation=PasswordVisualTransformation(),enabled=!waiting,modifier=Modifier.fillMaxWidth())
            if(!localMode)Text(if (google) "Konfirmasi menggunakan akun Google yang sedang masuk." else "Password diperlukan untuk mengonfirmasi identitas Anda.")
            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            if (waiting) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(enabled=!waiting && confirmation==phrase && (localMode || google || password.isNotEmpty()),onClick={
        error=null
        if(localMode)confirm(null,null)else if (google) {
            choosingAccount=true
            native.googleSignIn { token,failure ->
                choosingAccount=false
                if (token!=null) confirm(null,token) else error=failure ?: "Konfirmasi dibatalkan"
            }
        } else confirm(password,null)
    }) { Text(if (google && !localMode) "Konfirmasi dengan Google" else "Hapus Permanen",color=MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(enabled=!waiting,onClick=dismiss) { Text("Batal") } })
}
