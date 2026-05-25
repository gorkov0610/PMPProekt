package uklo.fikt.pmp.pmpproekt

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.ui.theme.Black
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import uklo.fikt.pmp.pmpproekt.ui.theme.RedCoral
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateDark
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary
import uklo.fikt.pmp.pmpproekt.ui.theme.White

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ProfileScreen(
    authManager: AuthManager,
    dbManager: DatabaseManager,
    onMySkillsClick: () -> Unit,
    onLikedSkillsClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = authManager.getCurrentUser()
    val uid = currentUser?.uid ?: ""

    // Состојби за менаџирање на дијалозите за бришење
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(currentUser?.email ?: "") }
    var profilePic by rememberSaveable { mutableStateOf("") }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        name = doc.getString("name") ?: ""
                        profilePic = doc.getString("profilePicture") ?: ""
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_loading_data),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = profilePic.ifEmpty { "https://ui-avatars.com/api/?name=${name.ifEmpty { "U" }}&background=A7F3D0&color=065F46&size=256" },
                        contentDescription = stringResource(R.string.desc_profile_pic),
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.1f))
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.label_username)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = profilePic,
                            onValueChange = { profilePic = it },
                            label = { Text(stringResource(R.string.label_url_link)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = name.ifEmpty { stringResource(R.string.user) },
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = email.ifEmpty { stringResource(R.string.label_no_email) },
                            color = if (email.isEmpty()) EmeraldPrimary else SlateDark,
                            fontSize = 16.sp,
                            fontWeight = if (email.isEmpty()) FontWeight.Medium else FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onMySkillsClick() },
                            colors = CardDefaults.cardColors(
                                containerColor = EmeraldPrimary.copy(
                                    alpha = 0.08f
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.label_my_posts),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onLikedSkillsClick() },
                            colors = CardDefaults.cardColors(
                                containerColor = EmeraldPrimary.copy(
                                    alpha = 0.08f
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.label_saved_posts),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = {
                            if (isEditing) {
                                if (name.isNotBlank()) {
                                    val updates = mapOf(
                                        "name" to name,
                                        "profilePicture" to profilePic
                                    )
                                    db.collection("users").document(uid)
                                        .update(updates)
                                        .addOnSuccessListener {
                                            isEditing = false
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.saved_changes),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener { e ->
                                            val errorMessage = e.localizedMessage ?: context.getString(R.string.error_unknown)
                                            val finalMessage = context.getString(R.string.error, errorMessage)

                                            Toast.makeText(
                                                context,
                                                finalMessage,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.error_empty_field),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                isEditing = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = White
                        )
                    ) {
                        Text(
                            text = if (isEditing) stringResource(R.string.save_changes) else stringResource(
                                R.string.edit_profile
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        border = BorderStroke(width = 2.dp, color = RedCoral),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCoral)
                    ) {
                        Text(
                            stringResource(R.string.btn_logout),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RedCoral
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedCoral),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_delete_profile),
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // ПРВ ДИЈАЛОГ: Првична потврда за бришење
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.label_attention),
                                color = RedCoral,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = { Text(text = stringResource(R.string.label_delete_profile)) },
                        confirmButton = {
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = RedCoral),
                                onClick = {
                                    showDeleteDialog = false

                                    if (currentUser != null && currentUser.uid.isNotEmpty()) {
                                        // 1. Прво ги бришеме неговите податоци од Firestore
                                        dbManager.deleteUserData(currentUser.uid) { success ->
                                            if (success) {
                                                // 2. Откако базата е чиста, го бришеме од Auth
                                                currentUser.delete()
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            Toast.makeText(
                                                                context,
                                                                context.getString(R.string.label_delete_success),
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            onLogout()
                                                        } else {
                                                            // СЕСИЈАТА Е ЗАСТАРЕНА (Потребна е ре-автентикација)
                                                            if (task.exception is FirebaseAuthRecentLoginRequiredException) {
                                                                val providerId = currentUser.providerData.getOrNull(1)?.providerId

                                                                if (providerId == "google.com") {
                                                                    // Спремање за Google бришење преку LoginScreen
                                                                    authManager.setIsDeletingFlag(true)
                                                                    authManager.signOut()
                                                                    onLogout() // Пренасочува на најава
                                                                } else {
                                                                    // Го отвораме дијалогот за внес на лозинка (за Email корисници)
                                                                    showPasswordDialog = true
                                                                }
                                                            } else {
                                                                Toast.makeText(
                                                                    context,
                                                                    task.exception?.localizedMessage ?: context.getString(R.string.error_unknown),
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                        }
                                                    }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.error_unknown),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.btn_continue), color = White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text(stringResource(R.string.btn_cancel), color = SlateSecondary)
                            }
                        }
                    )
                }

                // ВТОР ДИЈАЛОГ: Барање лозинка за ре-автентикација на Email корисници
                if (showPasswordDialog) {
                    AlertDialog(
                        onDismissRequest = { showPasswordDialog = false },
                        title = { Text("Потврда на идентитет", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("Поради безбедносни причини, внесете ја вашата лозинка за да го потврдите бришењето на профилот:")
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text(stringResource(R.string.label_password)) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = RedCoral),
                                onClick = {
                                    if (confirmPassword.isNotBlank()) {
                                        authManager.reauthenticateAndIdDeleteWithEmail(confirmPassword) { emailSuccess, errorMsg ->
                                            if (emailSuccess) {
                                                showPasswordDialog = false
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.label_delete_success),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onLogout()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    errorMsg ?: context.getString(R.string.error_unknown),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.error_empty_field), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Потврди и Избриши", color = White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPasswordDialog = false }) {
                                Text(stringResource(R.string.btn_cancel), color = SlateSecondary)
                            }
                        }
                    )
                }
            }
        }
    }
}