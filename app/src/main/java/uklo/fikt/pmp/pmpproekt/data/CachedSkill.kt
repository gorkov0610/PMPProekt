package uklo.fikt.pmp.pmpproekt.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_skills")
data class CachedSkill(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id : String = "",
    @ColumnInfo(name = "title")
    val title : String = "",
    @ColumnInfo(name = "description")
    val description : String = "",
    @ColumnInfo(name = "category")
    val category : String = "",
    @ColumnInfo(name = "author_name")
    val authorName : String = "",
    @ColumnInfo(name = "author_id")
    val authorId : String = "",
    @ColumnInfo(name = "contact_email")
    val contactEmail : String = "",
    @ColumnInfo(name = "likes_count")
    val likesCount : Int = 0,
)
