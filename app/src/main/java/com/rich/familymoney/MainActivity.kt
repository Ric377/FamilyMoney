// MainActivity.kt
package com.rich.familymoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rich.familymoney.repository.AuthRepository
import com.rich.familymoney.ui.theme.FamilyMoneyTheme
import com.rich.familymoney.ui.AuthScreen
import com.rich.familymoney.ui.MainNavigation
import com.rich.familymoney.ui.JoinGroupScreen
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope


class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = Firebase.auth
        val db = Firebase.firestore

        // Современный способ обработки результата от Google Sign-In
        val googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                lifecycleScope.launch {
                    val success = authRepository.handleGoogleSignInResult(data)
                    if (success) {
                        // Просто обновляем состояние, UI перерисуется сам
                        // recreate() не нужен
                    }
                }
            }
        }

        setContent {
            FamilyMoneyTheme {
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                var groupId by remember { mutableStateOf<String?>(null) }
                var loading by remember { mutableStateOf(true) }

                // Этот слушатель будет автоматически обновлять currentUser
                // при входе или выходе из системы.
                DisposableEffect(Unit) {
                    val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
                        currentUser = it.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                LaunchedEffect(currentUser?.uid) {
                    loading = true
                    currentUser?.uid?.let { uid ->
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                groupId = doc.getString("groupId")
                                loading = false
                            }
                            .addOnFailureListener {
                                // Ошибка загрузки данных, возможно, пользователя еще нет в Firestore
                                groupId = null
                                loading = false
                            }
                    } ?: run {
                        groupId = null
                        loading = false
                    }
                }

                when {
                    currentUser == null -> {
                        AuthScreen(
                            authRepository = authRepository,
                            onGoogleSignInClick = {
                                val intent = authRepository.getGoogleSignInClient(this@MainActivity).signInIntent
                                googleSignInLauncher.launch(intent)
                            },
                            onAuthSuccess = {
                                // Состояние обновится автоматически через AuthStateListener
                            }
                        )
                    }
                    loading -> {
                        LoadingScreen()
                    }
                    groupId != null -> {
                        MainNavigation(
                            groupId = groupId,
                            onLogoutClick = {
                                authRepository.logout(this@MainActivity)
                                // Состояние обновится автоматически
                            }
                        )
                    }
                    else -> {
                        JoinGroupScreen(onGroupJoined = { newGroupId ->
                            groupId = newGroupId
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Загрузка группы...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}