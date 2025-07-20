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
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rich.familymoney.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Обрабатываем data payload, который приходит от нашей облачной функции
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message Data payload: " + remoteMessage.data)

            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]

            sendNotification(title, body)
        } else {
            // Обрабатываем notification payload (например, для тестов из консоли Firebase)
            remoteMessage.notification?.let { notification ->
                Log.d(TAG, "Notification Message Body: ${notification.body}")
                sendNotification(notification.title, notification.body)
            }
        }
    }

    /**
     * Вызывается, когда система выдает новый или обновляет существующий токен.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        // Вызываем нашу новую общую функцию для сохранения токена
        sendTokenToFirestore(token)
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val channelId = "default_channel_id"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Убедитесь, что иконка существует
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Для Android 8.0 (API 26) и выше требуется Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel Name",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Companion object содержит "статические" методы,
     * которые можно вызывать из других классов без создания экземпляра сервиса.
     */
    companion object {
        private const val TAG = "FCM_SERVICE"

        /**
         * Получает актуальный токен и сохраняет его в Firestore.
         * Эту функцию мы вызываем из MainActivity после входа пользователя.
         */
        fun updateTokenAfterLogin() {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                Log.d(TAG, "Got token after login: $token")
                sendTokenToFirestore(token)
            }
        }

        /**
         * Централизованная функция для сохранения токена в Firestore.
         */
        private fun sendTokenToFirestore(token: String?) {
            if (token == null) return

            // Убедимся, что пользователь вошел в систему, чтобы знать, к какому документу привязать токен
            val userId = Firebase.auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "Cannot save token, user is not logged in.")
                return
            }

            val userDoc = Firebase.firestore.collection("users").document(userId)
            userDoc.update("fcmToken", token)
                .addOnSuccessListener { Log.d(TAG, "Token updated successfully in Firestore") }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating token", e) }
        }
    }
}