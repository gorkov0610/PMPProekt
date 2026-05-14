package uklo.fikt.pmp.pmpproekt

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun LoginScreen(onLoginSuccess : () -> Unit){
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }

    // State за Email и Password
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) } // Прекинувач меѓу најава и регистрација
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        authManager.handleFacebookLogin { success, errorMessage ->
            if (success) {
                onLoginSuccess()
            } else {
                Log.e("FB_Auth", "Грешка: $errorMessage")
            }
        }
        onDispose {}
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                authManager.signInWithGoogle(token) { success ->
                    if (success) {
                        onLoginSuccess()
                    } else {
                        Log.e("AuthError", "Firebase не го прифати токенот")
                    }
                }
            }
        } catch (e: ApiException) {
            Log.e("Auth", "Грешка од Google: ${e.statusCode}")
        }
    }


    Scaffold(containerColor = Color(0xFFF8F9FA)) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()), // За да може да се скрола ако тастатурата пречи
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.app_name), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
            Text(stringResource(R.string.slogan), color = SlateSecondary)

            Spacer(modifier = Modifier.height(48.dp))

            // --- EMAIL & PASSWORD ПОЛИЊА ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.label_email)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.label_password)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff

                    val description = if (passwordVisible) stringResource(R.string.desc_hide_password) else stringResource(R.string.desc_show_password)

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            // ГЛАВНО КОПЧЕ ЗА ЕМАИЛ (Најава или Регистрација)
            Button(
                onClick = {
                    val onAuthResult: (Boolean, Throwable?) -> Unit = { success, exception ->
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorMessage =
                                authManager.getErrorMessage(exception as? Exception, context)
                        }
                    }

                    if (isRegistering) {
                        authManager.signUpWithEmail(email, password, onAuthResult)
                    } else {
                        authManager.signInWithEmail(email, password, onAuthResult)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                enabled = email.isNotEmpty() && password.isNotEmpty()
            ) {
                Text(if (isRegistering) stringResource(R.string.btn_register) else stringResource(R.string.btn_login))
            }

            TextButton(onClick = { isRegistering = !isRegistering }) {
                Text(if (isRegistering) stringResource(R.string.text_have_account) else stringResource(R.string.text_no_account))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ЛИНИЈА ЗА ОДДЕЛУВАЊЕ ---
            Text(stringResource(R.string.text_or_continue), color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // --- СОЦИЈАЛНИ МРЕЖИ (Веќе средени) ---
            SocialLoginButtons(
                onGoogleClick = {
                    val signInIntent = authManager.getGoogleSignInClient().signInIntent
                    launcher.launch(signInIntent)
                },
                onFacebookClick = {
                    // Овде оди Facebook логиката кога ќе ја средиш
                    Log.d("Auth", "Facebook кликнато")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- АНОНИМНА НАЈАВА ---
            TextButton(
                onClick = {
                    authManager.signInAnonymously { success ->
                        if (success) onLoginSuccess()
                    }
                }
            ) {
                Text(stringResource(R.string.btn_guest), color = SlateSecondary, textDecoration = TextDecoration.Underline)
            }
        }
    }
}

@Composable
fun SocialLoginButtons(
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Google Копче
        OutlinedButton(
            onClick = onGoogleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("Продолжи со Google", color = Color.Black)
            }
        }

        // Facebook Копче
        Button(
            onClick = onFacebookClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.facebook_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("Продолжи со Facebook", color = Color.White)
            }
        }
    }
}