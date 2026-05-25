package uklo.fikt.pmp.pmpproekt

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import kotlinx.coroutines.delay
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Screen
import uklo.fikt.pmp.pmpproekt.data.Skill
import uklo.fikt.pmp.pmpproekt.ui.theme.BackgroundGray
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import uklo.fikt.pmp.pmpproekt.ui.theme.PMPProektTheme
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary
import uklo.fikt.pmp.pmpproekt.ui.theme.White

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PMPProektTheme {
                val authManager = remember { AuthManager(applicationContext) }
                var user by remember { mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null) }

                var isCheckingAuth by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.withTimeoutOrNull(4000) {
                        delay(1000)
                        user = authManager.getCurrentUser()
                    }
                    isCheckingAuth = false
                }

                Crossfade(
                    targetState = isCheckingAuth,
                    animationSpec = tween(durationMillis = 800),
                    label = "SplashToMainTransition"
                ) { checking ->
                    if (checking) {
                        SplashScreen(onTimeout = {})
                    } else {
                        if (user == null) {
                            LoginScreen(authManager = authManager, onLoginSuccess = {
                                user = authManager.getCurrentUser()
                            })
                        } else {
                            MainContent(
                                email = user?.email ?: stringResource(R.string.user)
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Suppress("UNUSED_VALUE", "AssignedValueIsNeverUsed", "RedundantSuppression")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(email : String){
    val navController = rememberNavController()
    val context = LocalContext.current
    val dbManager = remember { DatabaseManager() }
    val authManager = remember { AuthManager(context) }
    val defaultUsername = stringResource(R.string.user)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showDialog by rememberSaveable { mutableStateOf(false) }
    val currentUser = remember(authManager) { authManager.getCurrentUser() }
    val currentUserId = currentUser?.uid ?: ""

    DisposableEffect(currentUserId) {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        if (currentUserId.isNotEmpty()) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            Log.d("GLOBAL_FCM", "Активиран нов прецизен слушател за: $currentUserId")

            listenerRegistration = db.collectionGroup("messages")
                .whereEqualTo("receiverId", currentUserId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("GLOBAL_FCM", "Грешка при слушање", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val latestDoc = snapshot.documents.first()
                        val isFromCache = snapshot.metadata.isFromCache

                        val senderId = latestDoc.getString("senderId") ?: ""
                        val text = latestDoc.getString("text") ?: context.getString(R.string.default_message_text)
                        val senderName = latestDoc.getString("senderName") ?: context.getString(R.string.default_sender_name)


                        if (senderId != currentUserId && !isFromCache) {
                            showLocalNotification(
                                context = context,
                                title = context.getString(R.string.new_message, senderName),
                                message = text,
                                senderId = senderId,
                                senderName = senderName
                            )
                        }
                    }
                }
        }
        onDispose {
            listenerRegistration?.remove()
        }
    }
    Scaffold(
        topBar = {
            if (currentRoute == Screen.Feed.route) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name), color = White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldPrimary),
                    actions = {
                        IconButton(onClick = { navController.navigate("profile") }) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = stringResource(R.string.desc_profile), tint = White)
                        }
                        IconButton(onClick = { navController.navigate("inbox") }) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = stringResource(R.string.desc_inbox), tint = White)
                        }
                    }
                )
            } else if (currentRoute == "inbox") {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.inbox_title), color = White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldPrimary),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back), tint = White)
                        }
                    }
                )
            } else if (currentRoute?.startsWith("chat/") == true) {
                val authorName = navBackStackEntry?.arguments?.getString("authorName") ?: stringResource(R.string.user)

                TopAppBar(
                    title = {
                        Text(
                            text = authorName,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldPrimary),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.desc_back),
                                tint = White
                            )
                        }
                    }
                )
            }else if (currentRoute == "profile") {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.top_bar_profile), color = White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldPrimary),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back), tint = White)
                        }
                    }
                )
            } else if (currentRoute == "my_skills") {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.label_my_posts), color = White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldPrimary),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back), tint = White)
                        }
                    }
                )
            } else if (currentRoute == "liked_skills") {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.label_saved_posts), color = White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldPrimary),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back), tint = White)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (currentRoute == Screen.Feed.route) {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = EmeraldPrimary,
                    contentColor = White
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_add))
                }
            }
        }
    ){ padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(BackgroundGray)) {

            NavHost(
                navController,
                Screen.Feed.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.95f, animationSpec = tween(350))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(350)) + scaleOut(targetScale = 0.95f, animationSpec = tween(350))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.95f, animationSpec = tween(350))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(350)) + scaleOut(targetScale = 0.95f, animationSpec = tween(350))
                }
            ) {

                composable(Screen.Feed.route) {
                    SkillFeed(
                        dbManager,
                        authManager,
                        onChatClick = { skill ->
                            val encodeName = Uri.encode(skill.authorName)
                            navController.navigate(Screen.Chat.createRoute(skill.authorId,encodeName))
                        })
                }
                composable(Screen.Inbox.route) {
                    InboxScreen(
                        authManager = authManager,
                        onChatClick = { receiverId, name ->
                            val encodeName = Uri.encode(name)
                            navController.navigate(Screen.Chat.createRoute(receiverId, encodeName))
                        }
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        authManager = authManager,
                        onLogout = {
                            authManager.signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onMySkillsClick = {
                            navController.navigate(Screen.MySkills.route)
                        },
                        onLikedSkillsClick = {
                            navController.navigate(Screen.LikedSkills.route)
                        }
                    )
                }
                composable(Screen.MySkills.route) {
                    val uid = authManager.getCurrentUser()?.uid ?: ""
                    UserSkillsScreen(
                        uid = uid
                    )
                }
                composable(Screen.LikedSkills.route) {
                    val uid = authManager.getCurrentUser()?.uid ?: ""
                    LikedSkillsScreen(
                        uid = uid,
                        onChatClick = { authorId, authorName ->
                            navController.navigate("chat/$authorId/$authorName")
                        },
                        databaseManager = dbManager
                    )
                }
                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(
                        navArgument("receiverId") { type = NavType.StringType },
                        navArgument("authorName") { type = NavType.StringType }
                    ),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "skillswap://chat/{receiverId}/{authorName}" }
                    )
                ) { backStackEntry ->
                    val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
                    val authorName = backStackEntry.arguments?.getString("authorName") ?: ""

                    ChatScreen(
                        receiverId = receiverId,
                        receiverName = authorName,
                        authManager = authManager,
                        dbManager
                    )
                }
            }
            if (showDialog) {
                AddSkillDialog(
                    email = email,
                    authManager = authManager,
                    dbManager = dbManager,
                    defaultUsername = defaultUsername,
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSkillDialog(
    email: String,
    authManager: AuthManager,
    dbManager: DatabaseManager,
    defaultUsername: String,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var desc by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val categoryOptions = listOf(
        CategoryItem("GENERAL", R.string.cat_general),
        CategoryItem("MUSIC", R.string.cat_music),
        CategoryItem("TECH", R.string.cat_tech),
        CategoryItem("LANG", R.string.cat_languages),
        CategoryItem("SPORTS", R.string.cat_sports)
    )

    var selectedCategoryId by rememberSaveable { mutableStateOf("GENERAL") }

    val selectedCategoryItem = remember(selectedCategoryId) {
        categoryOptions.firstOrNull { it.id == selectedCategoryId } ?: categoryOptions[0]
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_skill_title), color = EmeraldPrimary) },
        text = {
            BoxWithConstraints {
                val isTablet = maxWidth > 500.dp

                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                OutlinedTextField(
                                    value = stringResource(selectedCategoryItem.nameRes),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.category_label)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    categoryOptions.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(item.nameRes)) },
                                            onClick = {
                                                selectedCategoryId = item.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text(stringResource(R.string.skill_name_label)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = desc,
                                onValueChange = { desc = it },
                                label = { Text(stringResource(R.string.description_label)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 4
                            )
                        }
                    }
                } else {
                    Column {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = stringResource(selectedCategoryItem.nameRes),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.category_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                categoryOptions.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(item.nameRes)) },
                                        onClick = {
                                            selectedCategoryId = item.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(R.string.skill_name_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text(stringResource(R.string.description_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                onClick = {
                    if (title.isNotBlank()) {
                        val currentUser = authManager.getCurrentUser()
                        val currentUserId = currentUser?.uid ?: ""

                        val firestoreDb = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        firestoreDb.collection("users").document(currentUserId).get()
                            .addOnSuccessListener { document ->
                                val actualName = if (document != null && document.exists()) {
                                    document.getString("name") ?: document.getString("username") ?: defaultUsername
                                } else {
                                    currentUser?.displayName ?: defaultUsername
                                }

                                val newSkill = Skill(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = title,
                                    description = desc,
                                    authorId = currentUserId,
                                    authorName = actualName,
                                    contactEmail = email,
                                    category = selectedCategoryItem.id
                                )

                                dbManager.saveSkill(newSkill) { success ->
                                    if (success) {
                                        Log.d("SkillSwap", "Успешно запишување")
                                        onDismiss()
                                    } else {
                                        Log.d("SkillSwap", "Неуспешно запишување")
                                    }
                                }
                            }
                            .addOnFailureListener {
                                val newSkill = Skill(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = title,
                                    description = desc,
                                    authorId = currentUserId,
                                    authorName = currentUser?.displayName ?: defaultUsername,
                                    contactEmail = email,
                                    category = selectedCategoryItem.id
                                )
                                dbManager.saveSkill(newSkill) { onDismiss() }
                            }
                    }
                }
            ) { Text(stringResource(R.string.publish_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button), color = SlateSecondary)
            }
        }
    )
}


