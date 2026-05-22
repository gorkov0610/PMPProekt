package uklo.fikt.pmp.pmpproekt.data

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import uklo.fikt.pmp.pmpproekt.PreferenceManager
import uklo.fikt.pmp.pmpproekt.R
import uklo.fikt.pmp.pmpproekt.showLocalNotification

fun toggleLikeSkill(skill: Skill, currentUserId: String, db: FirebaseFirestore, context: Context) {
    val skillRef = db.collection("skills").document(skill.id)
    val prefManager = PreferenceManager(context)
    val isAlreadyLiked = prefManager.isSkillLiked(skill.id)

    if (isAlreadyLiked) {
        skillRef.update(
            "likesCount", FieldValue.increment(1),
            "likedBy", FieldValue.arrayRemove(currentUserId)
        ).addOnSuccessListener {
            // Пресметуваме која ќе биде следната бројка на лајкови
            val nextLikesCount = skill.likesCount + 1

            // За тест пред професор: ставаме да реагира и на 1 и на 2 лајкови за полесно демонстрирање
            if (nextLikesCount == 1 || nextLikesCount == 2 || nextLikesCount == 100 || nextLikesCount == 500) {

                val notificationTitle = context.getString(R.string.notification_like_title)
                val notificationText = context.getString(R.string.notification_like_message, skill.title, nextLikesCount)

                showLocalNotification(
                    context = context,
                    title = notificationTitle,
                    message = notificationText,
                    senderId = "",    // Празно за да знае нотификацијата дека НЕ треба да отвори чет
                    senderName = ""   // Празно
                )
            }
        }
    } else {
        skillRef.update(
            "likesCount", FieldValue.increment(-1),
            "likedBy", FieldValue.arrayUnion(currentUserId)
        )
    }
}