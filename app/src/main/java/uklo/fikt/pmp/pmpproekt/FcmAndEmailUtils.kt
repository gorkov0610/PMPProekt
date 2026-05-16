package uklo.fikt.pmp.pmpproekt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri

fun sendEmailIntent(context: Context, receiverEmail: String, skillTitle: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(receiverEmail))
        putExtra(Intent.EXTRA_SUBJECT, "[SkillSwap] Прашање за вештината: $skillTitle")
        putExtra(Intent.EXTRA_TEXT, "Здраво,\n\nЗаинтересиран сум за вашата вештина '$skillTitle' објавена на SkillSwap...")
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Избери меил апликација:"))
    } catch (e: Exception) {
        Toast.makeText(context, "Нема пронајдено меил апликација", Toast.LENGTH_SHORT).show()
    }
}
fun showLocalNotification(context: Context, title: String, message: String, senderId: String, senderName: String) {
    val channelId = "skillswap_messages"
    val notificationId = System.currentTimeMillis().toInt()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // 1. КРЕИРАЊЕ НА КАНАЛОТ (Задолжително за Android 8.0+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    // 2. КРЕИРАЊЕ НА DEEP LINK НАМЕРА (INTENT) ЗА КЛИК
    // Го користиме точниот пакет на твојата апликација: uklo.fikt.pmp.pmpproekt
    val encodedName = Uri.encode(senderName)
    val intent = if (senderId.isNotEmpty()) {
        // АКО Е ЧАТ: Оди во соодветната соба
        val encodedName = Uri.encode(senderName)
        Intent(Intent.ACTION_VIEW, Uri.parse("skillswap://chat/$senderId/$encodedName")).apply {
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
        .setSmallIcon(android.R.drawable.stat_notify_chat)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true) // Оваа линија ја брише нотификацијата откако ќе кликнеш!
        .setContentIntent(pendingIntent) // КЛУЧНО: Ова ја извршува навигацијата при клик
        .setDefaults(NotificationCompat.DEFAULT_ALL)

    // 4. ПРИКАЖУВАЊЕ
    try {
        notificationManager.notify(notificationId, builder.build())
    } catch (e: Exception) {
        android.util.Log.e("GLOBAL_FCM", "Грешка при прикажување на нотификацијата", e)
    }
}