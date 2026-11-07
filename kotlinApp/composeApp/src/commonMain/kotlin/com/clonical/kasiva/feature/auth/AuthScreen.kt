package com.clonical.kasiva.feature.auth

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.platform.AppLifecycle
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFIconButton
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.ui.KasivaLogo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(repo: FinanceRepository, native: NativeServices, scope: CoroutineScope, lifecycle: AppLifecycle,
    startLocal:suspend (String,String,String,Long)->Unit={_,_,_,_->},restoreLocal:suspend (com.clonical.kasiva.core.model.FinanceBackup)->Unit={},cancel:(()->Unit)?=null, initialScreen:String="welcome") {
    var screen by rememberSaveable { mutableStateOf(initialScreen) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var obscure by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var backup by remember { mutableStateOf<com.clonical.kasiva.core.model.FinanceBackup?>(null) }
    val login = screen == "login"
    DisposableEffect(lifecycle, screen, busy) {
        lifecycle.onBack = {
            if (screen == initialScreen && cancel != null) { if (!busy) cancel(); true }
            else if (screen != "welcome") {
                if (!busy) { screen = "welcome"; password = ""; error = null }
                true
            } else if(cancel!=null){if(!busy)cancel();true}else false
        }
        onDispose { lifecycle.onBack = null }
    }
    fun submit(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: MR.strings.auth_error_sign_in_failed.text() }
            finally { busy = false }
        }
    }
    fun emailSubmit() {
        error = when {
            !login && name.trim().length < 2 -> MR.strings.auth_error_name_min.text()
            email.isBlank() -> MR.strings.auth_error_email_empty.text()
            !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim()) -> MR.strings.auth_error_email_invalid.text()
            password.isEmpty() -> MR.strings.auth_error_password_empty.text()
            !login && password.length < 8 -> MR.strings.auth_error_password_min.text()
            else -> null
        }
        if (error == null) submit { if (login) repo.login(email.trim(), password) else repo.register(name.trim(), email.trim(), password) }
    }
    if(screen=="local-setup")LocalSetupScreen(busy,{screen="welcome"}){currency,wallet,balance->submit{startLocal(name.trim(),currency,wallet,balance)}}
    else AuthViews(screen, { if (!busy) { if (it == "welcome" && initialScreen == "login" && cancel != null) cancel() else screen = it; password = ""; error = null } },
        name, { name = it }, email, { email = it }, password, { password = it }, busy, error, ::emailSubmit,
        google = {
            if (!busy) {
                busy = true
                error = null
                native.googleSignIn { token, failure ->
                    busy = false
                    if (token != null) submit { repo.googleLogin(token) }
                    else error = failure ?: MR.strings.auth_sign_in_cancelled.text()
                }
            }
        }, reset = {
            if (email.isBlank()) error = MR.strings.auth_error_email_first.text()
            else submit { repo.resetPassword(email.trim()); notice = MR.strings.auth_reset_sent.text() }
        },continueLocal={
            if(name.trim().isBlank() || name.trim().length>100)error=MR.strings.auth_error_name_required.text()
            else {error=null;screen="local-setup"}
        },restore={native.pickCsv{content,failure->
            if(content!=null)try{backup=com.clonical.kasiva.core.model.BackupCodec.decode(content)}catch(e:Exception){error=e.message}
            else if(failure!=null)error=failure
        }})
    if(cancel!=null && screen=="welcome")Box(Modifier.fillMaxSize().safeDrawingPadding(),contentAlignment=Alignment.TopStart){TextButton(onClick=cancel,enabled=!busy){Text(MR.strings.auth_back_to_local.text())}}
    if(screen=="local-setup" && error!=null)AlertDialog(onDismissRequest={error=null},title={Text(MR.strings.auth_cannot_continue.text())},text={Text(error.orEmpty())},confirmButton={TextButton(onClick={error=null}){Text(MR.strings.common_close.text())}})
    backup?.let{value->AlertDialog(onDismissRequest={backup=null},title={Text(MR.strings.auth_restore_title.text())},text={Text(MR.strings.auth_restore_summary_note.text(value.data.profile?.name.orEmpty(),value.data.transactions.size))},
        confirmButton={TextButton(enabled=!busy,onClick={submit{restoreLocal(value);backup=null}}){Text(MR.strings.auth_restore_confirm.text())}},dismissButton={TextButton(enabled=!busy,onClick={backup=null}){Text(MR.strings.common_cancel.text())}})}
    notice?.let { message -> AlertDialog(onDismissRequest = { notice = null }, title = { Text(MR.strings.auth_password_recovery_title.text()) }, text = { Text(message) }, confirmButton = { TextButton(onClick = { notice = null }) { Text(MR.strings.auth_got_it.text()) } }) }
}
