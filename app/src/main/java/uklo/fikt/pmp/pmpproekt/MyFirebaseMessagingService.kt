package uklo.fikt.pmp.pmpproekt

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // 1. Извлекување податоци од 'data' payload (Data messages)
        var title = remoteMessage.data["title"] ?: "Нова порака"
        var message = remoteMessage.data["message"] ?: ""
        var senderId = remoteMessage.data["senderId"] ?: ""
        var senderName = remoteMessage.data["senderName"] ?: "Корисник"

        // 2. Алтернативно извлекување ако пораката доаѓа како чист 'notification' payload
        remoteMessage.notification?.let {
            if (message.isEmpty()) {
                title = it.title ?: title
                message = it.body ?: message
            }
        }

        // 3. Филтрирање: Не прикажувај нотификација ако пристигнатиот senderId е на самиот моментално најавен корисник
        if (senderId.isNotEmpty() && senderId == currentUserId) {
            Log.d("FCM_SERVICE", "Игнорирана нотификација: Пораката е испратена од самиот корисник.")
            return
        }

        // 4. Прикажување на локалната нотификација со deep link поддршка
        if (message.isNotEmpty()) {
            showLocalNotification(
                context = this,
                title = title,
                message = message,
                senderId = senderId,
                senderName = senderName
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SERVICE", "Нов уред регистриран! FCM Токен: $token")
        // Токенот автоматски ќе си се ажурира преку AuthManager.updateFCMToken() во твојот MainActivity
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            sendTokenToFirestore(currentUserId, token)
        }
    }

    private fun sendTokenToFirestore(userId: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM_SERVICE", "FCM Токенот е успешно ажуриран во Firestore директно од сервисот.")
            }
            .addOnFailureListener { e ->
                Log.e("FCM_SERVICE", "Грешка при зачувување на FCM токенот", e)
            }
    }

}