package com.example.presentation.messenger

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.messenger.ClassMessengerManager
import com.example.domain.model.*
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassChatScreen(
    classGroupId: String,
    currentUser: User?,
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val group = remember(classGroupId) { ClassMessengerManager.getGroupById(classGroupId) }

    val allMessagesMap by ClassMessengerManager.groupMessages.collectAsState()
    val messages = allMessagesMap[classGroupId] ?: emptyList()
    val allStudents by mainViewModel.students.collectAsState()

    val isUserAllowedToMessage = remember(currentUser, group, allStudents) {
        if (currentUser == null) false
        else if (currentUser.isCreator || currentUser.role == UserRole.MANAGER || currentUser.role == UserRole.VICE_PRINCIPAL || currentUser.role == UserRole.TEACHER || currentUser.role == UserRole.COUNSELOR) true
        else {
            if (currentUser.role == UserRole.STUDENT) {
                val student = allStudents.firstOrNull {
                    it.id == currentUser.id ||
                    (it.studentCode.isNotBlank() && it.studentCode == currentUser.studentCode) ||
                    it.fullName.equals(currentUser.name, ignoreCase = true) ||
                    it.fullName.replace(" ", "").equals(currentUser.name.replace(" ", ""), ignoreCase = true)
                }
                val studentKey = if (student != null) {
                    SchoolClasses.getStudentClassKey(student.grade, student.className)
                } else {
                    val assigned = currentUser.assignedClasses.firstOrNull() ?: ""
                    SchoolClasses.getStudentClassKey(assigned, assigned)
                }
                val groupKey = group.title.replace("کلاس ", "").trim()
                studentKey == groupKey
            } else if (currentUser.role == UserRole.PARENT) {
                val myChildren = allStudents.filter { currentUser.childStudentIds.contains(it.id) || (it.studentCode.isNotBlank() && it.studentCode == currentUser.studentCode) }
                myChildren.any {
                    val childKey = SchoolClasses.getStudentClassKey(it.grade, it.className)
                    val groupKey = group.title.replace("کلاس ", "").trim()
                    childKey == groupKey
                }
            } else false
        }
    }

    var inputText by remember { mutableStateOf("") }
    var selectedAttendanceCardForDialog by remember { mutableStateOf<AttendanceCardData?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(colors = listOf(Color(group.avatarColorHex), Color(group.avatarColorHex).copy(alpha = 0.8f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = group.gradeCode.toPersianDigits(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = group.title, style = MaterialTheme.typography.titleSmall.copy(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold))
                            Text(text = "${group.memberCount.toPersianDigits()} عضو", style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
                actions = {
                    if (isUserAllowedToMessage) {
                        IconButton(onClick = { if (currentUser != null) ClassMessengerManager.sendAttendanceCard(classGroupId, currentUser) }) {
                            Icon(Icons.Default.Assignment, contentDescription = "ارسال فرم حضور و غیاب", tint = SchoolBluePrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFEFE9E0))
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(SchoolBluePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "گروه ${group.title}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "این گروه به سرور متصل است و در حال حاضر پیامی ندارد.\nاولین پیام را ارسال کنید!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val isMe = (currentUser != null && (message.senderId == currentUser.id || message.senderName == currentUser.name))
                        if (message.isAttendanceCard && message.attendanceData != null) {
                            TelegramAttendanceCard(message, message.attendanceData, isMe, currentUser, classGroupId) { selectedAttendanceCardForDialog = message.attendanceData }
                        } else {
                            TelegramMessageBubble(message, isMe)
                        }
                    }
                }
            }

            if (isUserAllowedToMessage) {
                TelegramBottomInputBar(
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    onSendClick = {
                        if (inputText.isNotBlank() && currentUser != null) {
                            ClassMessengerManager.sendMessage(classGroupId, currentUser, inputText)
                            inputText = ""
                        }
                    },
                    onSendAttendanceClick = {
                        if (currentUser != null) ClassMessengerManager.sendAttendanceCard(classGroupId, currentUser)
                    }
                )
            } else {
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (currentUser?.role == UserRole.STUDENT) {
                            "شما دانش‌آموز این کلاس نیستید و فقط مجاز به ارسال پیام در گروه کلاس خود هستید."
                        } else {
                            "شما دسترسی ارسال پیام به این کلاس را ندارید."
                        },
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center,
                        color = IncidentRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                    )
                }
            }
        }
    }

    if (selectedAttendanceCardForDialog != null) {
        val currentCardData = messages.firstOrNull { it.attendanceData?.id == selectedAttendanceCardForDialog?.id }?.attendanceData ?: selectedAttendanceCardForDialog!!
        AttendanceAttendeesDialog(currentCardData) { selectedAttendanceCardForDialog = null }
    }
}

@Composable
fun TelegramMessageBubble(message: ChatMessage, isMe: Boolean) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isMe) 16.dp else 4.dp, bottomEnd = if (isMe) 4.dp else 16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isMe) Color(0xFFEFFDDE) else Color(0xFFFFFFFF)),
            modifier = Modifier.widthIn(min = 90.dp, max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                if (!isMe) {
                    Text(text = message.senderName, style = MaterialTheme.typography.labelMedium.copy(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFF007ACC)))
                }
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily))
                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = message.timeFormatted, style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily, fontSize = 10.sp, color = Color(0xFF94A3B8)))
                    if (isMe) Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF2AABEE), modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
fun TelegramAttendanceCard(message: ChatMessage, attendanceData: AttendanceCardData, isMe: Boolean, currentUser: User?, classGroupId: String, onViewAttendeesClick: () -> Unit) {
    val isUserAttended = currentUser != null && ClassMessengerManager.isUserAttended(message.id, classGroupId, currentUser.id, currentUser.name)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(0.96f).padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "فرم حضور و غیاب ${attendanceData.classTitle}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold))
            Button(
                onClick = { if (currentUser != null) ClassMessengerManager.toggleAttendance(message.id, classGroupId, currentUser) },
                colors = ButtonDefaults.buttonColors(containerColor = if (isUserAttended) Color(0xFFE8F5E9) else PositiveGreen)
            ) { Text(if (isUserAttended) "شما حاضر زدید ✓" else "ثبت حضور (حاضرم) ✋", fontFamily = VazirmatnFontFamily) }
            TextButton(onClick = onViewAttendeesClick) { Text("مشاهده اسامی حاضرین (${attendanceData.attendees.size.toPersianDigits()} نفر)", fontFamily = VazirmatnFontFamily) }
        }
    }
}

@Composable
fun AttendanceAttendeesDialog(attendanceData: AttendanceCardData, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("لیست حاضرین", fontFamily = VazirmatnFontFamily) }, text = {
        LazyColumn { items(attendanceData.attendees) { attendee -> Text(attendee.userName, fontFamily = VazirmatnFontFamily) } }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("بستن", fontFamily = VazirmatnFontFamily) } })
}

@Composable
fun TelegramBottomInputBar(inputText: String, onInputTextChange: (String) -> Unit, onSendClick: () -> Unit, onSendAttendanceClick: () -> Unit) {
    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSendAttendanceClick) { Icon(Icons.Default.Assignment, contentDescription = null) }
            OutlinedTextField(value = inputText, onValueChange = onInputTextChange, modifier = Modifier.weight(1f), shape = RoundedCornerShape(22.dp))
            IconButton(onClick = onSendClick) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) }
        }
    }
}
