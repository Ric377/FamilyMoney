// app/src/main/java/com/rich/familymoney/ui/MainScreen.kt
package com.rich.familymoney.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.rich.familymoney.data.Payment
import com.rich.familymoney.repository.GroupRepository
import com.rich.familymoney.viewmodel.MainViewModel
import com.rich.familymoney.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    groupId: String,
    navController: NavHostController,
    onAddPaymentClick: () -> Unit,
    onLeaveGroupClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(groupId, GroupRepository())
    )
) {
    val state by viewModel.uiState.collectAsState()


    val user = Firebase.auth.currentUser ?: return
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snack = remember { SnackbarHostState() }

    val months = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var showEditDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }

    var userName by remember { mutableStateOf(user.displayName ?: "") }
    var userPhoto by remember { mutableStateOf(user.photoUrl?.toString() ?: "") }
    LaunchedEffect(user.uid) {
        Firebase.firestore.collection("users").document(user.uid).addSnapshotListener { d, _ ->
            if (d != null && d.exists()) {
                userName = d.getString("name") ?: userName
                userPhoto = d.getString("photoUrl") ?: userPhoto
            }
        }
    }


    val calendar = remember { Calendar.getInstance() }
    val monthPayments = state.payments
        .sortedByDescending { it.date }
        .filter {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.MONTH) == selectedMonth
        }

    val totalSum = monthPayments.sumOf { it.sum }
    val sumByUser = monthPayments.groupBy { it.name }.mapValues { it.value.sumOf { p -> p.sum } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (userPhoto.startsWith("drawable/")) {
                        val resId = context.resources.getIdentifier(
                            userPhoto.removePrefix("drawable/"), "drawable", context.packageName
                        )
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                    } else {
                        AsyncImage(
                            userPhoto, null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(userName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Edit, null, Modifier.clickable { showEditDialog = true })
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Код группы: $groupId", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton({
                        clipboard.setText(AnnotatedString(groupId))
                        scope.launch { snack.showSnackbar("Код скопирован") }
                    }) { Icon(Icons.Default.ContentCopy, null) }
                }

                if (state.members.isNotEmpty()) {
                    Text("Участники:", Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                    state.members.forEach { member ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (member.photoUrl.startsWith("drawable/")) {
                                val resId = LocalContext.current.resources.getIdentifier(
                                    member.photoUrl.removePrefix("drawable/"), "drawable", LocalContext.current.packageName
                                )
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                )
                            } else if (member.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    member.photoUrl, null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(member.name)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                NavigationDrawerItem(
                    label = { Text("Расчёт долгов") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("debt_screen/$groupId")
                    },
                    icon = { Icon(Icons.Default.Calculate, null) }
                )

                NavigationDrawerItem(
                    label = { Text("Сменить группу") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onLeaveGroupClick() } },
                    icon = { Icon(Icons.Default.Group, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Выйти") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onLogoutClick() } },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) }
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snack, Modifier.zIndex(1f)) },
            topBar = {
                TopAppBar(
                    title = { Text("Общие траты") },
                    navigationIcon = {
                        IconButton({ scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null)
                        }
                    }
                )
            },
            floatingActionButton = { FloatingActionButton(onAddPaymentClick) { Text("+") } }
        ) { padding ->
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                ) {
                    LazyRow(Modifier.fillMaxWidth()) {
                        items(months.indices.toList()) { i ->
                            val sel = i == selectedMonth
                            Text(
                                months[i],
                                style = if (sel) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(end = 12.dp).clickable { selectedMonth = i }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Всего: ${fmt(totalSum)} ₽")
                    sumByUser.forEach { (n, v) -> Text("$n — ${fmt(v)} ₽") }
                    Spacer(Modifier.height(16.dp))
                    if (monthPayments.isEmpty()) {
                        Text("Нет трат", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        monthPayments.forEach { payment ->
                            PaymentItem(payment) {
                                paymentToDelete = it
                            }
                        }
                    }
                }
            }
        }
    }

    paymentToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("Удалить запись?") },
            text = { Text("Трата на ${fmt(p.sum)} ₽ будет удалена.") },
            confirmButton = {
                TextButton({
                    viewModel.deletePayment(p.id)
                    paymentToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton({ paymentToDelete = null }) { Text("Отмена") } }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = userName,
            currentPhotoUrl = userPhoto,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newPhoto ->
                scope.launch {
                    val finalName = newName.ifBlank { userName }
                    var finalPhoto = newPhoto.ifBlank { userPhoto }
                    val storage = Firebase.storage

                    if (newPhoto.startsWith("content://")) {
                        try {
                            val ext = context.contentResolver.getType(Uri.parse(newPhoto))?.substringAfter('/') ?: "jpg"
                            val ref = storage.reference.child("avatars/${user.uid}/${System.currentTimeMillis()}.$ext")
                            ref.putFile(Uri.parse(newPhoto)).await()
                            finalPhoto = ref.downloadUrl.await().toString()
                        } catch (e: Exception) {
                            scope.launch { snack.showSnackbar("Ошибка загрузки аватара") }
                        }
                    }

                    val db = Firebase.firestore
                    db.collection("users").document(user.uid)
                        .set(mapOf("name" to finalName, "photoUrl" to finalPhoto), SetOptions.merge())
                        .await()

                    val gRef = db.collection("groups").document(groupId)
                    val snap = gRef.get().await()
                    val rawMembers = snap.get("members")
                    if (rawMembers is List<*>) {
                        val arr = rawMembers.mapNotNull { item ->
                            val map = item as? Map<*, *> ?: return@mapNotNull null
                            if (map["email"] == user.email) {
                                mapOf("email" to user.email, "name" to finalName, "photoUrl" to finalPhoto)
                            } else map
                        }
                        gRef.update("members", arr).await()
                    }

                    user.updateProfile(
                        com.google.firebase.auth.userProfileChangeRequest {
                            displayName = finalName
                            if (finalPhoto.isNotBlank()) {
                                photoUri = Uri.parse(finalPhoto)
                            }
                        }
                    ).await()

                    showEditDialog = false
                    scope.launch { snack.showSnackbar("Профиль сохранён") }
                }
            }
        )
    }
}

