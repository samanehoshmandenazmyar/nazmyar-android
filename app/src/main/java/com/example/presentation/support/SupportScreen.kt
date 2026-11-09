package com.example.presentation.support

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.support.SupportManager
import com.example.domain.model.Student
import com.example.domain.model.SupportMessage
import com.example.domain.model.SupportTicket
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.behavior.matchesClassFilter
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SupportScreen(
    user: User?,
    allStudents: List<Student>,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SupportManager.init(context)
    }

    val allTickets by SupportManager.tickets.collectAsState()

    // Support Staff / Creator Check
    val isSupportStaff = user?.isSupportStaff == true
    val isGuest = (user == null)

    // Unregistered Guest Matched Student State (cleared each time guest exits and re-enters!)
    var matchedGuestStudent by remember { mutableStateOf<Student?>(null) }

    // Active User Identity
    val userId = if (!isGuest) (user?.id ?: "guest") else (matchedGuestStudent?.id ?: "guest_unselected")
    val userName = if (!isGuest) (user?.name.orEmpty()) else (matchedGuestStudent?.fullName ?: "")
    val phoneOrCode = if (!isGuest) (user?.phone?.ifBlank { user?.studentCode }.orEmpty()) else (matchedGuestStudent?.studentCode ?: "مهمان")
    val roleTitle = if (!isGuest) (user?.rankTitle?.ifBlank { user?.role?.farsiTitle } ?: "کاربر") else "${matchedGuestStudent?.grade ?: ""} - ${matchedGuestStudent?.className ?: "دانش‌آموز"}"

    // Load/Get Ticket for active user
    val userTicket = remember(allTickets, userId, userName) {
        if (userId.isNotBlank() && userId != "guest_unselected") {
            SupportManager.getOrCreateTicketForUser(userId, userName, phoneOrCode, roleTitle)
        } else null
    }

    var selectedTab by remember { mutableIntStateOf(if (isSupportStaff) 0 else 1) }
    var selectedStaffTicketId by remember { mutableStateOf<String?>(null) }
    var staffSearchQuery by remember { mutableStateOf("") }
    var staffClassFilter by remember { mutableStateOf("همه") }

    val handleBackAction = {
        if (isSupportStaff && selectedStaffTicketId != null) {
            selectedStaffTicketId = null
        } else {
            // Clear guest matched student on exit so next time they must re-type & match!
            matchedGuestStudent = null
            onBackClick()
        }
    }

    BackHandler {
        handleBackAction()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("support_screen")
    ) {
        // Top Bar: Clean Title strictly "پشتیبانی سامانه نظم یار"
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SchoolBluePrimary,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { handleBackAction() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HeadsetMic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "پشتیبانی سامانه نظم یار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                }

                // Support Staff Tab Selection Bar
                if (isSupportStaff) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = Color.White,
                                    height = 3.dp
                                )
                            }
                        }
                    ) {
                        val unreadTotal = allTickets.count { it.unreadBySupport }
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; selectedStaffTicketId = null },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("پیام‌های پشتیبانی کاربران", fontWeight = FontWeight.Bold, fontFamily = VazirmatnFontFamily, fontSize = 13.sp)
                                    if (unreadTotal > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Red
                                        ) {
                                            Text(
                                                text = unreadTotal.toString().toPersianDigits(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("پشتیبانی شخصی من", fontWeight = FontWeight.Bold, fontFamily = VazirmatnFontFamily, fontSize = 13.sp) }
                        )
                    }
                }
            }
        }

        // Body Content
        if (isSupportStaff && selectedTab == 0 && selectedStaffTicketId == null) {
            // Staff View: All User Tickets Sorted & Filtered
            SupportStaffTicketsListView(
                allTickets = allTickets,
                allStudents = allStudents,
                searchQuery = staffSearchQuery,
                onSearchQueryChange = { staffSearchQuery = it },
                selectedClassFilter = staffClassFilter,
                onClassFilterChange = { staffClassFilter = it },
                onSelectTicket = { ticket ->
                    selectedStaffTicketId = ticket.id
                    SupportManager.markTicketReadBySupport(ticket.id)
                }
            )
        } else if (isSupportStaff && selectedTab == 0 && selectedStaffTicketId != null) {
            // Staff View: Active Chat with Selected Ticket
            val ticket = allTickets.firstOrNull { it.id == selectedStaffTicketId }
            if (ticket != null) {
                SupportChatConversationView(
                    ticket = ticket,
                    currentUser = user,
                    isStaffReplying = true,
                    headerTitle = "گفتگو با: ${ticket.userName} (${ticket.userRoleTitle})",
                    onBackToList = { selectedStaffTicketId = null }
                )
            } else {
                selectedStaffTicketId = null
            }
        } else if (isGuest && matchedGuestStudent == null) {
            // Unregistered Guest Mode: Input Name & Match against School Student List (`allStudents`)!
            UnregisteredGuestNameMatchingInputView(
                allStudents = allStudents,
                onConfirmStudent = { student ->
                    matchedGuestStudent = student
                }
            )
        } else if (userTicket != null) {
            // Normal User Chat Mode (or Confirmed Guest Matched Student)
            SupportChatConversationView(
                ticket = userTicket,
                currentUser = user,
                isStaffReplying = false
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("در حال بارگذاری...", fontFamily = VazirmatnFontFamily)
            }
        }
    }
}

