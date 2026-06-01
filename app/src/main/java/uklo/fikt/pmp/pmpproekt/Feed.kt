package uklo.fikt.pmp.pmpproekt

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore.getInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uklo.fikt.pmp.pmpproekt.data.AppDatabase
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.data.CachedSkill
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Skill
import uklo.fikt.pmp.pmpproekt.data.toggleLikeSkill
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldLight
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class CategoryItem(
    val id: String,
    val nameRes: Int
)
class PreferenceManager(context: Context) {
    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences("user_interest", Context.MODE_PRIVATE)
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
    private val remoteConfig = Firebase.remoteConfig.apply {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        setConfigSettingsAsync(configSettings)
        setDefaultsAsync(mapOf("default_category" to "ALL"))
    }
    fun setSkillLiked(skillId: String, isLiked: Boolean) {
        sharedPreferences.edit { putBoolean("like_$skillId", isLiked) }
        val bundle = Bundle().apply{
            putString(FirebaseAnalytics.Param.ITEM_ID, skillId)
            putString("action_type", if(isLiked) "LIKED" else "UNLIKED")
        }
        firebaseAnalytics.logEvent("skill_like_changed", bundle)
    }

    fun isSkillLiked(skillId: String): Boolean {
        return sharedPreferences.getBoolean("like_$skillId", false)
    }
    fun logFilterSelection(categoryId: String) {
        val cleanCategory = categoryId.uppercase().trim()
        val bundle = Bundle().apply {
            putString("filter_value", categoryId)
        }
        firebaseAnalytics.logEvent("filter_chip_selected", bundle)
        firebaseAnalytics.setUserProperty("user_interest", cleanCategory)
        Log.d("PreferenceManager", "logged user_interest=$cleanCategory")
    }
    fun syncRemoteConfig(onSyncComplete: (String) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSyncComplete(getMostInterestedCategory())
            }
        }
    }

    // Помошна функција за мапирање на македонските текстуални вредности од базата кон константите
    fun normalizeCategory(rawCategory: String): String {
        return when (rawCategory.uppercase()) {
            "TECH", "ПРОГРАМИРАЊЕ" -> "TECH"
            "MUSIC", "МУЗИКА" -> "MUSIC"
            "LANG", "ЈАЗИЦИ" -> "LANG"
            "SPORTS", "СПОРТ" -> "SPORTS"
            "GENERAL", "ОПШТО" -> "GENERAL"
            else -> "ALL"
        }
    }

    fun getMostInterestedCategory(): String {
        val remoteValue = remoteConfig.getString("default_category")
        if (remoteValue.isEmpty()) {
            return "ALL"
        }
        return normalizeCategory(remoteValue)
    }
}



// Функција за проверка дали уредот има активна интернет конекција
fun isOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}

