package uklo.fikt.pmp.pmpproekt.data

data class Message(
    val senderId : String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String = ""
)
