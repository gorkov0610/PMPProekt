package uklo.fikt.pmp.pmpproekt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri

fun sendEmailIntent(context: Context, receiverEmail: String, skillTitle: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(receiverEmail))
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject,skillTitle))
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.email_body))
    }
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.choose_mail_client)))
    } catch (e: Exception) {
        Log.e("EmailIntent", "Грешка при отворање на маил клиент", e)
        Toast.makeText(context, context.getString(R.string.no_email_client), Toast.LENGTH_SHORT).show()
    }
}
fun showLocalNotification(context: Context, title: String, message: String, senderId: String, senderName: String) {
    val channelId = "skillswap_messages"
    val notificationId = System.currentTimeMillis().toInt()
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (notificationManager.getNotificationChannel(channelId) == null) {
        val channel = NotificationChannel(
            channelId,
            "SkillSwap Chat Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notification channel for real-time chat messages"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }


    val intent = if (senderId.isNotEmpty()) {
        // АКО Е ЧАТ: Оди во соодветната соба
        val encodedName = Uri.encode(senderName)
        Intent(Intent.ACTION_VIEW, "skillswap://chat/$senderId/$encodedName".toUri()).apply {
            `package` = "uklo.fikt.pmp.pmpproekt"
        }
    } else {
        // АКО Е ЛАЈК: Само нормално отвори ја почетната страница на апликацијата
        context.packageManager.getLaunchIntentForPackage("uklo.fikt.pmp.pmpproekt")?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        } ?: Intent()
    }

    // Го пакуваме Intent-от во PendingIntent за Android систем да може да го изврши при клик
    val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 3. ИЗГРАДБА НА НОТИФИКАЦИЈАТА СО PENDING INTENT
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true) // Оваа линија ја брише нотификацијата откако ќе кликнеш!
        .setContentIntent(pendingIntent) // КЛУЧНО: Ова ја извршува навигацијата при клик
        .setDefaults(NotificationCompat.DEFAULT_ALL)

    try {
        notificationManager.notify(notificationId, builder.build())
    } catch (e: Exception) {
        Log.e("GLOBAL_FCM", "Грешка при прикажување на нотификацијата", e)
    }
}