package uklo.fikt.pmp.pmpproekt

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uklo.fikt.pmp.pmpproekt.data.AppDatabase
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.data.CachedSkill
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Skill

data class CategoryItem(
    val id: String,
    val nameRes: Int
)

class PreferenceManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun trackInterest(categoryId: String) {
        if (categoryId == "ALL") return
        val currentCount = sharedPreferences.getInt(categoryId, 0)
        sharedPreferences.edit { putInt(categoryId, currentCount + 1) }
    }

    fun getMostInterestedCategory(): String {
        val categories = listOf("MUSIC", "TECH", "LANG", "SPORTS", "GENERAL")
        return categories.maxByOrNull { sharedPreferences.getInt(it, 0) } ?: "ALL"
    }
}

@Composable
fun SkillFeed(
    dbManager: DatabaseManager,
    authManager: AuthManager,
    onChatClick: (Skill) -> Unit
) {
    val categoryItems = listOf(
        CategoryItem("ALL", R.string.cat_all),
        CategoryItem("MUSIC", R.string.cat_music),
        CategoryItem("TECH", R.string.cat_tech),
        CategoryItem("LANG", R.string.cat_languages),
        CategoryItem("SPORTS", R.string.cat_sports),
        CategoryItem("GENERAL", R.string.cat_general)
    )
    val context = LocalContext.current
    val prefManager = remember { PreferenceManager(context) }
    val currentUser = authManager.getCurrentUser()
    val currentUserId = currentUser?.uid ?: ""

    // 1. Иницијализација на Room базата
    val database = remember { AppDatabase.getDatabase(context) }
    val skillDao = remember { database.skillDao() }

    // Специјален скоп за корутини за запишување во базата во позадина
    val coroutineScope = rememberCoroutineScope()

    // Слушање на Room базата во реално време (UI чита оттука)
    val cachedSkillsList by skillDao.getAllSkills().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    // 2. Позадинска синхронизација: Firestore -> Room
    LaunchedEffect(Unit) {
        dbManager.getSkills { fetchedSkills ->
            // Кога ќе стигнат новите вештини од Firestore, ги пакуваме за Room во IO нишка
            coroutineScope.launch(Dispatchers.IO) {
                val roomSkills = fetchedSkills.map { skill ->
                    CachedSkill(
                        id = skill.id,
                        title = skill.title,
                        description = skill.description,
                        category = skill.category,
                        authorName = skill.authorName,
                        authorId = skill.authorId,
                        contactEmail = skill.contactEmail,
                        likesCount = skill.likesCount
                    )
                }
                skillDao.clearAll()         // Ги чистиме старите
                skillDao.insertSkills(roomSkills) // Ги внесуваме новите
            }
        }
    }

    // 3. Конвертирање на кешираните Room објекти назад во Skill објекти за да пасуваат во AdvancedSkillCard
    val skills = cachedSkillsList.map { cached ->
        Skill(
            id = cached.id,
            title = cached.title,
            description = cached.description,
            category = cached.category,
            authorName = cached.authorName,
            authorId = cached.authorId,
            contactEmail = cached.contactEmail,
            likesCount = cached.likesCount,
            likedBy = emptyList() // Ова поле не го кешираме во Room, доволно ни е likesCount
        )
    }

    // Филтрирањето останува потполно исто, но сега работи над преточената листа од Room
    val filteredSkills = skills.filter { skill ->
        val matchesSearch = skill.title.contains(searchQuery, ignoreCase = true)
        val matchesCategory = if (selectedCategory == "ALL") {
            true
        } else {
            skill.category.uppercase() == selectedCategory ||
                    (selectedCategory == "TECH" && skill.category == "Програмирање") ||
                    (selectedCategory == "MUSIC" && skill.category == "Музика") ||
                    (selectedCategory == "LANG" && skill.category == "Јазици") ||
                    (selectedCategory == "SPORTS" && skill.category == "Спорт") ||
                    (selectedCategory == "GENERAL" && skill.category == "Општо")
        }
        matchesCategory && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(25.dp),
            singleLine = true,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoryItems) { item ->
                FilterChip(
                    selected = selectedCategory == item.id,
                    onClick = {
                        selectedCategory = item.id
                        prefManager.trackInterest(item.id)
                        dbManager.logSkillView("Filter_Category", item.id)
                    },
                    label = { Text(stringResource(item.nameRes)) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        if (filteredSkills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(text = stringResource(R.string.no_skills), color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSkills) { currentSkill ->
                    AdvancedSkillCard(
                        skill = currentSkill,
                        currentUserId = currentUserId,
                        onLikeClick = {
                            toggleLikeSkill(currentSkill, currentUserId, com.google.firebase.firestore.FirebaseFirestore.getInstance(), context)
                        },
                        onChatClick = {
                            if (currentSkill.authorId != currentUserId) {
                                onChatClick(currentSkill)
                            } else {
                                android.widget.Toast.makeText(context, "Ова е твоја вештина!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}