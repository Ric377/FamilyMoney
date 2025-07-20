// файле: /ui/MainDrawer.kt
package com.rich.familymoney.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rich.familymoney.repository.GroupMember // ИСПРАВЛЕНО: Добавлен импорт
import com.rich.familymoney.viewmodel.MainScreenState // ИСПРАВЛЕНО: Правильное имя класса состояния

@Composable
fun MainDrawerContent(
    state: MainScreenState, // ИСПРАВЛЕНО: Используем правильное имя класса
    userName: String,
    userPhoto: String,
    groupId: String,
    onCloseDrawer: () -> Unit,
    onEditProfileClick: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onLeaveGroupClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Box(
        modifier = Modifier.width(280.dp)
    ) {
        ModalDrawerSheet {
            // Блок пользователя
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
                Icon(Icons.Default.Edit, null, Modifier.clickable(onClick = onEditProfileClick))
            }

            HorizontalDivider()

            // Название и код группы
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                Text(
                    text = "Группа:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.groupName.ifEmpty { "Загрузка..." },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Код: $groupId", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton({
                    clipboard.setText(AnnotatedString(groupId))
                }) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp)) }
            }

            // Участники
            if (state.members.isNotEmpty()) {
                Text("Участники:", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                // ИСПРАВЛЕНО: Оборачиваем цикл в Column для создания Composable контекста
                Column {
                    state.members.forEach { member ->
                        MemberRow(member = member) // Выносим в отдельную функцию для чистоты
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Кнопки навигации
            NavigationDrawerItem(
                label = { Text("Расчёт долгов") },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigateToDebts()
                },
                icon = { Icon(Icons.Default.Calculate, null) }
            )
            NavigationDrawerItem(
                label = { Text("Сменить группу") },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onLeaveGroupClick()
                },
                icon = { Icon(Icons.Default.Group, null) }
            )
            NavigationDrawerItem(
                label = { Text("Выйти") },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onLogoutClick()
                },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) }
            )
        }
    }
}

// Новая Composable функция для отображения одного участника
@Composable
private fun MemberRow(member: GroupMember) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (member.photoUrl.startsWith("drawable/")) {
            val resId = context.resources.getIdentifier(
                member.photoUrl.removePrefix("drawable/"), "drawable", context.packageName
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