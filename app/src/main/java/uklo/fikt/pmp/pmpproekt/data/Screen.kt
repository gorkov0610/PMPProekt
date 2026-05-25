package uklo.fikt.pmp.pmpproekt.data

import android.net.Uri

sealed class Screen(val route: String) {
    object Feed : Screen("feed")
    object Inbox : Screen("inbox")
    object Profile : Screen("profile")
    object MySkills : Screen("my_skills")
    object Login : Screen("login")
    object LikedSkills : Screen("liked_skills")
    object Chat : Screen("chat/{receiverId}/{authorName}") {
        fun createRoute(receiverId: String, authorName: String): String {
            val encodedName = Uri.encode(authorName)
            return "chat/$receiverId/$encodedName"
        }
    }
}