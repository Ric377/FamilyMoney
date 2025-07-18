// MyFirebaseMessagingService.kt
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
import com.rich.familymoney.R // Убедитесь, что R импортируется правильно

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        // КОММЕНТАРИЙ: Теперь мы не просто выводим в лог, а вызываем функцию показа уведомления
        remoteMessage.notification?.let { notification ->
            Log.d("FCM", "Notification Message Body: ${notification.body}")
            sendNotification(notification.title, notification.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        sendTokenToFirestore(token)
    }

    private fun sendTokenToFirestore(token: String?) {
        if (token == null) return
        val userId = Firebase.auth.currentUser?.uid ?: return

        val userDoc = Firebase.firestore.collection("users").document(userId)
        userDoc.update("fcmToken", token)
            .addOnSuccessListener { Log.d("FCM", "Token updated in Firestore") }
            .addOnFailureListener { e -> Log.w("FCM", "Error updating token", e) }
    }

    // КОММЕНТАРИЙ: НОВАЯ ФУНКЦИЯ ДЛЯ СОЗДАНИЯ И ПОКАЗА УВЕДОМЛЕНИЯ
    private fun sendNotification(title: String?, messageBody: String?) {
        val channelId = "default_channel_id"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ВАЖНО: Замените на вашу иконку
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Для Android 8.0 и выше требуется Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID уведомления */, notificationBuilder.build())
    }
}