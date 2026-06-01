package uklo.fikt.pmp.pmpproekt.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    val senderId : String = "",
    val receiverId : String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    val senderPhotoUrl : String = "",
    val senderName: String = ""
)
