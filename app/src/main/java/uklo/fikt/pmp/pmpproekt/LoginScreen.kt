package uklo.fikt.pmp.pmpproekt

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.ui.theme.Black
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldLight
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import uklo.fikt.pmp.pmpproekt.ui.theme.FbBlue
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateDark
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary
import uklo.fikt.pmp.pmpproekt.ui.theme.White
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun LoginScreen(
    authManager: AuthManager,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600 ||
            configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = if (isRegistering) {
        name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
    } else {
        email.isNotBlank() && password.isNotBlank()
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {

                    // Проверуваме дали сме дојдени за бришење
                    if (authManager.isDeletingFlag()) {
                        authManager.signInWithGoogle(idToken) { success ->
                            if (success) {
                                // Сега имаме свежа сесија, го бришеме корисникот од Auth инстантно
                                FirebaseAuth.getInstance().currentUser?.delete()
                                    ?.addOnSuccessListener {
                                        authManager.setIsDeletingFlag(false) // Исклучи го знаменцето
                                        authManager.signOut() // Излези за да остане на чист Login
                                        isLoading = false
                                        Toast.makeText(context, context.getString(R.string.label_delete_post_success), Toast.LENGTH_LONG).show()
                                    }
                                    ?.addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, context.getString(R.string.error, e.localizedMessage), Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                isLoading = false
                                Toast.makeText(context, context.getString(R.string.error_firebase), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // СТАНДАРДНА НАЈАВА (Твојот оригинален код)
                        authManager.signInWithGoogle(idToken) { success ->
                            isLoading = false
                            if (success) {
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, context.getString(R.string.error_firebase), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                } else {
                    isLoading = false
                    Toast.makeText(context, context.getString(R.string.error_google_token), Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                isLoading = false
                Toast.makeText(context, context.getString(R.string.error) + e.statusCode, Toast.LENGTH_SHORT).show()
            }
        } else {
            isLoading = false
        }
    }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (isTabletOrLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(EmeraldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.slogan),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = SlateSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoginFormContent(
                            isRegistering = isRegistering,
                            name = name,
                            onNameChange = { name = it },
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            passwordVisible = passwordVisible,
                            onPasswordVisibleChange = { passwordVisible = it },
                            isLoading = isLoading,
                            isFormValid = isFormValid,
                            context = context,
                            authManager = authManager,
                            googleSignInLauncher = googleSignInLauncher,
                            onLoginSuccess = onLoginSuccess,
                            onIsRegisteringChange = {
                                isRegistering = it
                                name = ""; email = ""; password = ""
                            },
                            onLoadingChange = { isLoading = it },
                            isTabletOrLandscape = true
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldPrimary,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = stringResource(R.string.slogan),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = SlateDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LoginFormContent(
                        isRegistering = isRegistering,
                        name = name,
                        onNameChange = { name = it },
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        passwordVisible = passwordVisible,
                        onPasswordVisibleChange = { passwordVisible = it },
                        isLoading = isLoading,
                        isFormValid = isFormValid,
                        context = context,
                        authManager = authManager,
                        googleSignInLauncher = googleSignInLauncher,
                        onLoginSuccess = onLoginSuccess,
                        onIsRegisteringChange = {
                            isRegistering = it
                            name = ""; email = ""; password = ""
                        },
                        onLoadingChange = { isLoading = it },
                        isTabletOrLandscape = false,
                    )
                }
            }
        }
    }
}
@Composable
fun LoginFormContent(
    isRegistering: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    isLoading: Boolean,
    isFormValid: Boolean,
    context: Context,
    authManager: AuthManager,
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    onLoginSuccess: () -> Unit,
    onIsRegisteringChange: (Boolean) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    isTabletOrLandscape: Boolean
) {
    Text(
        text = if (isRegistering) stringResource(R.string.create_account) else stringResource(R.string.welcome_back),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = EmeraldPrimary,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    AnimatedVisibility(visible = isRegistering) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.label_username)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.label_email)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true
    )

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(R.string.label_password)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val desc = if (passwordVisible) stringResource(R.string.desc_hide_password) else stringResource(R.string.desc_show_password)

            IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                Icon(imageVector = image, contentDescription = desc)
            }
        }
    )

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = EmeraldPrimary)
        }
    } else {
        Button(
            onClick = {
                onLoadingChange(true)
                if (isRegistering) {
                    //Го повикуваме signUpWithEmail од AuthManager и му го праќаме името
                    authManager.signUpWithEmail(email, password, name) { success, exception ->
                        onLoadingChange(false)
                        if (success) {
                            onLoginSuccess()
                        } else {
                            // Ја користиме getErrorMessage за убави пораки на македонски
                            val localizedError = authManager.getErrorMessage(exception as? Exception)
                            Toast.makeText(context, localizedError, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    //Го повикуваме signInWithEmail од AuthManager
                    authManager.signInWithEmail(email, password) { success, exception ->
                        onLoadingChange(false)
                        if (success) {
                            onLoginSuccess()
                        } else {
                            val localizedError = authManager.getErrorMessage(exception as? Exception)
                            Toast.makeText(context, localizedError, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                disabledContentColor = White,
                contentColor = White,
                disabledContainerColor = EmeraldLight
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = isFormValid
        ) {
            Text(
                text = if (isRegistering) stringResource(R.string.btn_register) else stringResource(R.string.btn_login),
                fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
        }
    }

    if (!isRegistering) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = SlateSecondary)
            Text(stringResource(R.string.text_or_continue), color = SlateSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = SlateSecondary)
        }
        Spacer(modifier = Modifier.height(20.dp))

        SocialLoginButtons(
            isTabletOrLandscape = isTabletOrLandscape,
            onGoogleClick = {
                onLoadingChange(true)
                val signInIntent = authManager.getGoogleSignInClient().signInIntent
                googleSignInLauncher.launch(signInIntent)
            },
            onFacebookClick = {
                onLoadingChange(true)
                val activity = context.findActivity()
                if (activity != null) {
                    authManager.handleFacebookLogin(activity) { success, error ->
                        onLoadingChange(false)
                        if (success) {
                            onLoginSuccess()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_fb) + error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    onLoadingChange(false)
                    Toast.makeText(context, context.getString(R.string.error_unknown), Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                onLoadingChange(true)
                authManager.signInAnonymously { success ->
                    onLoadingChange(false)
                    if (success) {
                        onLoginSuccess()
                    } else {
                        Toast.makeText(context, context.getString(R.string.error_guest), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_guest), color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = if (isRegistering) stringResource(R.string.text_have_account) else stringResource(R.string.text_no_account),
        color = SlateDark,
        fontSize = 14.sp,
        modifier = Modifier
            .clickable { onIsRegisteringChange(!isRegistering) }
            .padding(8.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun SocialLoginButtons(
    isTabletOrLandscape: Boolean,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit
) {
    if (isTabletOrLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onGoogleClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateSecondary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.google_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.signin_google), color = Black, maxLines = 1)
                }
            }

            Button(
                onClick = onFacebookClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.facebook_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.signin_fb), color = White, maxLines = 1)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onGoogleClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateSecondary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.google_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.signin_google), color = Black)
                }
            }

            Button(
                onClick = onFacebookClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.facebook_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.signin_fb), color = White)
                }
            }
        }
    }
}
fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return currentContext as? Activity
}