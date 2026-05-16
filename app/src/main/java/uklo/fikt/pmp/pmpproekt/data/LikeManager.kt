package uklo.fikt.pmp.pmpproekt

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import uklo.fikt.pmp.pmpproekt.data.Skill

fun toggleLikeSkill(skill: Skill, currentUserId: String, db: FirebaseFirestore, context: Context) {
    val skillRef = db.collection("skills").document(skill.id)
    val isAlreadyLiked = skill.likedBy.contains(currentUserId)

    if (isAlreadyLiked) {
        // Корисникот веќе лајкнал -> Го повлекува лајкот во Firestore
        skillRef.update(
            "likesCount", FieldValue.increment(-1),
            "likedBy", FieldValue.arrayRemove(currentUserId)
        )
    } else {
        // Нов лајк -> Зголеми број и додади го корисникот во листата во Firestore
        skillRef.update(
            "likesCount", FieldValue.increment(1),
            "likedBy", FieldValue.arrayUnion(currentUserId)
        ).addOnSuccessListener {
            // Пресметуваме која ќе биде следната бројка на лајкови
            val nextLikesCount = skill.likesCount + 1

            // За тест пред професор: ставаме да реагира и на 1 и на 2 лајкови за полесно демонстрирање
            if (nextLikesCount == 1 || nextLikesCount == 2 || nextLikesCount == 100 || nextLikesCount == 500) {

                // ТЕСТ ЗА НА ФАКУЛТЕТ: Го тргаме строгиот филтер skill.authorId == currentUserId
                // за да можеш самиот да си кликнеш лајк на емулаторот и ВЕДНАШ да ти излета нотификацијата како доказ!

                showLocalNotification(
                    context = context,
                    title = "Јубилеен лајк! 🎉",
                    message = "Вештината '${skill.title}' достигна $nextLikesCount лајкови! 🚀",
                    senderId = "",    // Празно за да знае нотификацијата дека НЕ треба да отвори чет
                    senderName = ""   // Празно
                )
            }
        }
    }
}