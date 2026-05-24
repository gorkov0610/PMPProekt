package uklo.fikt.pmp.pmpproekt.data

sealed class Screen(val route: String) {
    object Feed : Screen("feed")
    object Inbox : Screen("inbox")
    object Profile : Screen("profile")
    object MySkills : Screen("my_skills")
    object Login : Screen("login")
    object LikedSkills : Screen("liked_skills")
    object Chat : Screen("chat/{receiverId}/{authorName}") {
        fun createRoute(receiverId: String, authorName: String) = "chat/$receiverId/$authorName"
    }
}