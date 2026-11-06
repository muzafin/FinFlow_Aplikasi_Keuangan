package com.logikawan.finflow.feature.auth

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
import com.logikawan.finflow.core.ui.*

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
            Text("Selamat Datang",style=FFType.appTitle.copy(fontSize=32.sp,lineHeight=40.sp,fontWeight=FontWeight.Bold))
            Spacer(Modifier.height(6.dp))
            Text("Perjalanan menuju keuangan yang lebih baik dimulai di sini.",style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=24.sp),color=FFColors.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){FinFlowLogo(140.dp)}
            Spacer(Modifier.height(20.dp))
            EntryField("Nama lengkap","Nama lengkap",name,changeName,busy)
            Spacer(Modifier.height(28.dp))
            Text("Mencari data lamamu?",Modifier.fillMaxWidth(),style=FFType.metaData,textAlign=TextAlign.Center,color=FFColors.onSurfaceVariant)
            if(restore!=null) {
                TextButton(onClick=restore,enabled=!busy,modifier=Modifier.align(Alignment.CenterHorizontally)) {
                    FFIcon("history",entryAccent,20.dp);Spacer(Modifier.width(8.dp));Text("Pulihkan dari Cadangan",fontWeight=FontWeight.SemiBold)
                }
            }
            GoogleEntryButton(busy,google,"Masuk dengan Google")
            Spacer(Modifier.height(12.dp))
            Text("Baru di FinFlow? Lanjutkan tanpa akun dan data disimpan di perangkat ini. Hubungkan akun melalui Profil saat ingin memakai sinkronisasi cloud. Buat cadangan agar data lokal tetap aman.",
                style=FFType.metaData.copy(lineHeight=20.sp),color=FFColors.onSurfaceVariant,textAlign=TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            EntryButton("Lanjut",busy,continueLocal ?: {navigate("register")})
            Spacer(Modifier.height(12.dp))
            Row(Modifier.align(Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically) {
                Text("Sudah punya akun? ",style=FFType.metaData,color=FFColors.onSurfaceVariant)
                Text("Masuk",Modifier.clickable(enabled=!busy){navigate("login")},style=FFType.metaData,fontWeight=FontWeight.Bold,color=entryAccent)
            }
        } else {
            TextButton(onClick={navigate("welcome")},enabled=!busy,contentPadding=PaddingValues(0.dp)) { FFIcon("arrow_back",entryAccent,20.dp);Spacer(Modifier.width(8.dp));Text("Kembali") }
            Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){FinFlowLogo(108.dp)}
            Spacer(Modifier.height(24.dp))
            Text(if(login)"Selamat Datang Kembali" else "Buat Akun",style=FFType.appTitle.copy(fontSize=28.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold))
            Text(if(login)"Masuk untuk membuka data cloud milikmu." else "Hubungkan keuanganmu dengan akun FinFlow.",style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            if(!login){EntryField("Nama","Nama lengkap",name,changeName,busy);Spacer(Modifier.height(16.dp))}
            EntryField("Email","nama@email.com",email,changeEmail,busy,keyboardType=KeyboardType.Email)
            Spacer(Modifier.height(16.dp))
            EntryField("Password","Minimal 8 karakter",password,changePassword,busy,secret=true)
            if(login)TextButton(onClick=reset,enabled=!busy,modifier=Modifier.align(Alignment.End)){Text("Lupa password?")}
            else {Spacer(Modifier.height(6.dp));Text("Minimal 8 karakter.",style=FFType.metaData,color=FFColors.onSurfaceVariant)}
            Spacer(Modifier.height(24.dp));EntryButton(if(login)"Masuk" else "Daftar Sekarang",busy,submit)
            Spacer(Modifier.height(16.dp));GoogleEntryButton(busy,google,"Masuk dengan Google")
            Spacer(Modifier.height(24.dp))
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                Text(if(login)"Belum punya akun? " else "Sudah punya akun? ",style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
                Text(if(login)"Daftar" else "Masuk",Modifier.clickable(enabled=!busy){navigate(if(login)"register" else "login")},style=FFType.bodyMain,fontWeight=FontWeight.Bold,color=entryAccent)
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

@Composable
internal fun EntryField(label:String,placeholder:String,value:String,change:(String)->Unit,busy:Boolean,secret:Boolean=false,keyboardType:KeyboardType=KeyboardType.Text) {
    var obscure by remember{mutableStateOf(true)}
    OutlinedTextField(value,change,modifier=Modifier.fillMaxWidth().heightIn(min=60.dp).testTag("entry-${label.lowercase()}"),enabled=!busy,
        label={Text(label)},placeholder={Text(placeholder)},singleLine=true,textStyle=FFType.bodyMain,
        keyboardOptions=KeyboardOptions(keyboardType=if(secret)KeyboardType.Password else keyboardType),
        visualTransformation=if(secret && obscure)PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon=if(secret)({IconButton(onClick={obscure=!obscure},enabled=!busy){FFIcon(if(obscure)"visibility_outlined" else "visibility_off_outlined",contentDescription=if(obscure)"Tampilkan password" else "Sembunyikan password")}})else null,
        shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=entryAccent,unfocusedBorderColor=Color(0xFFE1E3E6)))
}
