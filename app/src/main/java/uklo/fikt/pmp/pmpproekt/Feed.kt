package uklo.fikt.pmp.pmpproekt

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Skill
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import androidx.core.net.toUri
import androidx.core.content.edit

data class CategoryItem(
    val id: String,
    val nameRes: Int
)
class PreferenceManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // Зголеми го бројот на кликови за одредена категорија
    fun trackInterest(categoryId: String) {
        if (categoryId == "ALL") return
        val currentCount = sharedPreferences.getInt(categoryId, 0)
        sharedPreferences.edit { putInt(categoryId, currentCount + 1) }
    }

    // Најди ја категоријата со најмногу кликови
    fun getMostInterestedCategory(): String {
        val categories = listOf("MUSIC", "TECH", "LANG", "SPORTS", "GENERAL")
        return categories.maxByOrNull { sharedPreferences.getInt(it, 0) } ?: "ALL"
    }
}
@Composable
fun SkillFeed(dbManager: DatabaseManager){

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

    var skills by remember { mutableStateOf(listOf<Skill>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        dbManager.getSkills { fetchedSkills ->
            skills = fetchedSkills
        }
    }

    val filteredSkills = skills.filter{ skill ->
        val matchesSearch = skill.title.contains(searchQuery, ignoreCase = true)
        val matchesCategory = if (selectedCategory == "ALL") {
            true
        } else {
            // Проверува дали се совпаѓа со ID-то (TECH) ИЛИ со името (Програмирање)
            // Ова е привремено решение додека не ја исчистиш базата
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
        if(filteredSkills.isEmpty()){
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(text = stringResource(R.string.no_skills), color = Color.Gray)
            }
        }else{
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSkills) { skill ->
                    SkillCard(skill, dbManager)
                }
            }
        }
    }
}

@Composable
fun SkillCard(skill: Skill, dbManager: DatabaseManager) {
    val context = LocalContext.current
    val emailSubject = stringResource(R.string.email_subject, skill.title)
    val emailBody = stringResource(R.string.email_body, skill.authorName)
    val noEmailError = stringResource(R.string.no_email_client)
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable{
                dbManager.logSkillView(skill.title, skill.category)

                val uriString = "mailto:${skill.contactEmail}?" +
                        "subject=${Uri.encode(emailSubject)}&" +
                        "body=${Uri.encode(emailBody)}"
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(uriString)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, noEmailError, Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors( containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val categoryDisplay = when (skill.category) {
                "GENERAL" -> stringResource(R.string.cat_general)
                "MUSIC" -> stringResource(R.string.cat_music)
                "TECH" -> stringResource(R.string.cat_tech)
                "LANG" -> stringResource(R.string.cat_languages)
                "SPORTS" -> stringResource(R.string.cat_sports)
                else -> skill.category // Ако е „Општо“ (стариот запис), прикажи го како што е
            }
            Text(text = categoryDisplay, color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = skill.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = skill.description, color = Color.Gray, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.skill_from) + skill.authorName , style = MaterialTheme.typography.labelSmall)
        }
    }
}