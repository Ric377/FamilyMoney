package com.rich.familymoney.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
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

    // КОММЕНТАРИЙ: Состояние для хранения даты и показа календаря
    var selectedDate by remember { mutableStateOf(Date()) } // По умолчанию - сегодня
    var showDatePicker by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru")) }

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

            // КОММЕНТАРИЙ: ИЗМЕНЕНИЕ 1 - Добавлено поле для выбора даты
            // НОВЫЙ КОД:
            Box {
                OutlinedTextField(
                    value = sdf.format(selectedDate),
                    onValueChange = {},
                    label = { Text("Дата") },
                    trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Выбрать дату") },
                    modifier = Modifier.fillMaxWidth(),
                    // КОММЕНТАРИЙ: Поле теперь активно, но ввод запрещён.
                    // Это заставляет его выглядеть так же, как и остальные поля.
                    enabled = true,
                    readOnly = true
                )
                // КОММЕНТАРИЙ: Невидимая область для нажатий остаётся,
                // чтобы гарантированно открывать календарь.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
            }

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
                            val userDoc = db.collection("users").document(uid).get().await()
                            val name = userDoc.getString("name") ?: "?"
                            val photoUrl = userDoc.getString("photoUrl") ?: ""

                            // КОММЕНТАРИЙ: ИЗМЕНЕНИЕ 2 - При сохранении используется выбранная дата
                            val payment = hashMapOf(
                                "sum" to parsedSum,
                                "comment" to comment,
                                "date" to selectedDate, // <-- ИЗМЕНЕНИЕ ЗДЕСЬ
                                "name" to name,
                                "photoUrl" to photoUrl
                            )

                            db.collection("groups").document(groupId)
                                .collection("payments")
                                .add(payment)

                            withContext(Dispatchers.Main) {
                                onBack()
                            }

                        } catch (e: Exception) {
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

    // КОММЕНТАРИЙ: ИЗМЕНЕНИЕ 3 - Добавлен диалог с календарём
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Date(it)
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}