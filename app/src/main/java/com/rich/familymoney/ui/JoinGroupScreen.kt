// JoinGroupScreen.kt
package com.rich.familymoney.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.*

@Composable
fun JoinGroupScreen(onGroupJoined: (String) -> Unit) {
    val db = Firebase.firestore
    val user = Firebase.auth.currentUser
    val uid = user?.uid ?: return

    val scope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
        ) {

            // Создание новой группы
            Text("Создать новую группу", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Название группы") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Button(
                onClick = {
                    if (groupName.isBlank()) {
                        message = "Введите название группы"
                        return@Button
                    }
                    isLoading = true
                    message = null

                    scope.launch {
                        try {
                            val newGroupId = UUID.randomUUID().toString().take(8).uppercase()
                            val email = user.email ?: "unknown"
                            val displayName = user.displayName ?: email.substringBefore('@')
                            val photoUrl = user.photoUrl?.toString() ?: ""

                            val groupData = mapOf(
                                "name" to groupName,
                                "members" to listOf(mapOf(
                                    "email" to email,
                                    "name" to displayName,
                                    "photoUrl" to photoUrl
                                ))
                            )

                            db.collection("groups").document(newGroupId).set(groupData).await()
                            // Используем безопасный метод set-merge
                            db.collection("users").document(uid)
                                .set(mapOf("groupId" to newGroupId), SetOptions.merge()).await()

                            withContext(Dispatchers.Main) {
                                onGroupJoined(newGroupId)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                message = "Ошибка создания группы: ${e.message}"
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.padding(top = 12.dp),
                enabled = !isLoading
            ) {
                Text("Создать группу")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Присоединение к существующей группе
            Text("Присоединиться к группе", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = joinCode,
                onValueChange = { joinCode = it.uppercase() },
                label = { Text("Код группы") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Button(
                onClick = {
                    if (joinCode.isBlank()) {
                        message = "Введите код группы"
                        return@Button
                    }
                    isLoading = true
                    message = null

                    scope.launch {
                        try {
                            val groupDoc = db.collection("groups").document(joinCode).get().await()

                            if (groupDoc.exists()) {
                                val email = user.email ?: "unknown"
                                val displayName = user.displayName ?: email.substringBefore('@')
                                val photoUrl = user.photoUrl?.toString() ?: ""

                                val newMember = mapOf(
                                    "email" to email,
                                    "name" to displayName,
                                    "photoUrl" to photoUrl
                                )

                                db.collection("groups").document(joinCode)
                                    .update("members", FieldValue.arrayUnion(newMember)).await()

                                // ИСПРАВЛЕНИЕ ЗДЕСЬ: Заменяем .update() на .set(..., SetOptions.merge())
                                db.collection("users").document(uid)
                                    .set(mapOf("groupId" to joinCode), SetOptions.merge()).await()

                                withContext(Dispatchers.Main) {
                                    onGroupJoined(joinCode)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    message = "Группа с таким кодом не найдена"
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                message = "Ошибка присоединения: ${e.message}"
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.padding(top = 12.dp),
                enabled = !isLoading
            ) {
                Text("Присоединиться")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }

            message?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}