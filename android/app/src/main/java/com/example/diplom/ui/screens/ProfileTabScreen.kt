package com.example.diplom.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.diplom.DiplomApplication
import com.example.diplom.data.AchievementDto
import com.example.diplom.data.ApiException
import com.example.diplom.data.FriendUserDto
import com.example.diplom.data.MeResponse
import com.example.diplom.data.PatchProfileRequest
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.Session
import com.example.diplom.data.WeightPostRequest
import com.example.diplom.data.apiAbsoluteUrl
import com.example.diplom.data.apiCall
import kotlinx.coroutines.launch

@Composable
private fun AchievementCard(a: AchievementDto) {
    val unlocked = a.unlocked
    Card(
        modifier = Modifier.width(196.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
        ),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (unlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
            )
            Text(
                a.title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (unlocked) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                a.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 4,
                color = if (unlocked) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                },
            )
        }
    }
}

private val goalEditOptions = listOf(
    "WEIGHT_LOSS" to "Похудение",
    "MUSCLE_GAIN" to "Набор массы",
    "MAINTENANCE" to "Поддержание формы",
    "ENDURANCE" to "Выносливость",
    "GENERAL_FITNESS" to "Общая активность",
)

private val experienceEditOptions = listOf(
    "BEGINNER" to "Начинающий",
    "INTERMEDIATE" to "Средний",
    "ADVANCED" to "Продвинутый",
)

private fun profileDisplayName(data: MeResponse): String {
    val dn = data.user.displayName?.trim()
    if (!dn.isNullOrEmpty()) return dn
    val email = data.user.email
    val local = email.substringBefore('@', email)
    return local.ifBlank { email }
}

