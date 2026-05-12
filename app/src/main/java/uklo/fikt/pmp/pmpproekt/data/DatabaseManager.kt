package uklo.fikt.pmp.pmpproekt.data


import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects

class DatabaseManager {
    private val db = FirebaseFirestore.getInstance()
    private val skillsCollection = db.collection("skills")
    val analytics = Firebase.analytics

    fun logSkillView(skillTitle : String, category: String){
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT){
            param(FirebaseAnalytics.Param.ITEM_ID, skillTitle)
            param(FirebaseAnalytics.Param.CONTENT_TYPE, category)
        }
    }
    fun saveSkill(skill: Skill, onComplete: (Boolean) -> Unit){
        val docRef = skillsCollection.document()
        val skillWithId = skill.copy(id = docRef.id)

        docRef.set(skillWithId)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    fun getSkills(onResult: (List<Skill>) -> Unit){
        skillsCollection.addSnapshotListener { snapshots, error ->
            if(error != null){
                onResult(emptyList())
                return@addSnapshotListener
            }
            val skills = snapshots?.toObjects<Skill>() ?: emptyList()
            onResult(skills)
        }
    }
    fun sendMessage(skillid : String, message: Message){
        db.collection("skills").document(skillid)
            .collection("message").add(message)
    }

    fun listenForMessages(skillId: String, onMessagesUpdate: (List<Message>) -> Unit) {
        db.collection("skills")
            .document(skillId)
            .collection("message") // Провери дали е "message" или "messages" како во Firebase
            .orderBy("timestamp", Query.Direction.DESCENDING) // Ги влечеме од најнови кон најстари
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)
                } ?: emptyList()

                onMessagesUpdate(messages)
            }
    }
}