package uklo.fikt.pmp.pmpproekt

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val auth = FirebaseAuth.getInstance()

    val activity = context as? Activity
    val isTabletOrLandscape = if (activity != null) {
        val windowSizeClass = calculateWindowSizeClass(activity)
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium ||
                windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    } else {
        false
    }

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
                    authManager.signInWithGoogle(idToken) { success ->
                        isLoading = false
                        if (success) {
                            authManager.saveUserToFirestore()
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, context.getString(R.string.error_firebase), Toast.LENGTH_SHORT).show()
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
            // РАСПОРЕД ЗА ТАБЛЕТ / ЛЕНДСКЕЈП (Два панели)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ЛЕВ ПАНЕЛ: Бренд елементи со позадина
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(EmeraldPrimary.copy(alpha = 0.05f)), // Нежна зелена позадина
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
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ДЕСЕН ПАНЕЛ: Форма за најава (скролабилна)
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
                            auth = auth,
                            context = context,
                            authManager = authManager,
                            googleSignInLauncher = googleSignInLauncher,
                            onLoginSuccess = onLoginSuccess,
                            onIsRegisteringChange = {
                                isRegistering = it
                                name = ""; email = ""; password = ""
                            },
                            onLoadingChange = { isLoading = it }
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
                        color = Color.Gray,
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
                        auth = auth,
                        context = context,
                        authManager = authManager,
                        googleSignInLauncher = googleSignInLauncher,
                        onLoginSuccess = onLoginSuccess,
                        onIsRegisteringChange = {
                            isRegistering = it
                            name = ""; email = ""; password = ""
                        },
                        onLoadingChange = { isLoading = it }
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
    auth: FirebaseAuth,
    context: android.content.Context,
    authManager: AuthManager,
    googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    onLoginSuccess: () -> Unit,
    onIsRegisteringChange: (Boolean) -> Unit,
    onLoadingChange: (Boolean) -> Unit
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.label_email)) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true
    )

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(R.string.label_password)) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
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
        CircularProgressIndicator(color = EmeraldPrimary)
    } else {
        Button(
            onClick = {
                onLoadingChange(true)
                if (isRegistering) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                displayName = name
                            }
                            authResult.user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                                onLoadingChange(false)
                                if (task.isSuccessful) {
                                    authManager.saveUserToFirestore()
                                    onLoginSuccess()
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            onLoadingChange(false)
                            Toast.makeText(context, context.getString(R.string.error) + exception.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                } else {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            onLoadingChange(false)
                            onLoginSuccess()
                        }
                        .addOnFailureListener { exception ->
                            onLoadingChange(false)
                            Toast.makeText(context, context.getString(R.string.error_login) + exception.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                disabledContentColor = SlateDark,
                contentColor = White,
                disabledContainerColor = SlateSecondary
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
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
            Text(stringResource(R.string.text_or_continue), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        }
        Spacer(modifier = Modifier.height(20.dp))

        SocialLoginButtons(
            onGoogleClick = {
                onLoadingChange(true)
                val signInIntent = authManager.getGoogleSignInClient().signInIntent
                googleSignInLauncher.launch(signInIntent)
            },
            onFacebookClick = {
                authManager.handleFacebookLogin { success, error ->
                    if (success) {
                        authManager.saveUserToFirestore()
                        onLoginSuccess()
                    } else {
                        Toast.makeText(context, context.getString(R.string.error_fb) + error, Toast.LENGTH_SHORT).show()
                    }
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
                        authManager.saveUserToFirestore()
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
        color = Color.Gray,
        fontSize = 14.sp,
        modifier = Modifier
            .clickable { onIsRegisteringChange(!isRegistering) }
            .padding(8.dp),
        textAlign = TextAlign.Center
    )
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
        OutlinedButton(
            onClick = onGoogleClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
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
            modifier = Modifier.fillMaxWidth().height(56.dp),
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