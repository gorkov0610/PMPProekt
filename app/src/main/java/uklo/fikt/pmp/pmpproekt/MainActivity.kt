package uklo.fikt.pmp.pmpproekt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp // За иконата за излез
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FloatingActionButton
import androidx.media3.common.util.UnstableApi
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Skill
import uklo.fikt.pmp.pmpproekt.ui.theme.BackgroundGray
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldDark
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldLight
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import uklo.fikt.pmp.pmpproekt.ui.theme.PMPProektTheme
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PMPProektTheme {
                val authManager = remember { AuthManager(applicationContext) }
                var user by remember { mutableStateOf(authManager.getCurrentUser()) }
                if(user == null){
                    LoginScreen(onLoginSuccess = {
                        user = null
                        user = authManager.getCurrentUser()
                    })
                } else {
                    MainContent(
                        email = user?.email?: "Корисник",
                        onLogout = {
                            authManager.signOut()
                            user = null
                        }
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(email : String, onLogout : () -> Unit){
    val context = LocalContext.current
    val dbManager = remember { DatabaseManager() }
    val authManager = remember { AuthManager(context) }

    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SkillSwap", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                    }
                })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ){ padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(BackgroundGray)) {
            SkillFeed(dbManager)
        }
    }
    if (showDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Што нудиш?", color = EmeraldPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Име на вештина (пр. Гитара)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Краток опис") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    onClick = {
                        Log.d("SkillSwap", "Копчето е кликнато")
                        if (title.isNotBlank()) {
                            val newSkill = Skill(
                                title = title,
                                description = desc,
                                authorName = authManager.getCurrentUser()?.displayName
                                    ?: "Корисник",
                                contactEmail = email,
                                category = "Општо"
                            )
                            dbManager.saveSkill(newSkill) { success ->
                                if (success) {
                                    Log.d("SkillSwap", "Успешно запишување")
                                    showDialog = false
                                }else{
                                    Log.d("SkillSwap", "Неуспешно запишување")
                                }
                            }
                        }
                    }
                ) { Text("Објави") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Откажи", color = Color.Gray)
                }
            }
        )
    }
}


