package uklo.fikt.pmp.pmpproekt.data

sealed class Screen(val route: String) {
    object Feed : Screen("feed")
    object Chat : Screen("chat/{skillId}/{authorName}") {
        fun createRoute(skillId: String, authorName: String) = "chat/$skillId/$authorName"
    }
}