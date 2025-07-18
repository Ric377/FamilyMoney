package com.rich.familymoney.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rich.familymoney.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        // ИЗМЕНЕНИЕ: Теперь мы в первую очередь обрабатываем 'data' payload,
        // так как он будет приходить всегда.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Message Data payload: " + remoteMessage.data)

            // Извлекаем title и body из data
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]

            // Вызываем нашу функцию для показа уведомления
            sendNotification(title, body)
        } else {
            // Оставляем обработку 'notification' payload на случай,
            // если отправляем тестовое уведомление из консоли Firebase.
            remoteMessage.notification?.let { notification ->
                Log.d("FCM", "Notification Message Body: ${notification.body}")
                sendNotification(notification.title, notification.body)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        sendTokenToFirestore(token)
    }

    private fun sendTokenToFirestore(token: String?) {
        if (token == null) return
        // Важно: токен может обновиться до того, как пользователь вошел в систему.
        // Лучше всего вызывать эту функцию также и после успешного входа.
        val userId = Firebase.auth.currentUser?.uid ?: return

        val userDoc = Firebase.firestore.collection("users").document(userId)
        userDoc.update("fcmToken", token)
            .addOnSuccessListener { Log.d("FCM", "Token updated in Firestore") }
            .addOnFailureListener { e -> Log.w("FCM", "Error updating token", e) }
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val channelId = "default_channel_id" // Убедитесь, что ID канала консистентен
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ВАЖНО: Убедитесь, что эта иконка существует!
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Для Android 8.0 (API 26) и выше требуется Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel Name", // Дайте каналу осмысленное имя
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // У каждого уведомления должен быть уникальный ID, если вы хотите показывать несколько одновременно.
        // Использование 0 перезапишет предыдущее уведомление.
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}