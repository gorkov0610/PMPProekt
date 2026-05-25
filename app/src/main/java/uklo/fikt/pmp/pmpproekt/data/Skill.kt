package uklo.fikt.pmp.pmpproekt.data

data class Skill(
    val id: String = "",
    val title: String = "",
    val description : String = "",
    val category : String = "",
    val authorName : String = "",
    val authorId : String = "",
    val contactEmail : String = "",
    val likesCount : Int = 0,
    val likedBy : List<String> = emptyList()
)

fun Skill.toCachedSkill(): CachedSkill {
    return CachedSkill(
        id = this.id,
        title = this.title,
        description = this.description,
        category = this.category,
        authorName = this.authorName,
        authorId = this.authorId,
        contactEmail = this.contactEmail,
        likesCount = this.likesCount
    )
}