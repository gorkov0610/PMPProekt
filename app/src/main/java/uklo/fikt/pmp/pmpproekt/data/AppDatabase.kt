package uklo.fikt.pmp.pmpproekt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [Skill::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun skillDao() : SkillDao

    companion object{
        @Volatile
        private var INSTANCE : AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "skill_swap_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}