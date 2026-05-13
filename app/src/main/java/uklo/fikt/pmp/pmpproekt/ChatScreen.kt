package uklo.fikt.pmp.pmpproekt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Message
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.*

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
@Composable
fun ChatBubble(message: Message, currentUserId : String) {
    val isCurrentUser = message.senderId == currentUserId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        // Ги турка твоите пораки десно, туѓите лево
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // Сликата да биде порамнета со дното на пораката
    ) {
        // 1. АКО Е ТУЃА ПОРАКА: Прикажи слика од лево
        if (!isCurrentUser) {
            AsyncImage(
                model = message.senderPhotoUrl.ifEmpty { "https://cdn-icons-png.flaticon.com/512/149/149071.png" },
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 2. БАЛОНЧЕТО (Твојот постоечки Surface)
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 2.dp,
                bottomEnd = if (isCurrentUser) 2.dp else 16.dp
            ),
            color = if (isCurrentUser) EmeraldPrimary else Color(0xFFF1F1F1),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser) Color.White else Color.Black
                )
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(skillId: String, dbManager: DatabaseManager, authManager: AuthManager, authorName: String, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    val currentUser = authManager.getCurrentUser()
    val currentUserId = currentUser?.uid ?: "unknown"
    val currentUserName = currentUser?.displayName ?: stringResource(R.string.me)

    LaunchedEffect(skillId) {
        dbManager.listenForMessages(skillId) { messages = it }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = EmeraldPrimary, // Твојата зелена боја
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(64.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Разговор со $authorName",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // 2. ЛИСТА СО ПОРАКИ
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true // Најновите пораки да бидат долу до тастатурата
        ) {
            items(messages) { msg ->
                ChatBubble(msg, currentUserId)
            }
        }

        // 3. ТУКА ОДИ НОВОТО ПАРЧЕ КОД (Контејнерот за внес)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .navigationBarsPadding()
                    .imePadding(), // Ова е магијата што го крева полето над тастатурата
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.type_message)) },
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Копчето спакувано во Box за да не се меша со FAB (+)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary)
                        .clickable {
                            if (messageText.isNotBlank()) {
                                val newMessage = Message(
                                    senderId = currentUserId,
                                    senderPhotoUrl = currentUser?.photoUrl.toString(),
                                    text = messageText,
                                    senderName = currentUserName,
                                    timestamp = System.currentTimeMillis()
                                )
                                dbManager.sendMessage(skillId, newMessage)
                                messageText = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}