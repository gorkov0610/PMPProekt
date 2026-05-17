package uklo.fikt.pmp.pmpproekt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import uklo.fikt.pmp.pmpproekt.data.AuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    authManager: AuthManager,
    onChatClick: (String, String) -> Unit // Враќа receiverId и receiverName при клик
) {
    val currentUserId = authManager.getCurrentUser()?.uid ?: ""
    var chatList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val db = FirebaseFirestore.getInstance()

    // Слушај во реално време за сите соби каде моменталниот корисник е во "participants"
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        // Ги подредуваме разговорите по најнова порака (најголем timestamp на врвот)
                        chatList = snapshot.documents
                            .mapNotNull { it.data }
                            .sortedByDescending { it["timestamp"] as? Long ?: 0L }
                    }
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Мои разговори 💬",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (chatList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Немате активни разговори.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(chatList) { chat ->
                    val participants = chat["participants"] as? List<String> ?: emptyList()
                    // Најди го ID-то на другиот корисник
                    val receiverId = participants.firstOrNull { it != currentUserId } ?: ""

                    val userNames = chat["userNames"] as? Map<String, String> ?: emptyMap()
                    // Земи го името на другиот корисник
                    val receiverName = userNames[receiverId] ?: "Корисник"
                    val lastMessage = chat["lastMessage"] as? String ?: "Нова порака..."
                    val timestamp = chat["timestamp"] as? Long ?: 0L

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onChatClick(receiverId, receiverName) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = receiverName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (timestamp > 0L) {
                                    Text(
                                        text = formatTimestamp(timestamp),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = lastMessage,
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}