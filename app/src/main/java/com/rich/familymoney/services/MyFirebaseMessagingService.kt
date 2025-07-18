// MyFirebaseMessagingService.kt
package com.rich.familymoney.services // Можете создать новую папку services

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Вызывается, когда приходит новое сообщение
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // TODO: Здесь будет код для отображения уведомления на устройстве
        // Пока мы просто выводим его в лог
        Log.d("FCM", "From: ${remoteMessage.from}")
        remoteMessage.notification?.let {
            Log.d("FCM", "Notification Message Body: ${it.body}")
        }
    }

    // Вызывается, когда Firebase выдаёт новый токен для этого устройства
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        // Отправляем токен на сервер (в наш Firestore)
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
}