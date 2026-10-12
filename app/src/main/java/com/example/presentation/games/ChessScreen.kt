package com.example.presentation.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Dark squares: sleek matte black / charcoal
private val LightSquareColor = Color(0xFFECECEC)
private val DarkSquareColor = Color(0xFF262626)
private val SelectedSquareColor = Color(0xFFD4AF37).copy(alpha = 0.65f)
private val BlockedSquareColor = Color(0xFFEF4444).copy(alpha = 0.55f)
private val LastMoveSquareColor = Color(0xFF90CAF9).copy(alpha = 0.40f)
private val CheckSquareColor = Color(0xFFDC2626).copy(alpha = 0.85f)

@Composable
fun ChessScreen(
    onBackClick: () -> Unit
) {
    val game = remember { ChessGameState() }
    val scope = rememberCoroutineScope()

    var showAboutDialog by remember { mutableStateOf(false) }

    // Game Mode & Bot Settings
    var gameMode by remember { mutableStateOf(ChessGameMode.TWO_PLAYERS_ONE_PHONE) }
    var userColor by remember { mutableStateOf(PieceColor.WHITE) }
    var botDifficulty by remember { mutableStateOf(BotDifficulty.HARD) }
    var isBotThinking by remember { mutableStateOf(false) }

    val botColor = userColor.opposite()

    // Bot move execution trigger
    LaunchedEffect(game.currentTurn, gameMode, game.winner, game.isDraw) {
        if (gameMode == ChessGameMode.VS_BOT &&
            game.currentTurn == botColor &&
            game.winner == null &&
            !game.isDraw
        ) {
            isBotThinking = true
            scope.launch {
                // Realistic thinking delay based on difficulty
                val thinkDelay = when (botDifficulty) {
                    BotDifficulty.EASY -> 450L
                    BotDifficulty.HARD -> 700L
                    BotDifficulty.IMPOSSIBLE -> 900L
                }
                delay(thinkDelay)

                val bestMove = withContext(Dispatchers.Default) {
                    ChessBot.chooseMove(game, botDifficulty, botColor)
                }

                if (bestMove != null) {
                    // Check if pawn promotes
                    val movingPiece = game.board[bestMove.from.row][bestMove.from.col]
                    val isPawnPromotion = movingPiece?.type == PieceType.PAWN &&
                            (bestMove.to.row == 0 || bestMove.to.row == 7)

                    game.executeMove(
                        bestMove,
                        if (isPawnPromotion) PieceType.QUEEN else PieceType.QUEEN
                    )
                }
                isBotThinking = false
            }
        }
    }

    // Reset game helper with bot trigger handling
    fun resetCurrentGame() {
        game.reset()
        isBotThinking = false
    }

    // About & Piece Guide Dialog
    if (showAboutDialog) {
        ChessAboutDialog(onDismiss = { showAboutDialog = false })
    }

    // Promotion Dialog
    val pendingPromo = game.pendingPromotion
    if (pendingPromo != null) {
        val promoColor = game.currentTurn
        AlertDialog(
            onDismissRequest = {
                game.completePromotion(PieceType.QUEEN)
            },
            title = {
                Text(
                    text = "ارتقای پیاده",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val promotionOptions = listOf(
                        PieceType.QUEEN to "وزیر",
                        PieceType.ROOK to "رخ",
                        PieceType.BISHOP to "فیل",
                        PieceType.KNIGHT to "اسب"
                    )
                    for ((pieceType, name) in promotionOptions) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    game.completePromotion(pieceType)
                                }
                                .padding(8.dp)
                        ) {
                            val dummyPiece = ChessPiece(pieceType, promoColor)
                            ChessPieceView(piece = dummyPiece, sizeSp = 36f)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        game.completePromotion(PieceType.QUEEN)
                    }
                ) {
                    Text("وزیر (پیش‌فرض)")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("chess_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت"
                )
            }

            Text(
                text = "بازی شطرنج فکری",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.testTag("chess_about_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "درباره و راهنمای بازی"
                    )
                }

                IconButton(
                    onClick = { resetCurrentGame() },
                    modifier = Modifier.testTag("chess_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "شروع مجدد"
                    )
                }
            }
        }

        // Mode Selector: "دونفره روی یک موبایل" vs "بازی با ربات"
        TabRow(
            selectedTabIndex = if (gameMode == ChessGameMode.TWO_PLAYERS_ONE_PHONE) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = gameMode == ChessGameMode.TWO_PLAYERS_ONE_PHONE,
                onClick = {
                    if (gameMode != ChessGameMode.TWO_PLAYERS_ONE_PHONE) {
                        gameMode = ChessGameMode.TWO_PLAYERS_ONE_PHONE
                        resetCurrentGame()
                    }
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ChessGameMode.TWO_PLAYERS_ONE_PHONE.persianName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (gameMode == ChessGameMode.TWO_PLAYERS_ONE_PHONE) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )

            Tab(
                selected = gameMode == ChessGameMode.VS_BOT,
                onClick = {
                    if (gameMode != ChessGameMode.VS_BOT) {
                        gameMode = ChessGameMode.VS_BOT
                        resetCurrentGame()
                    }
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ChessGameMode.VS_BOT.persianName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (gameMode == ChessGameMode.VS_BOT) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bot Controls (Difficulty + Piece Color Selection)
        AnimatedVisibility(visible = gameMode == ChessGameMode.VS_BOT) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Piece Color Choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "رنگ مهره شما:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = userColor == PieceColor.WHITE,
                                onClick = {
                                    if (userColor != PieceColor.WHITE) {
                                        userColor = PieceColor.WHITE
                                        resetCurrentGame()
                                    }
                                },
                                label = { Text("مهره سفید (شروع‌کننده)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = SchoolBluePrimary
                                )
                            )

                            FilterChip(
                                selected = userColor == PieceColor.BLACK,
                                onClick = {
                                    if (userColor != PieceColor.BLACK) {
                                        userColor = PieceColor.BLACK
                                        resetCurrentGame()
                                    }
                                },
                                label = { Text("مهره سیاه") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1E293B).copy(alpha = 0.15f),
                                    selectedLabelColor = Color(0xFF1E293B)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Difficulty Choice: آسان، سخت، غیر ممکن
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سطح هوش ربات:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BotDifficulty.entries.forEach { diff ->
                                val isSelected = botDifficulty == diff
                                val chipColor = when (diff) {
                                    BotDifficulty.EASY -> PositiveGreen
                                    BotDifficulty.HARD -> Color(0xFFD97706)
                                    BotDifficulty.IMPOSSIBLE -> Color(0xFFDC2626)
                                }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        botDifficulty = diff
                                    },
                                    label = {
                                        Text(
                                            text = diff.persianName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = chipColor.copy(alpha = 0.16f),
                                        selectedLabelColor = chipColor
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = botDifficulty.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (game.winner != null) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val winner = game.winner
                val isDraw = game.isDraw

                when {
                    winner != null -> {
                        val winnerName = if (winner == PieceColor.WHITE) "سفید" else "سیاه"
                        val winnerSub = if (gameMode == ChessGameMode.VS_BOT) {
                            if (winner == userColor) "شما برنده شدید! تبریک 🏆" else "ربات برنده شد 🤖"
                        } else {
                            "برنده: $winnerName 🏆"
                        }

                        Text(
                            text = "⚡ کیش! بازی تمام شد — $winnerSub",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    isDraw -> {
                        Text(
                            text = "پایان بازی — تساوی (پات)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    else -> {
                        val turnName = if (game.currentTurn == PieceColor.WHITE) "سفید" else "سیاه"
                        val turnLabel = if (gameMode == ChessGameMode.VS_BOT) {
                            if (game.currentTurn == userColor) "شما ($turnName)" else "ربات ($turnName)"
                        } else {
                            turnName
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isBotThinking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = SchoolBluePrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ربات در حال فکر کردن...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SchoolBluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(text = "نوبت: ", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = turnLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (game.currentTurn == PieceColor.WHITE) SchoolBluePrimary else Color(0xFF1E293B)
                                )
                            }

                            if (game.isCheck) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFFDC2626),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "کیش!",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { resetCurrentGame() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                ) {
                    Text(text = "شروع مجدد", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Piece Movement / Inspection Feedback Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = when {
                game.selectedPieceHasNoMoves -> Color(0xFFFEF2F2)
                game.selectedPosition != null -> Color(0xFFF0FDF4)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
            border = if (game.selectedPieceHasNoMoves) {
                BorderStroke(1.dp, Color(0xFFFCA5A5))
            } else if (game.selectedPosition != null) {
                BorderStroke(1.dp, Color(0xFF86EFAC))
            } else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when {
                    game.selectedPieceHasNoMoves -> Icons.Default.Block
                    game.selectedPosition != null -> Icons.Default.CheckCircle
                    else -> Icons.Default.Info
                }
                val iconTint = when {
                    game.selectedPieceHasNoMoves -> Color(0xFFDC2626)
                    game.selectedPosition != null -> Color(0xFF16A34A)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                val message = game.selectionStatusText
                    ?: "روی هر مهره بزنید تا مقصدهای مجاز و امکان حرکت آن را ببینید."

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (game.selectedPosition != null) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        game.selectedPieceHasNoMoves -> Color(0xFFDC2626)
                        game.selectedPosition != null -> Color(0xFF15803D)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Captured by White (Black pieces lost)
        CapturedPiecesRow(pieces = game.capturedBlack, label = "مهره‌های خارج شده:")

        Spacer(modifier = Modifier.height(6.dp))

        // Chess Board wrapped in LTR to maintain canonical board coordinates regardless of system locale
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(3.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
            ) {
                val tileSize = maxWidth / 8

                // Determine if a King is currently under check
                val checkedKingColor = if (game.winner != null) game.winner!!.opposite() else if (game.isCheck) game.currentTurn else null
                val checkedKingPos = checkedKingColor?.let { game.getKingPosition(it) }

                Column(modifier = Modifier.fillMaxSize()) {
                    for (row in 0..7) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (col in 0..7) {
                                val pos = Position(row, col)
                                val isLight = (row + col) % 2 == 0
                                val isSelected = game.selectedPosition == pos
                                val isPossibleMove = pos in game.validMoves
                                val isLastMove = game.lastMove?.let { it.first == pos || it.second == pos } == true
                                val isKingInCheck = checkedKingPos == pos
                                val piece = game.board[row][col]

                                val bgColor = when {
                                    isKingInCheck -> CheckSquareColor
                                    isSelected && game.selectedPieceHasNoMoves -> BlockedSquareColor
                                    isSelected -> SelectedSquareColor
                                    isLastMove -> LastMoveSquareColor
                                    isLight -> LightSquareColor
                                    else -> DarkSquareColor
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(bgColor)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true)
                                        ) {
                                            // Prevent user from moving when it's bot's turn
                                            if (gameMode == ChessGameMode.VS_BOT && game.currentTurn == botColor) {
                                                return@clickable
                                            }
                                            game.selectOrMove(row, col)
                                        }
                                        .then(
                                            if (isSelected) {
                                                if (game.selectedPieceHasNoMoves) {
                                                    Modifier.border(3.dp, Color(0xFFEF4444))
                                                } else {
                                                    Modifier.border(3.dp, Color(0xFFEAB308))
                                                }
                                            } else if (isKingInCheck) {
                                                Modifier.border(3.dp, Color(0xFFFF0000))
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .drawBehind {
                                            // Draw valid move indicators on canvas directly so they NEVER intercept clicks!
                                            if (isPossibleMove) {
                                                if (game.selectedPieceIsEnemy) {
                                                    drawCircle(
                                                        color = Color(0x99A855F7),
                                                        radius = size.minDimension * 0.16f
                                                    )
                                                } else if (piece != null) {
                                                    // Capture indicator: outer red ring + soft red tint
                                                    drawCircle(
                                                        color = Color(0xFFEF4444),
                                                        radius = size.minDimension * 0.42f,
                                                        style = Stroke(width = 8f)
                                                    )
                                                    drawCircle(
                                                        color = Color(0x33EF4444),
                                                        radius = size.minDimension * 0.42f
                                                    )
                                                } else {
                                                    // Normal move destination dot
                                                    drawCircle(
                                                        color = if (isLight) Color(0x77000000) else Color(0x99FFFFFF),
                                                        radius = size.minDimension * 0.18f
                                                    )
                                                }
                                            }
                                        }
                                        .testTag("chess_tile_${row}_${col}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Draw piece if exists
                                    if (piece != null) {
                                        ChessPieceView(
                                            piece = piece,
                                            sizeSp = tileSize.value * 0.58f
                                        )
                                    }

                                    // Blocked badge for selected piece that cannot move
                                    if (isSelected && game.selectedPieceHasNoMoves) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(16.dp)
                                                .background(Color(0xFFDC2626), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = "مسدود",
                                                tint = Color.White,
                                                modifier = Modifier.padding(2.dp)
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

        Spacer(modifier = Modifier.height(6.dp))

        // Captured by Black (White pieces lost)
        CapturedPiecesRow(pieces = game.capturedWhite, label = "مهره‌های خارج شده:")

        Spacer(modifier = Modifier.height(10.dp))

        // Actions: "شروع مجدد" alongside "درباره"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { resetCurrentGame() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chess_reset_game_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "شروع مجدد",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { showAboutDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chess_about_button_bottom"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "درباره",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ChessPieceView(
    piece: ChessPiece,
    sizeSp: Float
) {
    Box(contentAlignment = Alignment.Center) {
        if (piece.color == PieceColor.WHITE) {
            // White piece with crisp black outline (هاشیه سیاه)
            Text(
                text = piece.symbol,
                fontSize = sizeSp.sp,
                fontWeight = FontWeight.Black,
                style = LocalTextStyle.current.copy(
                    color = Color(0xFF101010),
                    drawStyle = Stroke(width = 6f, join = StrokeJoin.Round)
                )
            )
            // White inner fill
            Text(
                text = piece.symbol,
                fontSize = sizeSp.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        } else {
            // Black piece: dark fill with subtle crisp outer edge for visibility on dark squares
            Text(
                text = piece.symbol,
                fontSize = sizeSp.sp,
                fontWeight = FontWeight.Black,
                style = LocalTextStyle.current.copy(
                    color = Color(0xFFE0E0E0),
                    drawStyle = Stroke(width = 2.5f, join = StrokeJoin.Round)
                )
            )
            // Dark inner fill
            Text(
                text = piece.symbol,
                fontSize = sizeSp.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF151515)
            )
        }
    }
}

@Composable
private fun CapturedPiecesRow(pieces: List<ChessPiece>, label: String) {
    if (pieces.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (piece in pieces) {
                ChessPieceView(piece = piece, sizeSp = 20f)
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    } else {
        Spacer(modifier = Modifier.height(24.dp))
    }
}
