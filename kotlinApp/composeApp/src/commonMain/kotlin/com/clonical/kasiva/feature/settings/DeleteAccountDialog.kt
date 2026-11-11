package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.Field
import com.clonical.kasiva.i18n.*

@Composable
fun DeleteAccountDialog(native: NativeServices, google: Boolean, busy: Boolean, dismiss: () -> Unit,
    confirm: (password: String?, googleToken: String?) -> Unit,dataOnly:Boolean=false,localMode:Boolean=false) {
    // Frasa konfirmasi sengaja tidak diterjemahkan: pengguna mengetiknya persis.
    val phrase=if(dataOnly)"HAPUS DATA" else "HAPUS"
    var confirmation by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var choosingAccount by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val waiting = busy || choosingAccount
    AlertDialog(onDismissRequest = { if (!waiting) dismiss() }, containerColor = com.clonical.kasiva.core.ui.FFColors.canvas, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        icon = { com.clonical.kasiva.core.ui.FFIcon(AdIcons.alert, AdTone.danger, 28.dp) },
        title = { Text(if(dataOnly)MR.strings.auth_delete_data_title.text() else MR.strings.auth_delete_account_title.text(), style = adText(18.sp, androidx.compose.ui.text.font.FontWeight.SemiBold, 24.sp)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdBody(if(dataOnly)MR.strings.auth_delete_data_description.text() else MR.strings.auth_delete_account_description.text())
            Field(MR.strings.auth_delete_type_phrase.text(phrase),confirmation,{confirmation=it},enabled=!waiting)
            if (!google && !localMode) OutlinedTextField(password,{password=it},label={Text(MR.strings.auth_delete_account_password.text())},singleLine=true,
                visualTransformation=PasswordVisualTransformation(),enabled=!waiting,modifier=Modifier.fillMaxWidth())
            if(!localMode)AdBody(if (google) MR.strings.auth_delete_google_note.text() else MR.strings.auth_delete_password_note.text(), size = 13.sp)
            error?.let { Text(it,color=AdTone.danger) }
            
        }
    }, confirmButton = { TextButton(enabled=!waiting && confirmation==phrase && (localMode || google || password.isNotEmpty()),onClick={
        error=null
        if(localMode)confirm(null,null)else if (google) {
            choosingAccount=true
            native.googleSignIn { token,failure ->
                choosingAccount=false
                if (token!=null) confirm(null,token) else error=failure ?: MR.strings.auth_delete_cancelled.text()
            }
        } else confirm(password,null)
    }) { Text(if (google && !localMode) MR.strings.auth_delete_confirm_google.text() else MR.strings.auth_delete_permanently.text(),color=if(!waiting && confirmation==phrase && (localMode || google || password.isNotEmpty())) AdTone.danger else AdTone.danger.copy(alpha=.4f)) } },
        dismissButton = { TextButton(enabled=!waiting,onClick=dismiss) { Text(MR.strings.common_cancel.text(),color=com.clonical.kasiva.core.ui.FFColors.onSurfaceVariant) } })
}
