package uklo.fikt.pmp.pmpproekt

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Message
import uklo.fikt.pmp.pmpproekt.ui.theme.Black
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldLight
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateDark
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary
import uklo.fikt.pmp.pmpproekt.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun ChatBubble(message: Message, isCurrentUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser) {
            AsyncImage(
                model = message.senderPhotoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = message.senderName, fontSize = 10.sp, color = SlateDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 10.sp,
                    color = SlateSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCurrentUser) EmeraldPrimary else EmeraldLight
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = if (isCurrentUser) White else Black
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ChatScreen(
    receiverId: String,
    receiverName: String,
    authManager: AuthManager,
    databaseManager: DatabaseManager
) {
    val context = LocalContext.current
    val currentUser = authManager.getCurrentUser()
    val currentUserId = currentUser?.uid ?: ""
    var currentUserName by remember { mutableStateOf(context.getString(R.string.chat_loading)) }
    var receiverPhotoUrl by remember { mutableStateOf<String?>(null) }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var textState by remember { mutableStateOf("") }

    val chatRoomId = remember(receiverId) {
        if (currentUserId < receiverId) "${currentUserId}_${receiverId}" else "${receiverId}_${currentUserId}"
    }

    LaunchedEffect(chatRoomId) {
        databaseManager.listenForMessages(chatRoomId) { updatedMessages ->
            messages = updatedMessages
        }
    }

    LaunchedEffect(currentUserId, receiverId) {
        val db = FirebaseFirestore.getInstance()
        if (currentUserId.isNotEmpty()) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        currentUserName = document.getString("name") ?: document.getString("username") ?: context.getString(R.string.user)
                    }
                }
        }
        if (receiverId.isNotEmpty()) {
            db.collection("users").document(receiverId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Провери дали во твојата 'users' колекција клучот се вика "photoUrl" или "profilePicture"
                        receiverPhotoUrl = document.getString("profilePicture")
                    }
                }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentAlignment = Alignment.TopCenter
    ){
        Column(
            modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 600.dp)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f).imeNestedScroll(),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    val isCurrentUser = msg.senderId == currentUserId

                    val displayPhotoUrl = if (isCurrentUser) {
                        msg.senderPhotoUrl
                    } else {
                        receiverPhotoUrl ?: msg.senderPhotoUrl
                    }

                    val updatedMsg = msg.copy(senderPhotoUrl = displayPhotoUrl)
                    ChatBubble(
                        message = updatedMsg,
                        isCurrentUser = isCurrentUser,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.type_message)) },
                    shape = RoundedCornerShape(24.dp)
                )

                IconButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            val newMessage = Message(
                                senderId = currentUserId,
                                receiverId = receiverId,
                                text = textState,
                                timestamp = System.currentTimeMillis(),
                                senderName = currentUserName,
                                senderPhotoUrl = currentUser?.photoUrl?.toString() ?: ""
                            )

                            databaseManager.sendMessage(
                                chatRoomId = chatRoomId,
                                message = newMessage,
                                receiverName = receiverName,
                                currentUserName = currentUserName
                            )

                            textState = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.desc_send),
                        tint = EmeraldPrimary
                    )
                }
            }
        }
    }
}