package uklo.fikt.pmp.pmpproekt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class Skill(
    @PrimaryKey
    val id: String = "",
    val title: String = "",
    val description : String = "",
    val category : String = "",
    val authorName : String = "",
    val contactEmail : String = "",
    val isFavorite : Boolean = false
)