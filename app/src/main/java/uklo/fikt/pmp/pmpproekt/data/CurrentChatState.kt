package uklo.fikt.pmp.pmpproekt.data

object CurrentChatState {
    @Volatile
    var activeChatUserId: String? = null
}