@Composable
fun ProfileTabScreen(
    modifier: Modifier = Modifier,
    app: DiplomApplication,
    rootNav: NavController,
    onLogout: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var me by remember { mutableStateOf<MeResponse?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editHeight by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }
    var editSex by remember { mutableStateOf<String?>(null) }
    var editExp by remember { mutableStateOf("BEGINNER") }
    var editGoal by remember { mutableStateOf("GENERAL_FITNESS") }
    var editGym by remember { mutableStateOf(false) }
    var editDisplayName by remember { mutableStateOf("") }
    var newWeight by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<FriendUserDto>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val navEntry by rootNav.currentBackStackEntryAsState()
    val context = LocalContext.current

    fun reload() {
        scope.launch {
            loading = true
            val m = apiCall { RemoteApi.api.me() }
            m.onSuccess { me = it }.onFailure { error = (it as? ApiException)?.message ?: it.message }
            loading = false
        }
    }

    val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            saving = true
            error = null
            val r = apiCall { RemoteApi.uploadAvatar(context, uri) }
            saving = false
            r.onSuccess { reload() }.onFailure {
                error = (it as? ApiException)?.message ?: it.message
            }
        }
    }

    LaunchedEffect(navEntry?.destination?.route) {
        val fr = apiCall { RemoteApi.api.friendsList() }
        fr.onSuccess { friends = it.friends }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Профиль", style = MaterialTheme.typography.headlineSmall)
        if (loading && me == null) {
            CircularProgressIndicator()
            return@Column
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        me?.let { data ->
            val p = data.profile
            val resolvedAvatar = apiAbsoluteUrl(data.user.avatarUrl)
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (resolvedAvatar != null) {
                        AsyncImage(
                            model = resolvedAvatar,
                            contentDescription = "Аватар",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        profileDisplayName(data),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        data.user.email,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    val prof = p ?: return@OutlinedButton
                    editDisplayName = data.user.displayName?.trim() ?: ""
                    editHeight = prof.heightCm?.toString() ?: ""
                    editWeight = prof.weightKg?.let { String.format("%.1f", it) } ?: ""
                    editAge = prof.age?.toString() ?: ""
                    editSex = prof.sex
                    editExp = prof.experienceLevel ?: "BEGINNER"
                    editGoal = prof.trainingGoal ?: "GENERAL_FITNESS"
                    editGym = prof.gymAccess == true
                    showEditDialog = true
                },
                enabled = !saving && p?.surveyCompletedAt != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Редактировать") }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Друзья", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { rootNav.navigate("friendsSearch") }) { Text("Добавить") }
            }
            if (friends.isEmpty()) {
                Text(
                    "Пока нет друзей. Нажмите «Добавить», чтобы найти человека по email.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(friends, key = { it.id }) { f ->
                        val resolved = apiAbsoluteUrl(f.avatarUrl)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(76.dp)
                                .clickable { rootNav.navigate("friendProfile/${f.id}") },
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (resolved != null) {
                                    AsyncImage(
                                        model = resolved,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                f.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Text("Достижения", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                data.achievements.orEmpty().forEach { ach -> AchievementCard(ach) }
            }
            Button(
                onClick = {
                    newWeight = p?.weightKg?.let { String.format("%.1f", it) } ?: ""
                    showWeightDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Записать вес") }
            Button(onClick = { rootNav.navigate("subscription") }, modifier = Modifier.fillMaxWidth()) {
                Text("Подписка")
            }
            Button(
                onClick = {
                    scope.launch {
                        Session.token = null
                        app.tokenStore.setToken(null)
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Выйти") }
        }
    }

    if (showWeightDialog) {
        AlertDialog(
            onDismissRequest = { if (!saving) showWeightDialog = false },
            title = { Text("Новый вес (кг)") },
            text = {
                OutlinedTextField(
                    value = newWeight,
                    onValueChange = { newWeight = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.replace(',', '.') },
                    singleLine = true,
                    label = { Text("Вес") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val w = newWeight.toDoubleOrNull() ?: return@TextButton
                        saving = true
                        scope.launch {
                            val r = apiCall { RemoteApi.api.postWeight(WeightPostRequest(weightKg = w)) }
                            saving = false
                            r.onSuccess {
                                showWeightDialog = false
                                reload()
                            }.onFailure {
                                error = (it as? ApiException)?.message ?: it.message
                            }
                        }
                    },
                    enabled = !saving,
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }, enabled = !saving) { Text("Отмена") }
            },
        )
    }

    if (showEditDialog) {
        val editScroll = rememberScrollState()
        val sexRowScroll = rememberScrollState()
        AlertDialog(
            onDismissRequest = { if (!saving) showEditDialog = false },
            title = { Text("Редактировать профиль") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(editScroll),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val editAvatarUrl = apiAbsoluteUrl(me?.user?.avatarUrl)
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (editAvatarUrl != null) {
                            AsyncImage(
                                model = editAvatarUrl,
                                contentDescription = "Аватар",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Сменить фото") }
                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { new ->
                            if (new.length <= 80) editDisplayName = new
                        },
                        singleLine = true,
                        label = { Text("Имя в профиле") },
                        placeholder = { Text("Например: Анна Иванова") },
                        supportingText = { Text("До 80 символов, можно на русском") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editHeight,
                        onValueChange = { editHeight = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        label = { Text("Рост, см") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editWeight,
                        onValueChange = {
                            editWeight = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.replace(',', '.')
                        },
                        singleLine = true,
                        label = { Text("Вес, кг") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editAge,
                        onValueChange = { editAge = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        label = { Text("Возраст") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Пол", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(sexRowScroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = editSex == null,
                            onClick = { editSex = null },
                            label = { Text("Не указан") },
                        )
                        FilterChip(
                            selected = editSex == "MALE",
                            onClick = { editSex = "MALE" },
                            label = { Text("Муж.") },
                        )
                        FilterChip(
                            selected = editSex == "FEMALE",
                            onClick = { editSex = "FEMALE" },
                            label = { Text("Жен.") },
                        )
                        FilterChip(
                            selected = editSex == "OTHER",
                            onClick = { editSex = "OTHER" },
                            label = { Text("Другое") },
                        )
                    }
                    Text("Опыт", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        experienceEditOptions.forEach { (k, label) ->
                            FilterChip(
                                selected = editExp == k,
                                onClick = { editExp = k },
                                label = { Text(label) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Text("Цель тренировок", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        goalEditOptions.forEach { (k, label) ->
                            FilterChip(
                                selected = editGoal == k,
                                onClick = { editGoal = k },
                                label = { Text(label) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Доступ к залу", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = editGym, onCheckedChange = { editGym = it })
                    }
                    Text(
                        "При смене цели, опыта или зала программа подбирается заново. При изменении веса добавляется запись в историю.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val h = editHeight.toIntOrNull()
                        val w = editWeight.toDoubleOrNull()
                        val a = editAge.toIntOrNull()
                        if (h == null || h !in 100..250) {
                            error = "Рост: от 100 до 250 см"
                            return@TextButton
                        }
                        if (w == null || w < 30 || w > 250) {
                            error = "Вес: от 30 до 250 кг"
                            return@TextButton
                        }
                        if (a == null || a !in 10..99) {
                            error = "Возраст: от 10 до 99"
                            return@TextButton
                        }
                        saving = true
                        error = null
                        scope.launch {
                            val r = apiCall {
                                RemoteApi.api.patchProfile(
                                    PatchProfileRequest(
                                        heightCm = h,
                                        weightKg = w,
                                        age = a,
                                        sex = editSex,
                                        experienceLevel = editExp,
                                        trainingGoal = editGoal,
                                        gymAccess = editGym,
                                        displayName = editDisplayName.trim(),
                                    ),
                                )
                            }
                            saving = false
                            r.onSuccess {
                                showEditDialog = false
                                reload()
                            }.onFailure {
                                error = (it as? ApiException)?.message ?: it.message
                            }
                        }
                    },
                    enabled = !saving,
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }, enabled = !saving) { Text("Отмена") }
            },
        )
    }
}
