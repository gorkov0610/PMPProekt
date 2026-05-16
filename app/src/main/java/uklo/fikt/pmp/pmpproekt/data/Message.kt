package uklo.fikt.pmp.pmpproekt.data

data class Message(
    val senderId : String = "",
    val receiverId : String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val senderPhotoUrl : String = "",
    val senderName: String = ""
)
