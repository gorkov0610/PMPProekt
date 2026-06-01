package uklo.fikt.pmp.pmpproekt

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateDark
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    authManager: AuthManager,
    onChatClick: (String, String) -> Unit
) {
    val currentUserId = authManager.getCurrentUser()?.uid ?: ""
    var chatList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val db = FirebaseFirestore.getInstance()

    // Слушај во реално време за сите соби каде моменталниот корисник е во "participants"
    DisposableEffect(currentUserId) {
        var listenerRegistration: ListenerRegistration? = null
        if (currentUserId.isNotEmpty()) {
            listenerRegistration = db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("InboxScreen", "Грешка при слушање соби", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        chatList = snapshot.documents
                            .mapNotNull { it.data }
                            .sortedByDescending { it["timestamp"] as? Long ?: 0L }
                    }
                }
        }

        onDispose {
            listenerRegistration?.remove()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .widthIn(max = 600.dp)
        ) {
            if (chatList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_chats), color = SlateSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chatList) { chat ->
                        val participants = (chat["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        // Најди го ID-то на другиот корисник
                        val receiverId = participants.firstOrNull { it != currentUserId } ?: ""

                        val userNames = (chat["userNames"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString()} ?: emptyMap()
                        // Земи го името на другиот корисник
                        val receiverName =
                            userNames[receiverId] ?: stringResource(R.string.user)
                        val lastMessage =
                            chat["lastMessage"] as? String
                                ?: stringResource(R.string.msg_title_reserve)
                        val timestamp : Date? = when(val rawTimestamp = chat["timestamp"]){
                            is Timestamp -> rawTimestamp.toDate()
                            is Date -> rawTimestamp
                            else -> null
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    onChatClick(receiverId, receiverName)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.5f
                                )
                            )
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
                                    if(timestamp != null){
                                        Text(
                                            text = formatTimestamp(timestamp),
                                            fontSize = 12.sp,
                                            color = SlateSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = lastMessage,
                                    fontSize = 14.sp,
                                    color = SlateDark,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}