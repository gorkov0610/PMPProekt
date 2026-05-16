package uklo.fikt.pmp.pmpproekt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Message
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
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
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser) {
            // Сликата на другиот корисник
            AsyncImage(
                model = message.senderPhotoUrl,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Името
                Text(text = message.senderName, fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                // ВРЕМЕТО - еве каде се користи функцијата!
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCurrentUser) EmeraldPrimary else Color(0xFFE9E9EB)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = if (isCurrentUser) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun ChatScreen(
    receiverId: String,
    receiverName: String,
    authManager: AuthManager,
    databaseManager: DatabaseManager, // ГО ДОДАВАМЕ МЕНАЏЕРОТ ТУКА
    onBack: () -> Unit
) {
    val currentUser = authManager.getCurrentUser()
    val currentUserId = currentUser?.uid ?: ""
    val currentUserName = currentUser?.displayName ?: "Корисник"
    val context = LocalContext.current

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var textState by remember { mutableStateOf("") }

    // Генерирање на уникатен ID за собата (ист за двајцата корисници)
    val chatRoomId = remember(receiverId) {
        if (currentUserId < receiverId) "${currentUserId}_${receiverId}" else "${receiverId}_${currentUserId}"
    }
    var isFirstLoad by remember { mutableStateOf(true) }
    // Слушање за нови пораки преку DatabaseManager во реално време (Внатре во ChatScreen)
    LaunchedEffect(chatRoomId) {
        databaseManager.listenForMessages(chatRoomId) { updatedMessages ->
            // Само ги ажурираме пораките за да се нацртаат во LazyColumn-от на екранот
            messages = updatedMessages

            // Ова веќе не ни треба за нотификации, но ако ти зависи друга UI логика од него, можеш да го оставиш:
            isFirstLoad = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Наслов со името на примачот
        Text(
            text = "Разговор со $receiverName",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Листа на пораки
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = false // Пораките ќе се полнат убаво од горе на долу
        ) {
            items(messages) { msg ->
                ChatBubble(
                    message = msg,
                    isCurrentUser = msg.senderId == currentUserId,
                )
            }
        }

        // Поле за внес на порака
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Напиши порака...") },
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

                        // Го повикуваме средениот метод од DatabaseManager кој прави СЀ
                        databaseManager.sendMessage(
                            chatRoomId = chatRoomId,
                            message = newMessage,
                            receiverName = receiverName,
                            currentUserName = currentUserName
                        )

                        textState = "" // Го чистиме полето
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Прати",
                    tint = EmeraldPrimary
                )
            }
        }
    }
}