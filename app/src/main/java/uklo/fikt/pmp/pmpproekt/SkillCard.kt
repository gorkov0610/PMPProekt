package uklo.fikt.pmp.pmpproekt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uklo.fikt.pmp.pmpproekt.data.Skill
import uklo.fikt.pmp.pmpproekt.ui.theme.BackgroundGray
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import uklo.fikt.pmp.pmpproekt.ui.theme.RedCoral
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary

@Composable
fun SkillCard(
    skill: Skill,
    prefManager: PreferenceManager,
    onLikeClick: () -> Unit,
    onChatClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    var isLikedByMe by remember(skill.id) { mutableStateOf(prefManager.isSkillLiked(skill.id)) }
    val displayLikesCount = remember(skill.id) { mutableIntStateOf(skill.likesCount) }

    LaunchedEffect(skill.likesCount) {
        displayLikesCount.intValue = skill.likesCount
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Наслов
            Text(
                text = skill.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Опис
            Text(
                text = skill.description,
                fontSize = 14.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            val translatedCategory = remember(skill.category){
                getCategoryTranslation(skill.category, context)
            }

            // Мета податоци
            Text(
                text = stringResource(R.string.skill_metadata, skill.authorName, translatedCategory),
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BackgroundGray)

            // ДОЛНА ЛЕНТА СО КОПЧИЊА ЗА АКЦИЈА
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ЛЕВО: Лајк систем
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val nextLikedState = !isLikedByMe
                            prefManager.setSkillLiked(skill.id, nextLikedState)
                            isLikedByMe = nextLikedState

                            if (nextLikedState) {
                                displayLikesCount.intValue = skill.likesCount + 1
                            } else {
                                displayLikesCount.intValue = maxOf(0, skill.likesCount - 1)
                            }

                            onLikeClick()
                        }
                    ) {
                        Icon(
                            imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.desc_like),
                            tint = if (isLikedByMe) RedCoral else SlateSecondary
                        )
                    }
                    Text(
                        text = "${displayLikesCount.intValue}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isLikedByMe) RedCoral else SlateSecondary
                    )
                }

                // ДЕСНО: Паметна замена во зависност од тоа чиј е огласот
                if (onEditClick != null && onDeleteClick != null) {
                    // АВТОРСКИ ПРИКАЗ
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.desc_edit),
                                tint = EmeraldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.desc_delete),
                                tint = RedCoral
                            )
                        }
                    }
                } else {
                    // СТАНДАРДЕН ПРИКАЗ: Контакт опции
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                sendEmailIntent(context, skill.contactEmail, skill.title)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = stringResource(R.string.desc_gmail_contact),
                                tint = EmeraldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = onChatClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(stringResource(R.string.chat), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryTranslation(categoryId: String, context: android.content.Context): String {
    val resId = when (categoryId.uppercase()) {
        "GENERAL" -> R.string.cat_general
        "MUSIC" -> R.string.cat_music
        "TECH" -> R.string.cat_tech
        "LANG" -> R.string.cat_languages
        "SPORTS" -> R.string.cat_sports
        else -> R.string.cat_general
    }
    return context.getString(resId)
}