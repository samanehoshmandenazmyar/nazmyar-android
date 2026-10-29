package com.example.presentation.messenger

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.messenger.DecentralizedMessengerManager
import com.example.domain.model.*
import com.example.presentation.viewmodel.MainViewModel
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
    val group = remember(classGroupId) { DecentralizedMessengerManager.getGroupById(classGroupId) }

    val allP2pMessagesMap by DecentralizedMessengerManager.p2pMessages.collectAsState()
    val messages = allP2pMessagesMap[classGroupId] ?: emptyList()
    val discoveredPeers by DecentralizedMessengerManager.discoveredPeers.collectAsState()

    val isPeerOnline = remember(discoveredPeers, classGroupId) {
        discoveredPeers.any { classGroupId.contains(it.peerId) }
    }

    var inputText by remember { mutableStateOf("") }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D9488)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = group.title.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "E2EE",
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = if (isPeerOnline) "متصل در شبکه محلی Wi-Fi 🟢" else "آفلاین - ذخیره محلی و صف ارسال 🕒",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    color = if (isPeerOnline) Color(0xFF0D9488) else Color(0xFFD97706),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
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
                                    .background(Color(0xFF0D9488).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "گفتگوی مستقیم E2EE و P2P",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "پیام‌های این گفتگو با کلید اختصاصی دستگاه رمزنگاری و امضا می‌شوند و بدون عبور از سرور مرکزی به گیرنده می‌رسند.",
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val isMe = currentUser != null && (
                            message.senderId == currentUser.id || message.senderName == currentUser.name
                        )

                        P2pMessageBubble(message = message, isMe = isMe)
                    }
                }
            }

            // Input bar
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_bottom_input_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "پیام P2P خود را بنویسید...",
                                fontFamily = VazirmatnFontFamily,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF1F5F9),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF0D9488)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && currentUser != null) {
                                DecentralizedMessengerManager.sendMessageP2p(
                                    conversationId = classGroupId,
                                    currentUser = currentUser,
                                    text = inputText
                                )
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) Color(0xFF0D9488) else Color(0xFFCBD5E1))
                            .testTag("chat_send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ارسال پیام",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun P2pMessageBubble(message: ChatMessage, isMe: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) Color(0xFFEFFDDE) else Color(0xFFFFFFFF)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(min = 100.dp, max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                if (!isMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D9488)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timeFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    when (message.status) {
                        MessageStatus.SENDING -> {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "در انتظار ارسال",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        MessageStatus.DELIVERED, MessageStatus.READ -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "تحویل داده شد",
                                tint = Color(0xFF2AABEE),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "ارسال شد",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
