package uklo.fikt.pmp.pmpproekt

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary

@Composable
fun LoginScreen(
    authManager: AuthManager,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // ЛАНСЕР ЗА GOOGLE НАЈАВА: Го фаќа резултатот од прозорецот на Google
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    // Го праќаме стрингот (idToken) во твојата функција во AuthManager!
                    authManager.signInWithGoogle(idToken) { success ->
                        isLoading = false
                        if (success) {
                            authManager.saveUserToFirestore() // Го зачувуваме во базата
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, "Грешка при Firebase најава", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    isLoading = false
                    Toast.makeText(context, "Не е пронајден Google Token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                isLoading = false
                Toast.makeText(context, "Грешка: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SkillSwap",
                fontSize = 40.sp, // Големи, впечатливи букви
                fontWeight = FontWeight.Black,
                color = EmeraldPrimary,
                letterSpacing = 1.sp
            )

            // 2. СЛОГАН ПОД ИМЕТО
            Text(
                text = "Размени знаење, изгради иднина.", // Твојот слоган (слободно смени го текстот)
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isRegistering) "Креирај Акаунт" else "Добредојдовте Назад",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            AnimatedVisibility(visible = isRegistering) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Име и презиме") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Е-пошта") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Лозинка") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            if (isLoading) {
                CircularProgressIndicator(color = EmeraldPrimary)
            } else {
                Button(
                    onClick = {
                        if (isRegistering) {
                            if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener { authResult ->
                                        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                            displayName = name
                                        }
                                        authResult.user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                authManager.saveUserToFirestore()
                                                onLoginSuccess()
                                            }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        isLoading = false
                                        Toast.makeText(context, "Грешка: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Toast.makeText(context, "Ве молиме пополнете ги сите полиња!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        onLoginSuccess()
                                    }
                                    .addOnFailureListener { exception ->
                                        isLoading = false
                                        Toast.makeText(context, "Неуспешна најава: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Toast.makeText(context, "Пополнете емаил и лозинка!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (isRegistering) "Регистрирај се" else "Најави се", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!isRegistering) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    Text(" ИЛИ ", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Твојата компонента со подесени кликови
                SocialLoginButtons(
                    onGoogleClick = {
                        isLoading = true
                        // Го стартуваме Google прозорецот со помош на клиентот од твојот AuthManager
                        val signInIntent = authManager.getGoogleSignInClient().signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    },
                    onFacebookClick = {
                        // Повикување на твојот Facebook Login
                        authManager.handleFacebookLogin { success, error ->
                            if (success) {
                                authManager.saveUserToFirestore()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Facebook грешка: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Копче за анонимно најавување (Гост)
                TextButton(
                    onClick = {
                        isLoading = true
                        authManager.signInAnonymously { success ->
                            isLoading = false
                            if (success) {
                                authManager.saveUserToFirestore() // Ќе го зачува како "Гостин" благодарение на твојот убав услов во AuthManager
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Неуспешно најавување како гост", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Продолжи како гост (Анонимно)", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isRegistering) "Веќе имате акаунт? Најавете се" else "Немате акаунт? Креирајте го тука",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable {
                        isRegistering = !isRegistering
                        name = ""; email = ""; password = ""
                    }
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
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
        OutlinedButton(
            onClick = onGoogleClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = R.drawable.google_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Продолжи со Google", color = Color.Black)
            }
        }

        Button(
            onClick = onFacebookClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = R.drawable.facebook_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Продолжи со Facebook", color = Color.White)
            }
        }
    }
}