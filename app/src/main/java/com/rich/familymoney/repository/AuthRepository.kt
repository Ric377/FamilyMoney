// app/src/main/java/com/rich/familymoney/repository/AuthRepository.kt
package com.rich.familymoney.repository

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rich.familymoney.data.UserData
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("965519537654-1o79m7go9hm5s37ftpghg49t8o2ns8b6.apps.googleusercontent.com")
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Boolean {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).result ?: return false
        return firebaseAuthWithGoogle(account)
    }

    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Boolean {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val user = auth.signInWithCredential(credential).await().user ?: return false

        val updates = mapOf(
            "uid"      to user.uid,
            "email"    to (user.email ?: ""),
            "name"     to (user.displayName ?: ""),
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
                name = user.email ?: email,
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

    fun logout(activity: Activity) {
        auth.signOut()
        getGoogleSignInClient(activity).signOut()
    }
}
