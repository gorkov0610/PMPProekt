package uklo.fikt.pmp.pmpproekt

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import uklo.fikt.pmp.pmpproekt.data.Skill
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun UserSkillsScreen(
    uid: String,
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    var mySkills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val prefManager = remember { PreferenceManager(context) }

    // Состојби за дијалогот за измена
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var editSkillId by rememberSaveable { mutableStateOf("") }
    var editTitle by rememberSaveable { mutableStateOf("") }
    var editDescription by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            db.collection("skills")
                .whereEqualTo("authorId", uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    mySkills = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Skill::class.java)?.copy(id = doc.id)
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, context.getString(R.string.error_unknown), Toast.LENGTH_SHORT).show()
                }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldPrimary)
        }
    } else {
        if (mySkills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.no_skills))
            }
        } else {
            val renderSkillCard: @Composable (Skill) -> Unit = { skill ->
                SkillCard(
                    skill = skill,
                    prefManager = prefManager,
                    onLikeClick = {},
                    onChatClick = {},
                    onEditClick = {
                        editSkillId = skill.id
                        editTitle = skill.title
                        editDescription = skill.description
                        showEditDialog = true
                    },
                    onDeleteClick = {
                        db.collection("skills").document(skill.id).delete()
                            .addOnSuccessListener {
                                mySkills = mySkills.filter { it.id != skill.id }
                                Toast.makeText(context, context.getString(R.string.label_delete_post_success), Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, context.getString(R.string.error, e.localizedMessage ?: context.getString(R.string.error_unknown)), Toast.LENGTH_SHORT).show()
                            }
                    }
                )
            }
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTablet = maxWidth >= 600.dp // Сега користиме .dp директно!
                val contentPadding = PaddingValues(16.dp)

                if (isTablet) {
                    // 🖥️ ТАБЛЕТ РЕЖИМ: Мрежа со 2 колони
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(mySkills) { skill ->
                            renderSkillCard(skill)
                        }
                    }
                } else {
                    // 📱 ТЕЛЕФОН РЕЖИМ: Стандардна една колона
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mySkills) { skill ->
                            renderSkillCard(skill)
                        }
                    }
                }
            }
        }
    }

    // ДИЈАЛОГ ЗА УРЕДУВАЊЕ
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_post), fontWeight = FontWeight.Bold) }, // Додај во strings.xml или смени со чист текст по желба
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(stringResource(R.string.skill_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text(stringResource(R.string.description_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    onClick = {
                        if (editTitle.isNotBlank() && editDescription.isNotBlank()) {
                            val updatedData = mapOf(
                                "title" to editTitle,
                                "description" to editDescription
                            )

                            db.collection("skills").document(editSkillId)
                                .update(updatedData)
                                .addOnSuccessListener {
                                    mySkills = mySkills.map {
                                        if (it.id == editSkillId) {
                                            it.copy(title = editTitle, description = editDescription)
                                        } else it
                                    }
                                    showEditDialog = false
                                    Toast.makeText(context, context.getString(R.string.label_edit_post_success), Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {e ->
                                    Toast.makeText(context, context.getString(R.string.error, e.localizedMessage ?: context.getString(R.string.error_unknown)), Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, context.getString(R.string.error_empty_field), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.save_changes))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}