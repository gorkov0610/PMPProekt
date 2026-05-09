package uklo.fikt.pmp.pmpproekt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class Skill(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description : String,
    val category : String,
    val authorName : String,
    val contactEmail : String,
    val isFavorite : Boolean = false
)