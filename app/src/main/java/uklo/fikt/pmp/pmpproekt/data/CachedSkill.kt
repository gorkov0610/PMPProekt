package uklo.fikt.pmp.pmpproekt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_skills")
data class CachedSkill(
    @PrimaryKey val id : String,
    val title : String,
    val description : String,
    val category : String,
    val authorName : String,
    val authorId : String,
    val contactEmail : String,
    val likesCount : Int,
)
