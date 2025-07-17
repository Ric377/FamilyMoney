package com.rich.familymoney

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rich.familymoney.repository.AuthRepository
import com.rich.familymoney.ui.AuthScreen
import com.rich.familymoney.ui.JoinGroupScreen
import com.rich.familymoney.ui.MainNavigation
import com.rich.familymoney.ui.theme.FamilyMoneyTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = Firebase.auth
        val db = Firebase.firestore

        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings

        val googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                lifecycleScope.launch {
                    authRepository.handleGoogleSignInResult(data)
                }
            }
        }

        setContent {
            FamilyMoneyTheme {
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                var groupId by remember { mutableStateOf<String?>(null) }
                var loading by remember { mutableStateOf(true) }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        currentUser = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                LaunchedEffect(currentUser?.uid) {
                    loading = true
                    groupId = null
                    currentUser?.uid?.let { uid ->
                        try {
                            val document = db.collection("users").document(uid).get().await()
                            groupId = document.getString("groupId")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Ошибка при загрузке данных пользователя", e)
                        }
                    }
                    loading = false
                }

                // --- ИСПРАВЛЕНИЕ ЗДЕСЬ: Используем более простую и надежную структуру if/else ---
                if (currentUser == null) {
                    // Пользователь не авторизован
                    AuthScreen(
                        authRepository = authRepository,
                        onGoogleSignInClick = {
                            val intent = authRepository.getGoogleSignInClient(this@MainActivity).signInIntent
                            googleSignInLauncher.launch(intent)
                        },
                        onAuthSuccess = {}
                    )
                } else {
                    // Пользователь авторизован, проверяем состояние загрузки и группу
                    if (loading) {
                        LoadingScreen()
                    } else {
                        val currentGroupId = groupId
                        if (currentGroupId != null) {
                            // Все проверки пройдены, показываем главный экран
                            MainNavigation(
                                groupId = currentGroupId, // Теперь здесь точно не будет ошибки
                                onLogoutClick = {
                                    authRepository.logout(this@MainActivity)
                                }
                            )
                        } else {
                            // Пользователь авторизован, но не состоит в группе
                            JoinGroupScreen(onGroupJoined = { newGroupId ->
                                groupId = newGroupId
                            })
                        }
                    }
                }
                // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
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
                contentDescription = "Загрузка",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Загрузка данных...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}