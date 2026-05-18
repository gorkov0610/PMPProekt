package uklo.fikt.pmp.pmpproekt

import android.content.Context
import android.os.Bundle
import android.widget.Toast
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
import com.google.firebase.analytics.FirebaseAnalytics
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
        val mapCategory = normalizeCategory(categoryId)
        val currentCount = sharedPreferences.getInt(mapCategory, 0)
        sharedPreferences.edit { putInt(mapCategory, currentCount + 1) }
    }

    fun getMostInterestedCategory(): String {
        val categories = listOf("MUSIC", "TECH", "LANG", "SPORTS", "GENERAL")
        return categories.maxByOrNull { sharedPreferences.getInt(it, 0) } ?: "ALL"
    }

    // Помошна функција за мапирање на македонските текстуални вредности од базата кон константите
    fun normalizeCategory(rawCategory: String): String {
        return when (rawCategory.uppercase()) {
            "TECH", "ПРОГРАМИРАЊЕ" -> "TECH"
            "MUSIC", "МУЗИКА" -> "MUSIC"
            "LANG", "ЈАЗИЦИ" -> "LANG"
            "SPORTS", "СПОРТ" -> "SPORTS"
            "GENERAL", "ОПШТО" -> "GENERAL"
            else -> "GENERAL"
        }
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
    val analytics = remember { FirebaseAnalytics.getInstance(context) }

    val currentUser = authManager.getCurrentUser()
    val currentUserId = currentUser?.uid ?: ""

    val database = remember { AppDatabase.getDatabase(context) }
    val skillDao = remember { database.skillDao() }
    val coroutineScope = rememberCoroutineScope()

    val cachedSkillsList by skillDao.getAllSkills().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }

    // ПАМЕТНО ПОСТАВУВАЊЕ: Се вчитава категоријата која корисникот највеќе ја набљудувал!
    var selectedCategory by remember { mutableStateOf(prefManager.getMostInterestedCategory()) }

    LaunchedEffect(Unit) {
        dbManager.getSkills { fetchedSkills ->
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
                skillDao.clearAll()
                skillDao.insertSkills(roomSkills)
            }
        }
    }

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
            likedBy = emptyList()
        )
    }

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
                        // Корисникот експлицитно менува филтер
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredSkills) { currentSkill ->
                    AdvancedSkillCard(
                        skill = currentSkill,
                        currentUserId = currentUserId,
                        onLikeClick = {
                            toggleLikeSkill(currentSkill, currentUserId, com.google.firebase.firestore.FirebaseFirestore.getInstance(), context)
                        },
                        onChatClick = {
                            if (currentSkill.authorId != currentUserId) {
                                // 1. Локално бележиме интерес за категоријата на овој оглас
                                val mappedCat = prefManager.normalizeCategory(currentSkill.category)
                                prefManager.trackInterest(mappedCat)

                                // 2. Праќаме настан до Firebase Analytics дека е отворен ЧЕТ
                                val bundle = Bundle().apply {
                                    putString(FirebaseAnalytics.Param.ITEM_ID, currentSkill.id)
                                    putString(FirebaseAnalytics.Param.ITEM_NAME, currentSkill.title)
                                    putString(FirebaseAnalytics.Param.ITEM_CATEGORY, mappedCat)
                                    putString("interaction_type", "CHAT_INITIATED")
                                }
                                analytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM, bundle)

                                onChatClick(currentSkill)
                            } else {
                                Toast.makeText(context, "Ова е твоја вештина!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}