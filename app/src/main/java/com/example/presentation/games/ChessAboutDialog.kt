package com.example.presentation.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SchoolBluePrimary

data class PieceGuideInfo(
    val type: PieceType,
    val persianName: String,
    val englishName: String,
    val movementDescription: String
)

val chessPiecesGuide = listOf(
    PieceGuideInfo(
        type = PieceType.KING,
        persianName = "شاه",
        englishName = "King",
        movementDescription = "مهم‌ترین مهره بازی؛ در هر نوبت می‌تواند یک خانه به هر جهت (افقی، عمودی یا مورب) حرکت کند."
    ),
    PieceGuideInfo(
        type = PieceType.QUEEN,
        persianName = "وزیر",
        englishName = "Queen",
        movementDescription = "قدرتمندترین مهره بازی؛ به هر تعداد خانه در خطوط مستقیم افقی، عمودی و مورب حرکت می‌کند."
    ),
    PieceGuideInfo(
        type = PieceType.ROOK,
        persianName = "رخ",
        englishName = "Rook",
        movementDescription = "به هر تعداد خانه در خطوط مستقیم افقی و عمودی حرکت می‌کند و در قلعه رفتن نقش دارد."
    ),
    PieceGuideInfo(
        type = PieceType.BISHOP,
        persianName = "فیل",
        englishName = "Bishop",
        movementDescription = "به هر تعداد خانه به صورت مورب و فقط در خانه‌های هم‌رنگ خود حرکت می‌کند."
    ),
    PieceGuideInfo(
        type = PieceType.KNIGHT,
        persianName = "اسب",
        englishName = "Knight",
        movementDescription = "حرکت ویژه به شکل L (دو خانه مستقیم و یک خانه عمود)؛ تنها مهره‌ای که می‌تواند از روی مهره‌های دیگر بپرد."
    ),
    PieceGuideInfo(
        type = PieceType.PAWN,
        persianName = "پیاده",
        englishName = "Pawn",
        movementDescription = "یک خانه به جلو می‌رود (در حرکت نخست تا دو خانه)؛ برای زدن مهره حریف یک خانه مورب به جلو حرکت می‌کند."
    )
)

@Composable
fun ChessAboutDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("chess_about_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SchoolBluePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SchoolBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "درباره بازی و راهنمای مهره‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("chess_about_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Summary Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "توضیح خلاصه بازی شطرنج",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolBluePrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "شطرنج یک بازی فکری و کهن استراتژیک دو نفره است که بر روی صفحه‌ای ۶۴ خانه‌ای (۸×۸) با رنگ‌های سیاه و سفید برگزار می‌شود. هر بازیکن ۱۶ مهره (یک شاه، یک وزیر، دو رخ، دو فیل، دو اسب و هشت پیاده) در اختیار دارد.\n\nهدف اصلی: استقرار مهره‌ها، تصاحب مرکز تخته و کیش کردن شاه حریف است. طبق قوانین بازی، به محض اینکه بازیکنی شاه حریف را کیش کند، بازی در همان لحظه با پیروزی او به پایان می‌رسد.",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pieces Section Header
                    Text(
                        text = "شکل و معرفی مهره‌ها",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // List of Pieces
                    for (pieceInfo in chessPiecesGuide) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Piece Shapes: White and Black
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF262626)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // White piece
                                        ChessPieceView(
                                            piece = ChessPiece(pieceInfo.type, PieceColor.WHITE),
                                            sizeSp = 24f
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        // Black piece
                                        ChessPieceView(
                                            piece = ChessPiece(pieceInfo.type, PieceColor.BLACK),
                                            sizeSp = 24f
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Piece Name and Movement Description
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = pieceInfo.persianName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SchoolBluePrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${pieceInfo.englishName})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pieceInfo.movementDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                ) {
                    Text(
                        text = "متوجه شدم",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
