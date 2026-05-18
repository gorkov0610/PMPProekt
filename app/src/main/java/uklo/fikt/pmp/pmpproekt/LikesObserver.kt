package uklo.fikt.pmp.pmpproekt

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

fun startLikesObserver(context: Context, currentUserId: String) {
    val db = FirebaseFirestore.getInstance()

    Log.d("LIKES_OBSERVER", "Слушачот за лајкови е активиран за корисник: $currentUserId")

    db.collection("skills")
        .whereEqualTo("authorId", currentUserId)
        .addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w("LIKES_OBSERVER", "Грешка при слушање на Firestore", error)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                for (doc in snapshots.documentChanges) {
                    val adTitle = doc.document.getString("title") ?: "Твојот оглас"

                    // ВНИМАВАЈ: Името тука мора да е ИСТО како во Firestore конзолата!
                    val likesCount = doc.document.getLong("likesCount") ?: 0L

                    Log.d("LIKES_OBSERVER", "Пронајден оглас: $adTitle со лајкови: $likesCount")

                    // Привремено тргни ја проверката за MODIFIED и провери дали е 100
                    if (likesCount == 100L) {
                        Log.d("LIKES_OBSERVER", "Условот за 100 лајкови е ИСПУЛНЕТ! Пуштам нотификација...")

                        showLocalNotification(
                            context = context,
                            title = "Популарен оглас! 🔥",
                            message = "Честитки! Твојот оглас '$adTitle' достигна $likesCount лајкови.",
                            senderId = "system_alert",
                            senderName = "SkillSwap Систем"
                        )
                }
            }
        }
    }
}
