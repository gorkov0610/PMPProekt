package uklo.fikt.pmp.pmpproekt

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Skill
import uklo.fikt.pmp.pmpproekt.data.toggleLikeSkill
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import java.net.URLEncoder.encode


@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun LikedSkillsScreen(
    uid: String,
    prefManager : PreferenceManager,
    databaseManager: DatabaseManager,
    onChatClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    var likedSkills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            databaseManager.getLikedSkills(
                uid = uid,
                onResult = { skills ->
                    likedSkills = skills
                    isLoading = false
                },
                onFailure = { e ->
                    isLoading = false
                    Toast.makeText(context, context.getString(R.string.error, e.localizedMessage ?: context.getString(R.string.error_unknown)), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldPrimary)
        }
    } else {
        if (likedSkills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.label_no_liked_posts))
            }
        } else {
            if (isTablet) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(likedSkills) { skill ->
                        SkillCard(
                            skill = skill,
                            onLikeClick = {
                                toggleLikeSkill(skill, uid, db, context)

                                likedSkills = likedSkills.filter { it.id != skill.id }
                            },
                            prefManager = prefManager,
                            onChatClick = {
                                val encodedName = try {
                                    encode(skill.authorName, "UTF-8")
                                }catch (e: Exception){
                                    skill.authorName
                                }
                                onChatClick(skill.authorId, encodedName)
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(likedSkills) { skill ->
                        SkillCard(
                            skill = skill,
                            onLikeClick = {
                                toggleLikeSkill(skill, uid, db, context)
                                likedSkills = likedSkills.filter { it.id != skill.id }
                            },
                            prefManager = prefManager,
                            onChatClick = {
                                val encodedName = try {
                                    encode(skill.authorName, "UTF-8")
                                }catch (e: Exception){
                                    skill.authorName
                                }
                                onChatClick(skill.authorId, encodedName)
                            }
                        )
                    }
                }
            }
        }
    }
}