package uklo.fikt.pmp.pmpproekt.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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
        val docRef = skillsCollection.document(skill.id)
        docRef.set(skill)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    fun getSkills(onResult: (List<Skill>) -> Unit) {
        skillsCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                onResult(emptyList())
                return@addSnapshotListener
            }

            val skills = snapshots?.documents?.mapNotNull { doc ->
                doc.toObject(Skill::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            onResult(skills)
        }
    }


    fun sendMessage(chatRoomId: String, message: Message, receiverName: String, currentUserName: String) {
        db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .add(message)

        val chatRoomInfo = mapOf(
            "lastMessage" to message.text,
            "timestamp" to message.timestamp,
            "participants" to listOf(message.senderId, message.receiverId),
            "userNames" to mapOf(
                message.senderId to currentUserName,
                message.receiverId to receiverName
            )
        )
        db.collection("chats")
            .document(chatRoomId)
            .set(chatRoomInfo, SetOptions.merge())

        db.collection("users").document(message.receiverId).get()
            .addOnSuccessListener { userDoc ->
                val receiverToken = userDoc.getString("fcmToken")
                if (!receiverToken.isNullOrEmpty()) {
                    val notificationData = mapOf(
                        "to" to receiverToken,
                        "title" to currentUserName,
                        "body" to message.text,
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("notifications").add(notificationData)
                        .addOnSuccessListener { Log.d("FCM_FIRESTORE", "Нотификацијата е спремна!") }
                }
            }
    }

    fun listenForMessages(chatRoomId: String, onMessagesUpdate: (List<Message>) -> Unit) {
        db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Од најстари кон најнови за убав чет приказ
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore_Chat", "Грешка при слушање пораки", e)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)
                } ?: emptyList()

                onMessagesUpdate(messages)
            }
    }
    fun deleteUserData(userId: String, onComplete: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("skills")
            .whereEqualTo("authorId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()

                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }

                val userRef = db.collection("users").document(userId)
                batch.delete(userRef)

                batch.commit()
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e("DB_DELETE", "Грешка при бришење од Firestore", e)
                        onComplete(false)
                    }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
    fun getLikedSkills(uid: String, onResult: (List<Skill>) -> Unit, onFailure: (Exception) -> Unit) {
        skillsCollection.whereArrayContains("likedBy", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val skills = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Skill::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                onResult(skills)
            }
    }
}