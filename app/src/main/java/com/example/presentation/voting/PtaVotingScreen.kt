package com.example.presentation.voting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.voting.PtaVotingManager
import com.example.domain.model.PtaCandidate
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PtaVotingScreen(
    currentUser: User?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val userId = currentUser?.id ?: "guest_user"

    LaunchedEffect(userId) {
        PtaVotingManager.init(context, userId)
    }

    val votesMap by PtaVotingManager.votesMap.collectAsState()
    val userVotedCandidateId by PtaVotingManager.currentUserVoteId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: همه, 1: بیشترین آراء, 2: رأی من
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val totalVotes = votesMap.values.sum()

    val filteredCandidates = remember(votesMap, searchQuery, selectedFilterIndex, userVotedCandidateId) {
        var list = PtaVotingManager.initialCandidates

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.childRelation.contains(searchQuery, ignoreCase = true) ||
                    it.education.contains(searchQuery, ignoreCase = true) ||
                    it.bio.contains(searchQuery, ignoreCase = true)
            }
        }

        when (selectedFilterIndex) {
            1 -> list.sortedByDescending { votesMap[it.id] ?: 0 }
            2 -> list.filter { it.id == userVotedCandidateId }
            else -> list
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "انتخابات انجمن اولیا و مربیان",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "صندوق رأی آنلاین و انتخاب اعضای جدید",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("pta_voting_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    if (currentUser?.isCreator == true) {
                        IconButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier.testTag("reset_pta_votes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "صفر کردن تمام آراء",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("pta_voting_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Stats Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SchoolBluePrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HowToVote,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "صندوق رأی‌گیری الکترونیکی",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "انتخاب ۵ عضو اصلی انجمن اولیا و مربیان",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text(
                                    text = "موقت",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "مجموع کل آراء ثبت‌شده",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${totalVotes.toPersianDigits()} رأی",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            val userVotedCandidate = PtaVotingManager.initialCandidates.firstOrNull { it.id == userVotedCandidateId }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (userVotedCandidate != null) PositiveGreen.copy(alpha = 0.2f) else Color(0xFF334155).copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (userVotedCandidate != null) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (userVotedCandidate != null) PositiveGreen else Color(0xFFCBD5E1),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (userVotedCandidate != null) {
                                            "رأی شما: ${userVotedCandidate.name.replace("سرکار خانم ", "")}"
                                        } else {
                                            "شما هنوز رأی نداده‌اید"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (userVotedCandidate != null) PositiveGreen else Color(0xFFE2E8F0),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حساب کاربری رأی‌دهنده: ${currentUser?.name ?: "کاربر سامانه"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "جستجوی نامزد بر اساس نام، پایه فرزند یا تخصص...",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SchoolBluePrimary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pta_search_input")
                )
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterIndex == 0,
                        onClick = { selectedFilterIndex = 0 },
                        label = { Text("همه نامزدها (${PtaVotingManager.initialCandidates.size.toPersianDigits()})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                            selectedLabelColor = SchoolBluePrimary
                        ),
                        modifier = Modifier.testTag("filter_all_candidates")
                    )

                    FilterChip(
                        selected = selectedFilterIndex == 1,
                        onClick = { selectedFilterIndex = 1 },
                        label = { Text("بیشترین آراء") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                            selectedLabelColor = SchoolBluePrimary
                        ),
                        modifier = Modifier.testTag("filter_top_votes")
                    )

                    FilterChip(
                        selected = selectedFilterIndex == 2,
                        onClick = { selectedFilterIndex = 2 },
                        label = { Text("رأی من") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PositiveGreen.copy(alpha = 0.15f),
                            selectedLabelColor = PositiveGreen
                        ),
                        modifier = Modifier.testTag("filter_my_vote")
                    )
                }
            }

            if (filteredCandidates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "نامزدی با این مشخصات یافت نشد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredCandidates, key = { it.id }) { candidate ->
                    val votes = votesMap[candidate.id] ?: 0
                    val percentage = if (totalVotes > 0) (votes.toFloat() / totalVotes.toFloat()) * 100f else 0f
                    val isMyVote = userVotedCandidateId == candidate.id

                    CandidateVoteCard(
                        candidate = candidate,
                        votes = votes,
                        percentage = percentage,
                        isMyVote = isMyVote,
                        onVoteClick = {
                            if (currentUser == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("برای ثبت رأی، ابتدا باید وارد حساب کاربری خود شوید.")
                                }
                                return@CandidateVoteCard
                            }
                            val voted = PtaVotingManager.castVote(userId, candidate.id)
                            if (voted) {
                                scope.launch {
                                    if (PtaVotingManager.currentUserVoteId.value == candidate.id) {
                                        snackbarHostState.showSnackbar(
                                            message = "رأی شما با موفقیت برای «${candidate.name}» ثبت شد."
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            message = "رأی شما لغو گردید."
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = {
                    Text(
                        text = "صفر کردن تمام آراء",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("آیا مطمئن هستید که می‌خواهید تمام آراء صندوق به صفر بازنشانی شود؟")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            PtaVotingManager.resetAllVotesToZero()
                            showResetConfirmDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("تمام آراء نامزدها با موفقیت به صفر بازنشانی شدند.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("بله، صفر کن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}

@Composable
fun CandidateVoteCard(
    candidate: PtaCandidate,
    votes: Int,
    percentage: Float,
    isMyVote: Boolean,
    onVoteClick: () -> Unit
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage / 100f,
        label = "percentageAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("candidate_card_${candidate.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMyVote) PositiveGreenContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isMyVote) 2.dp else 1.dp,
            color = if (isMyVote) PositiveGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMyVote) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Photo, Name, Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // High Quality Veiled Iranian Mother Portrait
                Image(
                    painter = painterResource(id = candidate.imageRes),
                    contentDescription = candidate.name,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = candidate.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isMyVote) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PositiveGreen
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "رأی شما",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = candidate.childRelation,
                        style = MaterialTheme.typography.bodySmall,
                        color = SchoolBluePrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = candidate.education,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = candidate.bio,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Percentage and Vote Count Row (روی هر شیپ خانم‌ها)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMyVote) PositiveGreen else SchoolBluePrimary
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", percentage).toPersianDigits()}٪",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${votes.toPersianDigits()} رأی کسب‌شده",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Interactive Vote Button
                if (isMyVote) {
                    OutlinedButton(
                        onClick = onVoteClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PositiveGreen
                        ),
                        border = BorderStroke(1.5.dp, PositiveGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("unvote_button_${candidate.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PositiveGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ثبت شده (لغو)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onVoteClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SchoolBluePrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("vote_button_${candidate.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "رأی دادن",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Percentage Progress Bar
            LinearProgressIndicator(
                progress = { animatedPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isMyVote) PositiveGreen else SchoolBluePrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
