// app/src/main/java/com/rich/familymoney/ui/MainScreen.kt
package com.rich.familymoney.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.rich.familymoney.data.Payment
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import java.util.*

private data class Member(val name: String, val photoUrl: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAddPaymentClick: () -> Unit,
    onLeaveGroupClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val db = Firebase.firestore
    val storage = Firebase.storage
    val user = Firebase.auth.currentUser ?: return
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snack = remember { SnackbarHostState() }

    var groupId by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf(user.displayName ?: "") }
    var userPhoto by remember { mutableStateOf("") }
    var payments by remember { mutableStateOf(emptyList<Payment>()) }
    var members by remember { mutableStateOf(emptyList<Member>()) }

    val months = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var showEdit by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Payment?>(null) }

    LaunchedEffect(user.uid) {
        db.collection("users").document(user.uid).addSnapshotListener { d, _ ->
            if (d != null && d.exists()) {
                groupId = d.getString("groupId")
                userName = d.getString("name") ?: userName
                userPhoto = d.getString("photoUrl") ?: userPhoto
            }
        }
    }


    LaunchedEffect(groupId) {
        groupId?.let { gid ->
            // 👇 ВСТАВКА: автообновление members
            val gRef = db.collection("groups").document(gid)
            val snap = gRef.get().await()
            val raw = snap.get("members")
            if (raw is List<*> && raw.any { it !is Map<*, *> }) {
                val updated = raw.mapNotNull { item ->
                    val email = item as? String ?: return@mapNotNull null
                    val userDoc = db.collection("users")
                        .whereEqualTo("email", email).get().await().documents.firstOrNull()
                    if (userDoc != null) {
                        mapOf(
                            "email" to email,
                            "name" to (userDoc.getString("name") ?: email.substringBefore('@')),
                            "photoUrl" to (userDoc.getString("photoUrl") ?: "")
                        )
                    } else null
                }
                gRef.update("members", updated).await()
            }


            db.collection("groups").document(gid)
                .collection("payments")
                .addSnapshotListener { s, _ ->
                    payments = s?.documents?.mapNotNull { doc ->
                        val sum = doc.getDouble("sum") ?: return@mapNotNull null
                        val comment = doc.getString("comment") ?: ""
                        val date = doc.getTimestamp("date")?.toDate()?.time ?: 0L
                        val name = doc.getString("name") ?: "?"
                        val photo = doc.getString("photoUrl") ?: ""
                        Payment(doc.id, sum, comment, date, name, photo)
                    } ?: emptyList()
                }

            db.collection("groups").document(gid).addSnapshotListener { d, _ ->
                val rawMembers = d?.get("members")
                members = if (rawMembers is List<*>) {
                    rawMembers.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        val name = (map["name"] as? String)?.ifBlank { null }
                            ?: (map["email"] as? String ?: "?").substringBefore('@')
                        val photo = map["photoUrl"] as? String ?: ""
                        Member(name, photo)
                    }
                } else emptyList()

            }
        }
    }


    val calendar = remember { Calendar.getInstance() }
    val monthPayments = payments
        .sortedByDescending { it.date } // ← вот ключ
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
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                    Icon(Icons.Default.Edit, null, Modifier.clickable { showEdit = true })
                }
                groupId?.let { gid ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Код группы: $gid", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton({
                            clipboard.setText(AnnotatedString(gid))
                            scope.launch { snack.showSnackbar("Код скопирован") }
                        }) { Icon(Icons.Default.ContentCopy, null) }
                    }
                }
                if (members.isNotEmpty()) {
                    Text("Участники:", Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                    members.forEach {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (it.photoUrl.startsWith("drawable/")) {
                                val resId = LocalContext.current.resources.getIdentifier(
                                    it.photoUrl.removePrefix("drawable/"), "drawable", LocalContext.current.packageName
                                )
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                )
                            } else if (it.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    it.photoUrl, null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(it.name)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState)
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
                    monthPayments.forEach { PaymentItem(it) { pendingDelete = it } }
                }
            }
        }
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить запись?") },
            text = { Text("Трата на ${fmt(p.sum)} ₽ будет удалена.") },
            confirmButton = {
                TextButton({
                    groupId?.let { gid ->
                        db.collection("groups").document(gid)
                            .collection("payments").document(p.id).delete()
                    }
                    pendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton({ pendingDelete = null }) { Text("Отмена") } }
        )
    }

    if (showEdit) {
        EditProfileDialog(
            currentName = userName,
            currentPhotoUrl = userPhoto,
            onDismiss = { showEdit = false },
            onSave = { newName, newPhoto ->
                scope.launch {
                    val finalName = newName.ifBlank { userName }
                    var finalPhoto = newPhoto.ifBlank { userPhoto }

                    if (newPhoto.startsWith("content://")) {
                        try {
                            val ext = context.contentResolver.getType(Uri.parse(newPhoto))?.substringAfter('/') ?: "jpg"
                            val ref = storage.reference.child("avatars/${user.uid}/${System.currentTimeMillis()}.$ext")
                            ref.putFile(Uri.parse(newPhoto)).await()
                            finalPhoto = ref.downloadUrl.await().toString()
                        } catch (e: Exception) {
                            snack.showSnackbar("Ошибка загрузки аватара")
                        }
                    }

                    db.collection("users").document(user.uid)
                        .set(mapOf("name" to finalName, "photoUrl" to finalPhoto), SetOptions.merge())
                        .await()

                    groupId?.let { gid ->
                        val gRef = db.collection("groups").document(gid)
                        val snap = gRef.get().await()
                        val rawMembers = snap.get("members")
                        val arr = if (rawMembers is List<*>) {
                            rawMembers.mapNotNull { item ->
                                val map = item as? Map<*, *> ?: return@mapNotNull null
                                if (map["email"] == user.email) {
                                    mapOf(
                                        "email" to user.email,
                                        "name" to finalName,
                                        "photoUrl" to finalPhoto
                                    )
                                } else map
                            }
                        } else emptyList()


                        gRef.update("members", arr).await()
                    }

                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(finalName)
                        .apply {
                            if (finalPhoto.isNotBlank()) {
                                setPhotoUri(Uri.parse(finalPhoto))
                            }
                        }
                        .build()

                    user.updateProfile(profileUpdates)


                    userName = finalName
                    userPhoto = finalPhoto
                    showEdit = false
                    snack.showSnackbar("Профиль сохранён")
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
    val scope = rememberCoroutineScope()

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
            TextButton({
                onSave(name.trim(), selectedAvatar)

                // 🔄 Обновление всех операций
                val finalName = name.trim()
                val finalPhoto = selectedAvatar
                val db = Firebase.firestore
                val user = Firebase.auth.currentUser
                val groupIdRef = db.collection("users").document(user!!.uid)

                scope.launch {
                    val groupIdSnap = groupIdRef.get().await()
                    val groupId = groupIdSnap.getString("groupId") ?: return@launch
                    val paymentsRef = db.collection("groups").document(groupId).collection("payments")
                    val docs = paymentsRef.whereEqualTo("name", finalName).get().await()
                    docs.documents.forEach { doc ->
                        paymentsRef.document(doc.id).update("photoUrl", finalPhoto)
                    }
                }

            }) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } }
    )
}



