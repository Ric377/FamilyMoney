package com.rich.familymoney.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
    var selectedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru")) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Новая трата") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Поля ввода теперь находятся в основной части Column
            OutlinedTextField(
                value = sum,
                onValueChange = { sum = it },
                label = { Text("Сумма") },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий") },
                singleLine = true, // Сделано однострочным для единого дизайна
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Spacer(Modifier.height(8.dp))

            // Поле для выбора даты
            Box {
                OutlinedTextField(
                    value = sdf.format(selectedDate),
                    onValueChange = {},
                    label = { Text("Дата") },
                    trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Выбрать дату") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    readOnly = true
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { if (!isLoading) showDatePicker = true }
                )
            }

            // Распорка, чтобы кнопка всегда была внизу
            Spacer(Modifier.weight(1f))

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Кнопка Сохранить
            Button(
                onClick = {
                    val parsedSum = sum.toDoubleOrNull()
                    if (parsedSum == null || parsedSum <= 0) {
                        errorText = "Введите корректную сумму"
                        return@Button
                    }
                    if (user?.uid == null || groupId == null) {
                        errorText = "Ошибка пользователя или группы"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            val userDoc = db.collection("users").document(user.uid).get().await()
                            val name = userDoc.getString("name") ?: "?"
                            val photoUrl = userDoc.getString("photoUrl") ?: ""

                            val payment = hashMapOf(
                                "sum" to parsedSum,
                                "comment" to comment,
                                "date" to selectedDate,
                                "name" to name,
                                "photoUrl" to photoUrl,
                                "email" to (user?.email ?: "")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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

    // Диалог с календарём
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