package com.rich.familymoney.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentScreen(
    groupId: String?,
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    val user = Firebase.auth.currentUser
    val scope = rememberCoroutineScope()

    var sum by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая трата") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = sum,
                onValueChange = { sum = it },
                label = { Text("Сумма") },
                singleLine = true,
                enabled = !isLoading
            )
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий") },
                singleLine = false,
                maxLines = 3,
                enabled = !isLoading
            )

            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
            }

            // Кнопка Сохранить
            Button(
                onClick = {
                    val parsedSum = sum.toDoubleOrNull()
                    if (parsedSum == null || parsedSum <= 0) {
                        errorText = "Введите корректную сумму"
                        return@Button
                    }

                    val uid = user?.uid
                    if (uid == null || groupId == null) {
                        errorText = "Ошибка пользователя или группы"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            // Сначала получаем данные пользователя.
                            // Эта операция использует .await(), т.к. нам нужны эти данные для записи.
                            // Если их нет в кеше, при офлайне здесь возникнет ошибка.
                            val userDoc = db.collection("users").document(uid).get().await()
                            val name = userDoc.getString("name") ?: "?"
                            val photoUrl = userDoc.getString("photoUrl") ?: ""

                            val payment = hashMapOf(
                                "sum" to parsedSum,
                                "comment" to comment,
                                "date" to Date(),
                                "name" to name,
                                "photoUrl" to photoUrl
                            )

                            // ИСПРАВЛЕНИЕ: Добавляем трату в очередь БЕЗ .await()
                            // Firestore SDK сам поставит запись в очередь и отправит при появлении сети.
                            db.collection("groups").document(groupId)
                                .collection("payments")
                                .add(payment)

                            // Сразу же возвращаемся на предыдущий экран
                            withContext(Dispatchers.Main) {
                                onBack()
                            }

                        } catch (e: Exception) {
                            // Этот блок сработает, если, например, данные пользователя не были
                            // загружены в кеш и интернет отсутствует.
                            withContext(Dispatchers.Main) {
                                errorText = "Ошибка. Проверьте интернет и попробуйте снова."
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Сохранить")
                }
            }
        }
    }
}