package uklo.fikt.pmp.pmpproekt.data

data class Message(
    val senderId : String = "",
    val receiverId : String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val senderPhotoUrl : String = "",
    val senderName: String = ""
){
    constructor() : this("","","",0L,"","")
}
