// ChangeGroupScreen.kt
package com.rich.familymoney.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeGroupScreen(onBack: () -> Unit) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val user = auth.currentUser

    val scope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Создать группу") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (groupName.isBlank()) {
                    errorText = "Введите название"
                    return@FloatingActionButton
                }
                if (user == null) {
                    errorText = "Пользователь не авторизован"
                    return@FloatingActionButton
                }
                isLoading = true
                errorText = null

                scope.launch {
                    try {
                        val uid = user.uid
                        val email = user.email ?: ""
                        val displayName = user.displayName ?: email.substringBefore('@')
                        val photoUrl = user.photoUrl?.toString() ?: ""

                        val newGroupId = UUID.randomUUID().toString().take(8).uppercase()

                        val data = mapOf(
                            "name" to groupName,
                            "members" to listOf(
                                mapOf(
                                    "email" to email,
                                    "name" to displayName,
                                    "photoUrl" to photoUrl
                                )
                            )
                        )

                        // НОВЫЙ КОД:
                        db.collection("groups").document(newGroupId).set(data).await()

                        // КОММЕНТАРИЙ: Безопасно обновляем groupId для текущего пользователя, используя newGroupId
                        db.collection("users").document(uid)
                            .set(mapOf("groupId" to newGroupId), SetOptions.merge()).await()

                        withContext(Dispatchers.Main) {
                            onBack() // КОММЕНТАРИЙ: Возвращаемся на предыдущий экран, как и должно быть
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            errorText = "Ошибка создания группы: ${e.message}"
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                        }
                    }
                }
            }) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("✓")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Название группы") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }
    }
}