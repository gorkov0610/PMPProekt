package uklo.fikt.pmp.pmpproekt.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import uklo.fikt.pmp.pmpproekt.sendFcmMessageDirectly

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

    fun getSkills(onResult: (List<Skill>) -> Unit, onFailure: (Exception) -> Unit): ListenerRegistration {
        return skillsCollection.addSnapshotListener { snapshots, error ->
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


    fun sendMessage(context : Context, chatRoomId: String, message: Message, receiverName: String, currentUserName: String) {
        db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                Log.d("DatabaseManager", "Пораката е зачувана во соба:$chatRoomId")
            }

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
            .addOnSuccessListener {
                Log.d("DatabaseManager", "Инбоксот е ажуриран")
            }


        db.collection("users").document(message.receiverId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()){
                    val receiverToken = document.getString("fcmToken") ?: ""
                    if (receiverToken.isNotEmpty()) {
                        sendFcmMessageDirectly(
                            context = context,
                            targetToken = receiverToken,
                            senderName = currentUserName,
                            senderId = message.senderId,
                            messageText = message.text
                        )
                    } else {
                        Log.d("FCM_V1_LIVE","Примачот нема FCM токен")
                    }
                }
            }
    }

    fun listenForMessages(chatRoomId: String, onMessagesUpdate: (List<Message>) -> Unit): ListenerRegistration {
        return db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
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
    fun getLikedSkills(uid: String, onResult: (List<Skill>) -> Unit, onFailure: (Exception) -> Unit): ListenerRegistration {
        return skillsCollection.whereArrayContains("likedBy", uid)
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

    fun getUserTokenAndDetails(userId: String, onResult: (String?, String?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if(document != null && document.exists()){
                    val token = document.getString("fcmToken")
                    val photoUrl = document.getString("profilePicture")
                    onResult(token, photoUrl)
                } else {
                    onResult(null, null)
                }
            }
            .addOnFailureListener {
                onResult(null, null)
            }
    }

    fun updateFcmTokenForCurrentUser(userId: String) {
        if (userId.isEmpty()) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if(!task.isSuccessful){
                Log.w("DatabaseManager", "Грешка при превземање FCM token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result

            val tokenData = mapOf("fcmToken" to token)

            db.collection("users").document(userId).set(tokenData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("DatabaseManager", "FCM токенот е успешно ажуриран за $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("DatabaseManager", "Се случи грешка при ажурирање на токенот", e)
                }
        }
    }
}