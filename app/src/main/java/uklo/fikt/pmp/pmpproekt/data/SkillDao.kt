package uklo.fikt.pmp.pmpproekt.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills")
    fun getAllSkills() : Flow<List<Skill>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertSkill(skill: Skill)

    @Delete
    suspend fun deleteSkill(skill: Skill)

    @Query("SELECT * FROM skills WHERE isFavorite = 1")
    fun getFavoriteSkills() : Flow<List<Skill>>
}