@SuppressLint("LocalContextGetResourceValueCall")
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


    val currentUserId = authManager.getCurrentUser()?.uid ?: ""

    val database = remember { AppDatabase.getDatabase(context) }
    val skillDao = remember { database.skillDao() }
    val coroutineScope = rememberCoroutineScope()

    var liveSkills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        selectedCategory = prefManager.getMostInterestedCategory()
        if (isOnline(context)) {
            prefManager.syncRemoteConfig { updatedCategory ->
                selectedCategory = updatedCategory
                Log.d("RemoteConfigSync", "Новата категорија е успешна повлечена: $updatedCategory")
            }
        }
    }

    fun saveToLocalCache(skillsToCache: List<Skill>){
        coroutineScope.launch(Dispatchers.IO) {
            if (skillsToCache.isNotEmpty()) {
                val roomSkills = skillsToCache.map { skill ->
                    CachedSkill(
                        id = skill.id,
                        title = skill.title,
                        description = skill.description,
                        category = prefManager.normalizeCategory(skill.category),
                        authorName = skill.authorName,
                        authorId = skill.authorId,
                        contactEmail = skill.contactEmail,
                        likesCount = skill.likesCount
                    )
                }
                skillDao.clearAll()
                skillDao.insertSkills(roomSkills)
                Log.d("RoomCache", "Податоците се успешно зачувани во локалната база.")
            }
        }
    }

    DisposableEffect(Unit) {
        var listenerRegistration: ListenerRegistration? = null

        if (isOnline(context)) {
            listenerRegistration = dbManager.getSkills(
                onResult = { fetchedSkills ->
                    liveSkills = fetchedSkills
                    saveToLocalCache(fetchedSkills)
                },
                onFailure = { e ->
                    Log.e("FirebaseFetch", "Грешка при влечење огласи", e)
                    Toast.makeText(context, "Грешка при вчитување на податоците.", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, context.getString(R.string.label_offline), Toast.LENGTH_SHORT).show()
            coroutineScope.launch(Dispatchers.IO) {
                val cachedList = skillDao.getAllSkills().first()
                val mappedSkills = cachedList.map { cached ->
                    val isLikedLocally = prefManager.isSkillLiked(cached.id)
                    Skill(
                        id = cached.id,
                        title = cached.title,
                        description = cached.description,
                        category = cached.category,
                        authorName = cached.authorName,
                        authorId = cached.authorId,
                        contactEmail = cached.contactEmail,
                        likesCount = cached.likesCount,
                        likedBy = if (isLikedLocally) listOf(currentUserId) else emptyList()
                    )
                }
                withContext(Dispatchers.Main){
                    liveSkills = mappedSkills
                }
            }
        }

        onDispose {
            listenerRegistration?.remove()
        }
    }


    val filteredSkills = liveSkills.filter { skill ->
        val matchesSearch = skill.title.contains(searchQuery, ignoreCase = true)
        val normalizedSkillCat = prefManager.normalizeCategory(skill.category)
        val matchesCategory = if (selectedCategory == "ALL") true else normalizedSkillCat == selectedCategory
        matchesCategory && matchesSearch
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletOrLandscape = maxWidth > 600.dp

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
                            prefManager.logFilterSelection(item.id)
                            dbManager.logSkillView("Filter_Category", item.id)
                        },
                        label = { Text(stringResource(item.nameRes)) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            if (filteredSkills.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_skills), color = SlateSecondary)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    state = pullToRefreshState,
                    onRefresh = {
                        if(isOnline(context)){
                            isRefreshing = true
                            dbManager.getSkills(
                                onResult = { fetchedSkills ->
                                    liveSkills = fetchedSkills
                                    saveToLocalCache(fetchedSkills)
                                    isRefreshing = false
                                },
                                onFailure = { e ->
                                    Log.e("FirebaseRefresh", "Грешка при рефреш", e)
                                    isRefreshing = false
                                }
                            )
                        } else {
                            Toast.makeText(context, context.getString(R.string.label_offline_refresh), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            containerColor = MaterialTheme.colorScheme.surface,
                            color = EmeraldLight,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                ){
                    val contentPadding = PaddingValues(16.dp)

                    if (isTabletOrLandscape) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = contentPadding,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredSkills) { currentSkill ->
                                SkillCard(
                                    skill = currentSkill,
                                    onLikeClick = {
                                        val isLiked = currentSkill.likedBy.contains(currentUserId) || prefManager.isSkillLiked(currentSkill.id)
                                        liveSkills = liveSkills.map {
                                            if (it.id == currentSkill.id) {
                                                it.copy(
                                                    likesCount = if (isLiked) it.likesCount - 1 else it.likesCount + 1,
                                                    likedBy = if (isLiked) emptyList() else listOf(currentUserId)
                                                )
                                            } else it
                                        }

                                        toggleLikeSkill(currentSkill, currentUserId, getInstance(), context)
                                    },
                                    prefManager = prefManager,
                                    onChatClick = {
                                        if (currentSkill.authorId != currentUserId) {
                                            onChatClick(currentSkill)
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.error_own_skill), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredSkills) { currentSkill ->
                                SkillCard(
                                    skill = currentSkill,
                                    onLikeClick = {
                                        val isLiked = currentSkill.likedBy.contains(currentUserId) || prefManager.isSkillLiked(currentSkill.id)
                                        liveSkills = liveSkills.map {
                                            if (it.id == currentSkill.id) {
                                                it.copy(
                                                    likesCount = if (isLiked) it.likesCount - 1 else it.likesCount + 1,
                                                    likedBy = if (isLiked) emptyList() else listOf(currentUserId)
                                                )
                                            } else it
                                        }

                                        toggleLikeSkill(currentSkill, currentUserId, getInstance(), context)
                                    },
                                    prefManager = prefManager,
                                    onChatClick = {
                                        if (currentSkill.authorId != currentUserId) {
                                            onChatClick(currentSkill)
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.error_own_skill), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