private fun fmt(v: Double) =
    if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(v)

@Composable
private fun EditProfileDialog(
    currentName: String,
    currentPhotoUrl: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var selectedAvatar by remember {
        mutableStateOf(
            if (currentPhotoUrl.startsWith("drawable/")) currentPhotoUrl
            else ""
        )
    }

    val avatarList = (1..17).map { "avatar_$it" }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать профиль") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Выберите аватар:")
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    items(avatarList) { avatar ->
                        val resId = context.resources.getIdentifier(avatar, "drawable", context.packageName)
                        val selected = selectedAvatar == "drawable/$avatar"
                        Box(
                            Modifier
                                .padding(4.dp)
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar = "drawable/$avatar" },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton({ onSave(name.trim(), selectedAvatar) }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } }
    )
}

// Замените только эту функцию в вашем файле MainScreen.kt
@Composable
private fun PaymentItem(p: Payment, askDel: (Payment) -> Unit) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    var showEdit by remember { mutableStateOf(false) }
    var newSum by remember { mutableStateOf(p.sum.toString()) }
    var newComment by remember { mutableStateOf(p.comment) }

    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            // КОММЕНТАРИЙ: Блок с аватаром остаётся без изменений.
            if (p.photoUrl.startsWith("drawable/")) {
                val resId = context.resources.getIdentifier(
                    p.photoUrl.removePrefix("drawable/"), "drawable", context.packageName
                )
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                AsyncImage(
                    p.photoUrl, null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(Modifier.width(12.dp))

            // КОММЕНТАРИЙ: ИЗМЕНЕНИЕ 1 - Из этой колонки убрана строка с суммой.
            // Теперь здесь только комментарий, дата и имя.
            Column(modifier = Modifier.weight(1f)) {
                // Text(fmt(p.sum) + " ₽", style = MaterialTheme.typography.titleMedium) // <-- ЭТА СТРОКА УДАЛЕНА
                if (p.comment.isNotBlank()) Text(p.comment, style = MaterialTheme.typography.bodyMedium)
                Text(sdf.format(Date(p.date)), style = MaterialTheme.typography.bodySmall)
                Text(p.name, style = MaterialTheme.typography.bodySmall)
            }

            // КОММЕНТАРИЙ: ИЗМЕНЕНИЕ 2 - В правую колонку добавлена сумма.
            // Теперь здесь сумма и под ней две иконки.
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    fmt(p.sum) + " ₽",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold // Добавим жирности для акцента
                )
                Row {
                    IconButton({ showEdit = true }) {
                        Icon(Icons.Default.Edit, null)
                    }
                    IconButton({ askDel(p) }) {
                        Icon(Icons.Default.Delete, null)
                    }
                }
            }
        }
    }

    // КОММЕНТАРИЙ: Диалоговое окно для редактирования остаётся без изменений.
    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Редактировать трату") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newSum,
                        onValueChange = { newSum = it },
                        label = { Text("Сумма") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        label = { Text("Комментарий") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updatedSum = newSum.toDoubleOrNull()
                    if (updatedSum != null) {
                        val user = Firebase.auth.currentUser
                        if (user != null) {
                            val db = Firebase.firestore
                            val userDoc = db.collection("users").document(user.uid)
                            userDoc.get().addOnSuccessListener { snapshot ->
                                val groupId = snapshot.getString("groupId")
                                if (!groupId.isNullOrBlank()) {
                                    db.collection("groups").document(groupId)
                                        .collection("payments").document(p.id)
                                        .update(mapOf("sum" to updatedSum, "comment" to newComment))
                                }
                            }
                        }
                    }
                    showEdit = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}