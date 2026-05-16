package uklo.fikt.pmp.pmpproekt

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: com.google.firebase.messaging.RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Провери дали пораката содржи data payload
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Нова порака"
            val message = remoteMessage.data["message"] ?: ""

            // КЛУЧНО: Ги извлекуваме ID-то и името пратени од серверот за Deep Link-от
            val senderId = remoteMessage.data["senderId"] ?: ""
            val senderName = remoteMessage.data["senderName"] ?: "Некој корисник"

            // 2. Ја повикуваме ажурираната функција со сите 5 параметри
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
    }
}