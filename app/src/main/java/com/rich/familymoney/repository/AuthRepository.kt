// app/src/main/java/com/rich/familymoney/repository/AuthRepository.kt
package com.rich.familymoney.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rich.familymoney.R // Убедись, что R импортирован из твоего пакета
import com.rich.familymoney.data.UserData
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // УЛУЧШЕНИЕ: Используем ссылку на ресурсы вместо жестко закодированной строки.
            // Это более безопасный и стандартный подход.
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    // ИСПРАВЛЕНИЕ: Обернули логику в try-catch для безопасной обработки ошибок
    suspend fun handleGoogleSignInResult(data: Intent?): Boolean {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            firebaseAuthWithGoogle(account)
        } catch (e: Exception) {
            // Логируем ошибку, чтобы видеть ее в Logcat при отладке
            Log.w("AuthRepository", "Google sign in failed", e)
            false
        }
    }

    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Boolean {
        // idToken не может быть null, если мы его запрашивали в gso
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        val user = auth.signInWithCredential(credential).await().user ?: return false

        val updates = mapOf(
            "uid"      to user.uid,
            "email"    to (user.email ?: ""),
            "name"     to (user.displayName ?: user.email?.substringBefore('@') ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: "")
        )

        // merge = дописываем без стирания groupId
        db.collection("users")
            .document(user.uid)
            .set(updates, SetOptions.merge())
            .await()

        return true
    }


    suspend fun register(email: String, password: String): Result<Boolean> {
        return try {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: return Result.failure(Exception("Ошибка регистрации"))
            val userData = UserData(
                uid = user.uid,
                email = user.email ?: email,
                name = user.email?.substringBefore('@') ?: email,
                groupId = null,
                photoUrl = ""
            )
            db.collection("users").document(user.uid).set(userData).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout(context: Context) {
        auth.signOut()
        // Важно: передаем context, а не activity, чтобы репозиторий не зависел от Activity
        getGoogleSignInClient(context).signOut()
    }
}