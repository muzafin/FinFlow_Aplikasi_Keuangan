package com.clonical.kasiva.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.i18n.*

@Composable
fun AuthViews(screen:String,navigate:(String)->Unit,name:String,changeName:(String)->Unit,email:String,changeEmail:(String)->Unit,
    password:String,changePassword:(String)->Unit,busy:Boolean,error:String?,submit:()->Unit,google:()->Unit,reset:()->Unit,
    continueLocal:(()->Unit)?=null,restore:(()->Unit)?=null) = EntryTheme {
    val welcome=screen=="welcome"
    val login=screen=="login"
    Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding().imePadding().testTag("auth-$screen")
        .verticalScroll(rememberScrollState()).padding(horizontal=24.dp)) {
        if(welcome) {
            Spacer(Modifier.height(54.dp))
            Text(MR.strings.auth_welcome_title.text(),style=FFType.appTitle.copy(fontSize=32.sp,lineHeight=40.sp,fontWeight=FontWeight.Bold))
            Spacer(Modifier.height(6.dp))
            Text(MR.strings.auth_welcome_subtitle.text(),style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=24.sp),color=FFColors.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){KasivaLogo(140.dp)}
            Spacer(Modifier.height(20.dp))
            EntryField(MR.strings.auth_full_name.text(),MR.strings.auth_full_name.text(),name,changeName,busy,tag="Nama lengkap")
            Spacer(Modifier.height(28.dp))
            Text(MR.strings.auth_looking_for_old_data.text(),Modifier.fillMaxWidth(),style=FFType.metaData,textAlign=TextAlign.Center,color=FFColors.onSurfaceVariant)
            if(restore!=null) {
                TextButton(onClick=restore,enabled=!busy,modifier=Modifier.align(Alignment.CenterHorizontally)) {
                    FFIcon("history",entryAccent,20.dp);Spacer(Modifier.width(8.dp));Text(MR.strings.auth_restore_from_backup.text(),fontWeight=FontWeight.SemiBold)
                }
            }
            GoogleEntryButton(busy,google,MR.strings.auth_sign_in_google.text())
            Spacer(Modifier.height(12.dp))
            Text(MR.strings.auth_local_mode_note.text(),
                style=FFType.metaData.copy(lineHeight=20.sp),color=FFColors.onSurfaceVariant,textAlign=TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            EntryButton(MR.strings.common_next.text(),busy,continueLocal ?: {navigate("register")})
            Spacer(Modifier.height(12.dp))
            Row(Modifier.align(Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically) {
                Text(MR.strings.auth_have_account.text()+" ",style=FFType.metaData,color=FFColors.onSurfaceVariant)
                Text(MR.strings.auth_sign_in.text(),Modifier.clickable(enabled=!busy){navigate("login")},style=FFType.metaData,fontWeight=FontWeight.Bold,color=entryAccent)
            }
        } else {
            TextButton(onClick={navigate("welcome")},enabled=!busy,contentPadding=PaddingValues(0.dp)) { FFIcon("arrow_back",entryAccent,20.dp);Spacer(Modifier.width(8.dp));Text(MR.strings.common_back.text()) }
            Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){KasivaLogo(108.dp)}
            Spacer(Modifier.height(24.dp))
            Text(if(login)MR.strings.auth_welcome_back.text() else MR.strings.auth_create_account.text(),style=FFType.appTitle.copy(fontSize=28.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold))
            Text(if(login)MR.strings.auth_login_subtitle.text() else MR.strings.auth_register_subtitle.text(),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            if(!login){EntryField(MR.strings.auth_name.text(),MR.strings.auth_full_name.text(),name,changeName,busy,tag="Nama");Spacer(Modifier.height(16.dp))}
            EntryField(MR.strings.auth_email.text(),MR.strings.auth_email_placeholder.text(),email,changeEmail,busy,keyboardType=KeyboardType.Email,tag="Email")
            Spacer(Modifier.height(16.dp))
            EntryField(MR.strings.auth_password.text(),MR.strings.auth_password_min_placeholder.text(),password,changePassword,busy,secret=true,tag="Password")
            if(login)TextButton(onClick=reset,enabled=!busy,modifier=Modifier.align(Alignment.End)){Text(MR.strings.auth_forgot_password.text())}
            else {Spacer(Modifier.height(6.dp));Text(MR.strings.auth_password_min_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
            Spacer(Modifier.height(24.dp));EntryButton(if(login)MR.strings.auth_sign_in.text() else MR.strings.auth_register_now.text(),busy,submit)
            Spacer(Modifier.height(16.dp));GoogleEntryButton(busy,google,MR.strings.auth_sign_in_google.text())
            Spacer(Modifier.height(24.dp))
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                Text((if(login)MR.strings.auth_no_account.text() else MR.strings.auth_have_account.text())+" ",style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
                Text(if(login)MR.strings.auth_register.text() else MR.strings.auth_sign_in.text(),Modifier.clickable(enabled=!busy){navigate(if(login)"register" else "login")},style=FFType.bodyMain,fontWeight=FontWeight.Bold,color=entryAccent)
            }
        }
        error?.let { Spacer(Modifier.height(16.dp));Text(it,Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer,RoundedCornerShape(12.dp)).padding(12.dp),color=MaterialTheme.colorScheme.onErrorContainer) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun EntryButton(label:String,busy:Boolean,onClick:()->Unit) {
    Button(onClick=onClick,enabled=!busy,modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(20.dp)) {
        if(busy)CircularProgressIndicator(Modifier.size(22.dp),color=MaterialTheme.colorScheme.onPrimary,strokeWidth=2.dp)
        else Text(label,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
    }
}

@Composable
private fun GoogleEntryButton(busy:Boolean,onClick:()->Unit,label:String) {
    OutlinedButton(onClick=onClick,enabled=!busy,modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(14.dp),
        colors=ButtonDefaults.outlinedButtonColors(contentColor=FFColors.onSurface),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFFE1E3E6))) {
        FigmaAsset("fig_117_354_google");Spacer(Modifier.width(10.dp));Text(label,fontWeight=FontWeight.SemiBold)
    }
}

/** [tag] membentuk testTag `entry-<tag>`; tetap berupa label Indonesia agar tidak berubah mengikuti bahasa. */
@Composable
internal fun EntryField(label:String,placeholder:String,value:String,change:(String)->Unit,busy:Boolean,secret:Boolean=false,keyboardType:KeyboardType=KeyboardType.Text,tag:String=label) {
    var obscure by remember{mutableStateOf(true)}
    OutlinedTextField(value,change,modifier=Modifier.fillMaxWidth().heightIn(min=60.dp).testTag("entry-${tag.lowercase()}"),enabled=!busy,
        label={Text(label)},placeholder={Text(placeholder)},singleLine=true,textStyle=FFType.bodyMain,
        keyboardOptions=KeyboardOptions(keyboardType=if(secret)KeyboardType.Password else keyboardType),
        visualTransformation=if(secret && obscure)PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon=if(secret)({IconButton(onClick={obscure=!obscure},enabled=!busy){FFIcon(if(obscure)"visibility_outlined" else "visibility_off_outlined",contentDescription=if(obscure)MR.strings.auth_show_password.text() else MR.strings.auth_hide_password.text())}})else null,
        shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=entryAccent,unfocusedBorderColor=Color(0xFFE1E3E6)))
}
