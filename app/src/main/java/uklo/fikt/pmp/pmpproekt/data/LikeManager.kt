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

    // ПРОВЕРКА ОД БАЗАТА: Дали мојот UID е веќе во низата на огласот?
    val isAlreadyLiked = skill.likedBy.contains(currentUserId)

    if (isAlreadyLiked) {
        // Веќе имало мој UID -> Значи корисникот сака да ОДЛАЈКНЕ
        prefManager.setSkillLiked(skill.id, false)

        skillRef.update(
            "likesCount", FieldValue.increment(-1),
            "likedBy", FieldValue.arrayRemove(currentUserId)
        )
    } else {
        // Го нема мојот UID -> Корисникот сака да ЛАЈКНЕ
        prefManager.setSkillLiked(skill.id, true)

        skillRef.update(
            "likesCount", FieldValue.increment(1),
            "likedBy", FieldValue.arrayUnion(currentUserId)
        ).addOnSuccessListener {
            val nextLikesCount = skill.likesCount + 1
            if (nextLikesCount == 1 || nextLikesCount == 2 || nextLikesCount == 100 || nextLikesCount == 500) {
                showLocalNotification(
                    context = context,
                    title = context.getString(R.string.notification_like_title),
                    message = context.getString(R.string.notification_like_message, skill.title, nextLikesCount),
                    senderId = "",
                    senderName = ""
                )
            }
        }
    }
}