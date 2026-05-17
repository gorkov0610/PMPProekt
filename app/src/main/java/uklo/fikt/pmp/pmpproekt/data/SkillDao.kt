package uklo.fikt.pmp.pmpproekt.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM cached_skills")
    fun getAllSkills(): Flow<List<CachedSkill>> // Преку Flow, Compose веднаш ќе ги црта промените

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Ако веќе постои Skill со тоа ID, го пребришува со најновиот од Firebase
    suspend fun insertSkills(skills: List<CachedSkill>)

    @Query("DELETE FROM cached_skills")
    suspend fun clearAll()
}