package uklo.fikt.pmp.pmpproekt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uklo.fikt.pmp.pmpproekt.data.DatabaseManager
import uklo.fikt.pmp.pmpproekt.data.Skill
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary

@Composable
fun SkillFeed(dbManager: DatabaseManager){
    var skills by remember { mutableStateOf(listOf<Skill>()) }

    LaunchedEffect(Unit) {
        dbManager.getSkills { fetchedSkills ->
            skills = fetchedSkills
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(skills){ skill ->
            SkillCard(skill, dbManager)
        }
    }
}

@Composable
fun SkillCard(skill: Skill, dbManager: DatabaseManager) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable{
                dbManager.logSkillView(skill.title, skill.category)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors( containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = skill.category, color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = skill.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = skill.description, color = Color.Gray, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.skill_from) + skill.authorName , style = MaterialTheme.typography.labelSmall)
        }
    }
}