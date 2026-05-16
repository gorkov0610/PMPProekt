package uklo.fikt.pmp.pmpproekt.data

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicture: String = "",
    val fcmToken: String = ""
)
