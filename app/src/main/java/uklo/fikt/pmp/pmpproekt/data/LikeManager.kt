package uklo.fikt.pmp.pmpproekt.data

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import uklo.fikt.pmp.pmpproekt.PreferenceManager
import uklo.fikt.pmp.pmpproekt.R
import uklo.fikt.pmp.pmpproekt.showLocalNotification

fun toggleLikeSkill(skill: Skill, currentUserId: String, db: FirebaseFirestore, context: Context) {
    val appContext = context.applicationContext
    val skillRef = db.collection("skills").document(skill.id)
    val prefManager = PreferenceManager(appContext)

    val currentlyLikedInApp = prefManager.isSkillLiked(skill.id)

    if (!currentlyLikedInApp) {
        skillRef.update(
            "likesCount", FieldValue.increment(-1),
            "likedBy", FieldValue.arrayRemove(currentUserId)
        )
    } else {
        skillRef.update(
            "likesCount", FieldValue.increment(1),
            "likedBy", FieldValue.arrayUnion(currentUserId)
        ).addOnSuccessListener {
            val nextLikesCount = skill.likesCount + 1
            if (nextLikesCount == 1 || nextLikesCount == 2 || nextLikesCount == 100 || nextLikesCount == 500) {

                showLocalNotification(
                    context = appContext,
                    title = appContext.getString(R.string.notification_like_title),
                    message = appContext.getString(R.string.notification_like_message, skill.title, nextLikesCount),
                    senderId = "",
                    senderName = ""
                )
            }
        }
    }
}