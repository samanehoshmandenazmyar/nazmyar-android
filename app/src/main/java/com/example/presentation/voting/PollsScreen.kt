package com.example.presentation.voting

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.voting.PollsManager
import com.example.domain.model.Poll
import com.example.domain.model.PollOption
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollsScreen(
    currentUserId: String = "",
    currentUserRole: UserRole = UserRole.STUDENT,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(currentUserId) {
        PollsManager.init(context, currentUserId)
    }

    val polls by PollsManager.polls.collectAsState()
    val userVotesMap by PollsManager.userVotesMap.collectAsState()
    val votesCountMap by PollsManager.votesCountMap.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteConfirmPollId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "سامانه نظرسنجی و رای‌گیری",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SchoolBluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(
                        text = "ایجاد نظرسنجی جدید",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            if (polls.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Poll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "هیچ نظرسنجی فعال نیست",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(polls, key = { it.id }) { poll ->
                        val userVotedOptionId = userVotesMap[poll.id]
                        val pollOptionCounts = votesCountMap[poll.id] ?: emptyMap()

                        PollCardItem(
                            poll = poll,
                            userVotedOptionId = userVotedOptionId,
                            optionVoteCounts = pollOptionCounts,
                            onVote = { optionId ->
                                val success = PollsManager.castVote(poll.id, optionId)
                                if (success) {
                                    Toast.makeText(context, "رأی شما ثبت گردید", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onToggleActive = {
                                PollsManager.togglePollActive(poll.id)
                            },
                            onDelete = {
                                deleteConfirmPollId = poll.id
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Poll Dialog
    if (showCreateDialog) {
        CreatePollDialog(
            onDismiss = { showCreateDialog = false },
            onCreatePoll = { newPoll ->
                PollsManager.addPoll(newPoll)
                showCreateDialog = false
                Toast.makeText(context, "نظرسنجی جدید با موفقیت ایجاد شد", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Delete Poll Confirmation Dialog
    if (deleteConfirmPollId != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmPollId = null },
            title = {
                Text(
                    text = "حذف نظرسنجی",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "آیا از حذف این نظرسنجی اطمینان دارید؟ تمامی آراء ثبت‌شده حذف خواهند شد.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VazirmatnFontFamily
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmPollId?.let { id ->
                            PollsManager.deletePoll(id)
                        }
                        deleteConfirmPollId = null
                        Toast.makeText(context, "نظرسنجی حذف شد", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف قطعی", fontFamily = VazirmatnFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPollId = null }) {
                    Text("انصراف", fontFamily = VazirmatnFontFamily)
                }
            }
        )
    }
}

@Composable
private fun PollCardItem(
    poll: Poll,
    userVotedOptionId: String?,
    optionVoteCounts: Map<String, Int>,
    onVote: (String) -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val totalVotes = optionVoteCounts.values.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (poll.isActive) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.5.dp, if (poll.isActive) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = if (poll.isActive) "فعال" else "پایان یافته",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = if (poll.isActive) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (!poll.isPermanent) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(0.5.dp, Color(0xFFF59E0B))
                        ) {
                            Text(
                                text = if (poll.expiryDate.isNotBlank()) "موقت (تا ${poll.expiryDate.toPersianDigits()})" else "موقت",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = poll.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "تنظیمات نظرسنجی",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (poll.isActive) "بستن نظرسنجی" else "فعال‌سازی مجدد",
                                    fontFamily = VazirmatnFontFamily
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleActive()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "حذف نظرسنجی",
                                    color = MaterialTheme.colorScheme.error,
                                    fontFamily = VazirmatnFontFamily
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            if (poll.description.isNotBlank()) {
                Text(
                    text = poll.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Total votes indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HowToVote,
                    contentDescription = null,
                    tint = SchoolBluePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "تعداد کل آراء: ${totalVotes.toPersianDigits()} رأی",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        color = SchoolBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Options List
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                poll.options.forEach { option ->
                    val optVotes = optionVoteCounts[option.id] ?: option.baseVotes
                    val percentage = if (totalVotes > 0) (optVotes.toFloat() / totalVotes.toFloat()) else 0f
                    val isSelected = (userVotedOptionId == option.id)

                    PollOptionRow(
                        option = option,
                        votesCount = optVotes,
                        percentage = percentage,
                        isSelected = isSelected,
                        isPollActive = poll.isActive,
                        onSelect = { onVote(option.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PollOptionRow(
    option: PollOption,
    votesCount: Int,
    percentage: Float,
    isSelected: Boolean,
    isPollActive: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        onClick = { if (isPollActive) onSelect() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) SchoolBlueContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) SchoolBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Option Candidate Image (or Avatar Box)
                CandidateImageThumbnail(
                    imageUrl = option.imageUrl,
                    imageRes = option.imageRes,
                    title = option.title
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (option.description.isNotBlank()) {
                        Text(
                            text = option.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                RadioButton(
                    selected = isSelected,
                    onClick = { if (isPollActive) onSelect() },
                    enabled = isPollActive
                )
            }

            // Progress Bar & Vote Stats
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isSelected) SchoolBluePrimary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(percentage * 100).toInt().toPersianDigits()}٪",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Text(
                        text = "${votesCount.toPersianDigits()} رأی",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateImageThumbnail(
    imageUrl: String?,
    imageRes: Int?,
    title: String
) {
    val size = 48.dp

    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, SchoolBluePrimary.copy(alpha = 0.4f), CircleShape)
        )
    } else if (imageRes != null && imageRes != 0) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, SchoolBluePrimary.copy(alpha = 0.4f), CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(SchoolBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(1),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = VazirmatnFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SchoolBluePrimary
                )
            )
        }
    }
}

private data class TempOptionState(
    var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var description: String = "",
    var imageUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePollDialog(
    onDismiss: () -> Unit,
    onCreatePoll: (Poll) -> Unit
) {
    val context = LocalContext.current
    var pollTitle by remember { mutableStateOf("") }
    var pollDescription by remember { mutableStateOf("") }
    var isPermanent by remember { mutableStateOf(true) }
    var expiryDateInput by remember { mutableStateOf(com.example.core.utils.DateTimeUtils.getCurrentPersianDate()) }
    var activePickingOptionIndex by remember { mutableStateOf<Int?>(null) }

    val tempOptions = remember {
        mutableStateListOf(
            TempOptionState(title = "گزینه اول"),
            TempOptionState(title = "گزینه دوم")
        )
    }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        val idx = activePickingOptionIndex
        if (uri != null && idx != null && idx in 0 until tempOptions.size) {
            val targetIdx: Int = idx
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                    val bytes = stream.toByteArray()
                    val base64Img = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    tempOptions[targetIdx] = tempOptions[targetIdx].copy(imageUrl = base64Img)
                } else {
                    tempOptions[targetIdx] = tempOptions[targetIdx].copy(imageUrl = uri.toString())
                }
            } catch (_: Exception) {
                tempOptions[targetIdx] = tempOptions[targetIdx].copy(imageUrl = uri.toString())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ایجاد نظرسنجی جدید",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = VazirmatnFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = pollTitle,
                    onValueChange = { pollTitle = it },
                    label = { Text("عنوان / موضوع نظرسنجی *", fontFamily = VazirmatnFontFamily) },
                    placeholder = { Text("مثلاً: انتخاب مکان اردوی علمی یا موضوع کنفرانس", fontFamily = VazirmatnFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = pollDescription,
                    onValueChange = { pollDescription = it },
                    label = { Text("توضیحات تکمیلی (اختیاری)", fontFamily = VazirmatnFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // POLL TYPE SELECTOR (دائمی vs موقت)
                Text(
                    text = "نوع مهلت نظرسنجی:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Permanent Choice
                    Surface(
                        onClick = { isPermanent = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPermanent) SchoolBluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            width = if (isPermanent) 1.8.dp else 1.dp,
                            color = if (isPermanent) SchoolBluePrimary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "دائمی (همیشگی)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPermanent) SchoolBluePrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "بدون تگ مهلت",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Temporary Choice
                    Surface(
                        onClick = { isPermanent = false },
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isPermanent) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            width = if (!isPermanent) 1.8.dp else 1.dp,
                            color = if (!isPermanent) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "موقت (دارای مهلت)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isPermanent) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "با نمایش تگ موقت",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // If Temporary, show Expiry Date field
                if (!isPermanent) {
                    OutlinedTextField(
                        value = expiryDateInput,
                        onValueChange = { expiryDateInput = it },
                        label = { Text("تاریخ مهلت / انقضاء نظرسنجی *", fontFamily = VazirmatnFontFamily) },
                        placeholder = { Text("مثال: ۱۴۰۳/۰۹/۱۵", fontFamily = VazirmatnFontFamily) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "گزینه‌های نظرسنجی (${tempOptions.size.toPersianDigits()})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    TextButton(
                        onClick = {
                            tempOptions.add(TempOptionState(title = ""))
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("افزودن گزینه جدید", fontFamily = VazirmatnFontFamily)
                        }
                    }
                }

                tempOptions.forEachIndexed { index, optionState ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "گزینه ${(index + 1).toPersianDigits()}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = SchoolBluePrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                if (tempOptions.size > 2) {
                                    IconButton(
                                        onClick = { tempOptions.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف گزینه",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = optionState.title,
                                onValueChange = {
                                    tempOptions[index] = optionState.copy(title = it)
                                },
                                label = { Text("نام گزینه *", fontFamily = VazirmatnFontFamily) },
                                placeholder = { Text("مثال: گزینه ۱، پارک علم و فناوری...", fontFamily = VazirmatnFontFamily) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = optionState.description,
                                onValueChange = {
                                    tempOptions[index] = optionState.copy(description = it)
                                },
                                label = { Text("توضیحات گزینه (اختیاری)", fontFamily = VazirmatnFontFamily) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Pick photo from device storage
                            if (optionState.imageUrl.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AsyncImage(
                                            model = optionState.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, SchoolBluePrimary, CircleShape)
                                        )
                                        Text(
                                            text = "عکس از دستگاه انتخاب شد ✓",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                color = SchoolBluePrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    IconButton(
                                        onClick = { tempOptions[index] = optionState.copy(imageUrl = "") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف عکس",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        activePickingOptionIndex = index
                                        imagePickerLauncher.launch("image/*")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "افزودن عکس گزینه از حافظه دستگاه (اختیاری)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = VazirmatnFontFamily)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pollTitle.isBlank()) return@Button
                    val validOptions = tempOptions.filter { it.title.isNotBlank() }
                    if (validOptions.size < 2) return@Button

                    val pollOptions = validOptions.map { opt ->
                        PollOption(
                            id = UUID.randomUUID().toString(),
                            title = opt.title.trim(),
                            description = opt.description.trim(),
                            imageUrl = opt.imageUrl.ifBlank { null }
                        )
                    }

                    val newPoll = Poll(
                        id = "poll_" + System.currentTimeMillis(),
                        title = pollTitle.trim(),
                        description = pollDescription.trim(),
                        options = pollOptions,
                        isActive = true,
                        isPermanent = isPermanent,
                        expiryDate = if (!isPermanent) expiryDateInput.trim() else ""
                    )

                    onCreatePoll(newPoll)
                },
                enabled = pollTitle.isNotBlank() && tempOptions.count { it.title.isNotBlank() } >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
            ) {
                Text("ثبت و انتشار نظرسنجی", fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", fontFamily = VazirmatnFontFamily)
            }
        }
    )
}