@Composable
private fun PaymentItem(p: Payment, askDel: (Payment) -> Unit) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val db = Firebase.firestore
    val context = LocalContext.current

    var showEdit by remember { mutableStateOf(false) }
    var newSum by remember { mutableStateOf(p.sum.toString()) }
    var newComment by remember { mutableStateOf(p.comment) }

    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp) // внешний отступ карточки
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 1.dp) // убрал лишний отступ внутри
        ) {
            if (p.photoUrl.startsWith("drawable/")) {
                val resId = context.resources.getIdentifier(
                    p.photoUrl.removePrefix("drawable/"),
                    "drawable",
                    context.packageName
                )
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(start = 8.dp) // немного сдвинем аватар
                        .size(50.dp)
                        .clip(CircleShape)
                )
            } else {
                AsyncImage(
                    p.photoUrl, null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(50.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp) // справа от текста
            ) {
                Text(fmt(p.sum) + " ₽", style = MaterialTheme.typography.titleMedium)
                if (p.comment.isNotBlank()) Text(p.comment)
                Text(sdf.format(Date(p.date)), style = MaterialTheme.typography.bodySmall)
                Text(p.name, style = MaterialTheme.typography.bodySmall)
            }

            Column {
                IconButton({ showEdit = true }) {
                    Icon(Icons.Default.Edit, null)
                }
                IconButton({ askDel(p) }) {
                    Icon(Icons.Default.Delete, null)
                }
            }
        }
    }



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
                            val userDoc = db.collection("users").document(user.uid)
                            userDoc.get().addOnSuccessListener { snapshot ->
                                val groupId = snapshot.getString("groupId")
                                if (!groupId.isNullOrBlank()) {
                                    db.collection("groups").document(groupId)
                                        .collection("payments").document(p.id)
                                        .update(mapOf(
                                            "sum" to updatedSum,
                                            "comment" to newComment
                                        ))
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