@Composable
fun UnregisteredGuestNameMatchingInputView(
    allStudents: List<Student>,
    onConfirmStudent: (Student) -> Unit
) {
    var typedName by remember { mutableStateOf("") }

    // Search and match student from school student list (`allStudents`)
    val matchedStudent = remember(typedName, allStudents) {
        val cleanTyped = typedName.trim().replace("  ", " ")
        if (cleanTyped.length >= 3) {
            allStudents.firstOrNull { st ->
                st.fullName.trim().replace("  ", " ").equals(cleanTyped, ignoreCase = true) ||
                        st.fullName.trim().replace("  ", " ").contains(cleanTyped, ignoreCase = true)
            }
        } else null
    }

    val suggestions = remember(typedName, allStudents) {
        val cleanTyped = typedName.trim().replace("  ", " ")
        if (cleanTyped.length >= 2) {
            allStudents.filter { st ->
                st.fullName.contains(cleanTyped, ignoreCase = true) ||
                        st.studentCode.contains(cleanTyped)
            }.take(3)
        } else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("unregistered_guest_name_input_view"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SchoolBluePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SchoolBluePrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "پشتیبانی و راهنمایی نظم یار",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SchoolBluePrimary,
                    fontFamily = VazirmatnFontFamily
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "جهت ورود به چت پشتیبانی رسمی خود، نام و نام خانوادگی خود را طبق لیست مدرسه وارد کنید:",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = VazirmatnFontFamily
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = typedName,
                    onValueChange = { typedName = it },
                    label = { Text("نام و نام خانوادگی", fontFamily = VazirmatnFontFamily) },
                    placeholder = { Text("مثلاً: علی محمدی", fontSize = 13.sp) },
                    trailingIcon = {
                        if (matchedStudent != null) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(PositiveGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تأیید نام",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guest_name_input_field"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (matchedStudent != null) PositiveGreen else SchoolBluePrimary,
                        unfocusedBorderColor = if (matchedStudent != null) PositiveGreen.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Sub-label showing matched student details
                if (matchedStudent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ دانش‌آموز یافت شد: ${matchedStudent.fullName} | پایه ${matchedStudent.grade} - ${matchedStudent.className}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = PositiveGreen,
                        textAlign = TextAlign.Center,
                        fontFamily = VazirmatnFontFamily
                    )
                } else if (typedName.trim().length >= 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ نام وارد شده در لیست دانش‌آموزان مدرسه یافت نشد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontFamily = VazirmatnFontFamily
                    )
                }

                // Autocomplete Suggestions List
                if (suggestions.isNotEmpty() && matchedStudent == null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("پیشنهادات لیست مدرسه:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        suggestions.forEach { st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { typedName = st.fullName }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(st.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("پایه ${st.grade} (${st.className})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (matchedStudent != null) {
                            onConfirmStudent(matchedStudent)
                        }
                    },
                    enabled = matchedStudent != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (matchedStudent != null) PositiveGreen else SchoolBluePrimary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("guest_confirm_name_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "تأیید",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تأیید و ورود به پشتیبانی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun SupportStaffTicketsListView(
    allTickets: List<SupportTicket>,
    allStudents: List<Student>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedClassFilter: String,
    onClassFilterChange: (String) -> Unit,
    onSelectTicket: (SupportTicket) -> Unit
) {
    val classFilterOptions = listOf("همه", "7/1", "7/2", "8/1", "8/2", "9/1", "9/2")

    val studentMap = remember(allStudents) {
        allStudents.associateBy { it.id }
    }

    val filteredSortedTickets = remember(allTickets, studentMap, searchQuery, selectedClassFilter) {
        allTickets.filter { ticket ->
            val matchesQuery = searchQuery.isBlank() ||
                    ticket.userName.contains(searchQuery, ignoreCase = true) ||
                    ticket.userPhoneOrCode.contains(searchQuery) ||
                    ticket.userRoleTitle.contains(searchQuery, ignoreCase = true) ||
                    ticket.lastMessage.contains(searchQuery, ignoreCase = true)

            val student = studentMap[ticket.userId]
            val matchesGrade = if (student != null) {
                matchesClassFilter(student, selectedClassFilter)
            } else {
                selectedClassFilter == "همه" || ticket.userRoleTitle.contains(selectedClassFilter)
            }

            matchesQuery && matchesGrade
        }.sortedByDescending { it.lastUpdated }
    }

    var ticketToDelete by remember { mutableStateOf<SupportTicket?>(null) }

    ticketToDelete?.let { ticket ->
        AlertDialog(
            onDismissRequest = { ticketToDelete = null },
            title = {
                Text(
                    text = "حذف گفتگو",
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
            },
            text = {
                Text(
                    text = "می‌خواهید گفتگوی «${ticket.userName}» را چگونه حذف کنید؟",
                    fontFamily = VazirmatnFontFamily
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            SupportManager.deleteTicketForEveryone(ticket.id)
                            ticketToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IncidentRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حذف برای دو طرف", color = Color.White, fontFamily = VazirmatnFontFamily)
                    }

                    Button(
                        onClick = {
                            SupportManager.deleteTicketLocally(ticket.id)
                            ticketToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حذف فقط برای من", color = Color.White, fontFamily = VazirmatnFontFamily)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { ticketToDelete = null }) {
                    Text("انصراف", fontFamily = VazirmatnFontFamily)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("support_staff_tickets_list_view")
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("جستجو در نام، کد یا پیام کاربران...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(classFilterOptions) { filter ->
                val isSelected = selectedClassFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { onClassFilterChange(filter) },
                    label = {
                        Text(
                            text = if (filter == "همه") "تمام کلاس‌ها" else "کلاس $filter",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SchoolBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredSortedTickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هیچ پیام یا تیکت پشتیبانی یافت نشد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = VazirmatnFontFamily
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSortedTickets, key = { it.id }) { ticket ->
                    val unreadCount = if (ticket.unreadBySupport) {
                        ticket.messages.count { !it.isFromSupport }
                    } else 0

                    val timeFormatted = if (ticket.lastUpdated > 0) {
                        SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault()).format(Date(ticket.lastUpdated)).toPersianDigits()
                    } else ""

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .pointerInput(ticket.id) {
                                detectTapGestures(
                                    onTap = { onSelectTicket(ticket) },
                                    onLongPress = { ticketToDelete = ticket }
                                )
                            }
                            .testTag("staff_ticket_card_${ticket.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (unreadCount > 0) SchoolBlueContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(
                            1.dp,
                            if (unreadCount > 0) SchoolBluePrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (unreadCount > 0) SchoolBluePrimary else SchoolBluePrimary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ticket.userName.take(1).ifBlank { "ک" },
                                        fontWeight = FontWeight.Bold,
                                        color = if (unreadCount > 0) Color.White else SchoolBluePrimary,
                                        fontSize = 18.sp
                                    )
                                }

                                if (unreadCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Red,
                                        border = BorderStroke(1.5.dp, Color.White)
                                    ) {
                                        Text(
                                            text = unreadCount.toString().toPersianDigits(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ticket.userName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = VazirmatnFontFamily
                                    )

                                    if (timeFormatted.isNotBlank()) {
                                        Text(
                                            text = timeFormatted,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${ticket.userRoleTitle} | ${ticket.userPhoneOrCode.toPersianDigits()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = VazirmatnFontFamily
                                )

                                if (ticket.lastMessage.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ticket.lastMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportChatConversationView(
    ticket: SupportTicket,
    currentUser: User?,
    isStaffReplying: Boolean,
    headerTitle: String? = null,
    onBackToList: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<SupportMessage?>(null) }
    var editingMessage by remember { mutableStateOf<SupportMessage?>(null) }

    // Multi-Selection State
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Delete Confirmation Dialog State
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var messageIdsToDelete by remember { mutableStateOf<Set<String>>(emptySet()) }

    val listState = rememberLazyListState()

    LaunchedEffect(ticket.messages.size) {
        if (ticket.messages.isNotEmpty()) {
            listState.animateScrollToItem(ticket.messages.size - 1)
        }
    }

    LaunchedEffect(ticket.id) {
        if (!isStaffReplying) {
            SupportManager.markTicketReadByUser(ticket.id)
        }
    }

    // Confirmation Dialog for Deleting Messages
    if (showDeleteConfirmDialog && messageIdsToDelete.isNotEmpty()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                messageIdsToDelete = emptySet()
            },
            title = {
                Text(
                    text = "حذف پیام‌ها",
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
            },
            text = {
                Text(
                    text = "آیا از حذف ${messageIdsToDelete.size.toString().toPersianDigits()} پیام انتخاب‌شده اطمینان دارید؟",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = VazirmatnFontFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SupportManager.deleteMessages(ticket.id, messageIdsToDelete)
                        Toast.makeText(
                            context,
                            "${messageIdsToDelete.size.toString().toPersianDigits()} پیام حذف شد",
                            Toast.LENGTH_SHORT
                        ).show()
                        showDeleteConfirmDialog = false
                        messageIdsToDelete = emptySet()
                        isMultiSelectMode = false
                        selectedMessageIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold, fontFamily = VazirmatnFontFamily)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        messageIdsToDelete = emptySet()
                    }
                ) {
                    Text("انصراف", fontFamily = VazirmatnFontFamily)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Multi-Select Action Bar OR Header Bar
        if (isMultiSelectMode) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SchoolBluePrimary,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedMessageIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedMessageIds.size.toString().toPersianDigits()} پیام انتخاب شده",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 15.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Multi-Copy Button
                        IconButton(
                            onClick = {
                                val selectedMsgs = ticket.messages.filter { it.id in selectedMessageIds }
                                val combinedText = selectedMsgs.joinToString("\n---\n") { "${it.senderName}: ${it.content}" }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Selected Support Messages", combinedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "${selectedMessageIds.size.toString().toPersianDigits()} پیام کپی شد", Toast.LENGTH_SHORT).show()
                                isMultiSelectMode = false
                                selectedMessageIds = emptySet()
                            },
                            enabled = selectedMessageIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "کپی همه", tint = Color.White)
                        }

                        // Multi-Delete Button
                        IconButton(
                            onClick = {
                                messageIdsToDelete = selectedMessageIds
                                showDeleteConfirmDialog = true
                            },
                            enabled = selectedMessageIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف همه", tint = Color.White)
                        }
                    }
                }
            }
        } else if (headerTitle != null || onBackToList != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onBackToList != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onBackToList() }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = SchoolBluePrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "بازگشت به لیست",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SchoolBluePrimary,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }

                    Text(
                        text = headerTitle ?: "گفتگو با: ${ticket.userName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }
        }

        // Chat Messages Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!ticket.isStarted) {
                // Telegram-Bot Style START Card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_telegram_bot_start_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.35f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SchoolBluePrimary, Color(0xFF0284C7))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HeadsetMic,
                                    contentDescription = "ربات پشتیبانی",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "ربات پشتیبانی نظم یار",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontFamily = VazirmatnFontFamily
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "برای شروع گفتگو و مطرح کردن سوالات، روی دکمه‌ی «شروع» در پایین صفحه کلیک کنید.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }
            } else {
                // Chat Message Bubbles List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ticket.messages, key = { it.id }) { msg ->
                        val isMyMsg = (isStaffReplying && msg.isFromSupport) || (!isStaffReplying && !msg.isFromSupport)
                        val isSelected = msg.id in selectedMessageIds

                        SupportMessageBubble(
                            msg = msg,
                            isCurrentUserMsg = isMyMsg,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) {
                                    val newSet = selectedMessageIds - msg.id
                                    selectedMessageIds = newSet
                                    if (newSet.isEmpty()) isMultiSelectMode = false
                                } else {
                                    selectedMessageIds = selectedMessageIds + msg.id
                                    isMultiSelectMode = true
                                }
                            },
                            onReplyClick = {
                                replyingToMessage = msg
                            },
                            onEditClick = {
                                editingMessage = msg
                                inputText = msg.content
                            },
                            onDeleteClick = {
                                messageIdsToDelete = setOf(msg.id)
                                showDeleteConfirmDialog = true
                            },
                            onEnterMultiSelect = {
                                isMultiSelectMode = true
                                selectedMessageIds = setOf(msg.id)
                            }
                        )
                    }
                }
            }
        }

        // Quoted Reply Banner / Editing Indicator
        AnimatedVisibility(visible = replyingToMessage != null || editingMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(32.dp)
                                .background(SchoolBluePrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val title = if (editingMessage != null) "در حال ویرایش پیام" else "پاسخ به ${replyingToMessage?.senderName ?: "کاربر"}"
                            val subtitle = editingMessage?.content ?: replyingToMessage?.content.orEmpty()
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SchoolBluePrimary,
                                fontFamily = VazirmatnFontFamily
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }

                    IconButton(onClick = {
                        replyingToMessage = null
                        if (editingMessage != null) {
                            editingMessage = null
                            inputText = ""
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "انصراف", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Bottom Bar: Start Button OR Typing Input Bar (Send Button strictly on RIGHT side!)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                if (!ticket.isStarted) {
                    Button(
                        onClick = { SupportManager.startTicketSession(ticket.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("support_start_bot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "شروع",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "شروع",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // SEND BUTTON ON RIGHT SIDE
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val editMsg = editingMessage
                                    if (editMsg != null) {
                                        SupportManager.editMessage(ticket.id, editMsg.id, inputText)
                                        editingMessage = null
                                    } else {
                                        if (isStaffReplying) {
                                            SupportManager.sendMessageFromSupport(ticket.id, currentUser, inputText, replyingToMessage)
                                        } else {
                                            SupportManager.sendMessageFromUser(ticket.id, currentUser, inputText, replyingToMessage)
                                        }
                                        replyingToMessage = null
                                    }
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) SchoolBluePrimary else MaterialTheme.colorScheme.outlineVariant)
                                .testTag("support_send_button"),
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                contentDescription = "ارسال",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // TEXTFIELD ON LEFT
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(if (editingMessage != null) "ویرایش پیام..." else "مورد یا سوال خود را بنویسید...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("support_input_textfield"),
                            shape = RoundedCornerShape(16.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SchoolBluePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SupportMessageBubble(
    msg: SupportMessage,
    isCurrentUserMsg: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onReplyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEnterMultiSelect: () -> Unit
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    val timeFormatted = remember(msg.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(msg.timestamp)).toPersianDigits()
    }

    val bubbleColor = when {
        isSelected -> SchoolBluePrimary.copy(alpha = 0.85f)
        isCurrentUserMsg -> SchoolBluePrimary
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    }

    val textColor = if (isCurrentUserMsg || isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .pointerInput(msg.id, isMultiSelectMode) {
                if (!isMultiSelectMode) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -60f) {
                                onReplyClick()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            if (dragAmount < 0 || offsetX < 0) {
                                offsetX = (offsetX + dragAmount).coerceIn(-100f, 0f)
                            }
                        }
                    )
                }
            },
        contentAlignment = if (isCurrentUserMsg) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCurrentUserMsg) Arrangement.End else Arrangement.Start
        ) {
            // Multi-select Checkbox indicator
            if (isMultiSelectMode) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PositiveGreen else MaterialTheme.colorScheme.outlineVariant)
                        .clickable { onToggleSelect() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "انتخاب شده", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Box {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isCurrentUserMsg) 16.dp else 2.dp,
                        bottomEnd = if (isCurrentUserMsg) 2.dp else 16.dp
                    ),
                    color = bubbleColor,
                    border = if (isSelected) BorderStroke(2.dp, PositiveGreen) else null,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isCurrentUserMsg) 16.dp else 2.dp,
                                bottomEnd = if (isCurrentUserMsg) 2.dp else 16.dp
                            )
                        )
                        .pointerInput(msg.id, isMultiSelectMode) {
                            detectTapGestures(
                                onTap = {
                                    if (isMultiSelectMode) {
                                        onToggleSelect()
                                    }
                                },
                                onLongPress = {
                                    if (isMultiSelectMode) {
                                        onToggleSelect()
                                    } else {
                                        showContextMenu = true
                                    }
                                }
                            )
                        }
                        .testTag("support_message_bubble_${msg.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Sender Name Header
                        val displayName = if (msg.senderName == "ربات پشتیبانی نظم‌یار" || msg.senderName == "bot_nezamyar") "ربات پشتیبانی نظم یار" else msg.senderName
                        Row(
                            modifier = Modifier.padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName.ifBlank { if (msg.isFromSupport) "پشتیبانی" else "کاربر" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentUserMsg || isSelected) Color.White.copy(alpha = 0.9f) else SchoolBluePrimary,
                                fontFamily = VazirmatnFontFamily
                            )
                        }

                        // Quoted Reply Box inside Bubble (if this message is replying to another message)
                        if (msg.replyToContent != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrentUserMsg || isSelected) Color.White.copy(alpha = 0.18f) else SchoolBluePrimary.copy(alpha = 0.12f))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(28.dp)
                                            .background(if (isCurrentUserMsg || isSelected) Color.White else SchoolBluePrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = msg.replyToSenderName ?: "پیام قبلی",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentUserMsg || isSelected) Color.White else SchoolBluePrimary,
                                            fontFamily = VazirmatnFontFamily
                                        )
                                        Text(
                                            text = msg.replyToContent,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isCurrentUserMsg || isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = VazirmatnFontFamily
                                        )
                                    }
                                }
                            }
                        }

                        // Message Text
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                lineHeight = 22.sp,
                                color = textColor
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Time, Pending & Sent Delivery Status Indicator
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (msg.isEdited) {
                                Text(
                                    text = "ویرایش‌شده • ",
                                    fontSize = 9.sp,
                                    color = if (isCurrentUserMsg || isSelected) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                            Text(
                                text = timeFormatted,
                                fontSize = 10.sp,
                                color = if (isCurrentUserMsg || isSelected) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isCurrentUserMsg) {
                                if (msg.isPending) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "در حال ارسال",
                                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "ارسال شده",
                                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Telegram Style Context Menu (Long-press)
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("کپی متن", fontFamily = VazirmatnFontFamily, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "کپی", modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showContextMenu = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Support Message", msg.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "متن پیام کپی شد", Toast.LENGTH_SHORT).show()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("پاسخ (ریپلای)", fontFamily = VazirmatnFontFamily, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Reply, contentDescription = "ریپلای", modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showContextMenu = false
                            onReplyClick()
                        }
                    )

                    if (isCurrentUserMsg) {
                        DropdownMenuItem(
                            text = { Text("ویرایش پیام", fontFamily = VazirmatnFontFamily, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "ویرایش", modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showContextMenu = false
                                onEditClick()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("انتخاب چندتایی", fontFamily = VazirmatnFontFamily, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = "انتخاب چندتایی", modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showContextMenu = false
                            onEnterMultiSelect()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("حذف پیام", fontFamily = VazirmatnFontFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showContextMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}
