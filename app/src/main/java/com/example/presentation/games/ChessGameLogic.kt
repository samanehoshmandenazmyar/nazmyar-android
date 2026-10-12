package com.example.presentation.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE

    val persianName: String
        get() = if (this == WHITE) "سفید" else "سیاه"
}

enum class PieceType {
    PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING;

    val persianName: String
        get() = when (this) {
            KING -> "شاه"
            QUEEN -> "وزیر"
            ROOK -> "رخ"
            BISHOP -> "فیل"
            KNIGHT -> "اسب"
            PAWN -> "پیاده"
        }
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
) {
    val symbol: String
        get() = when (type) {
            PieceType.KING -> "♚"
            PieceType.QUEEN -> "♛"
            PieceType.ROOK -> "♜"
            PieceType.BISHOP -> "♝"
            PieceType.KNIGHT -> "♞"
            PieceType.PAWN -> "♟"
        }
}

data class Position(val row: Int, val col: Int)

data class Move(
    val from: Position,
    val to: Position,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    val isPromotion: Boolean = false
)

class ChessGameState {
    // Observable Compose state
    var board by mutableStateOf<Array<Array<ChessPiece?>>>(createInitialBoard())
    var currentTurn by mutableStateOf(PieceColor.WHITE)
    var selectedPosition by mutableStateOf<Position?>(null)
    var validMoves by mutableStateOf<List<Position>>(emptyList())
    var availableLegalMoves: List<Move> = emptyList()

    // Selection Inspection details
    var selectedPieceHasNoMoves by mutableStateOf(false)
    var selectedPieceIsEnemy by mutableStateOf(false)
    var selectionStatusText by mutableStateOf<String?>(null)

    val capturedWhite = mutableStateListOf<ChessPiece>()
    val capturedBlack = mutableStateListOf<ChessPiece>()

    var winner by mutableStateOf<PieceColor?>(null)
    var isDraw by mutableStateOf(false)
    var isCheck by mutableStateOf(false)
    var isCheckGameOver by mutableStateOf(false)
    var lastMove by mutableStateOf<Pair<Position, Position>?>(null)

    // For en-passant tracking
    var enPassantTarget: Position? = null

    // For promotion handling
    var pendingPromotion by mutableStateOf<Move?>(null)

    init {
        updateCheckStatus()
    }

    companion object {
        fun createInitialBoard(): Array<Array<ChessPiece?>> {
            val b = Array(8) { arrayOfNulls<ChessPiece>(8) }
            // Black back rank
            b[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
            b[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
            b[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
            b[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
            b[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
            b[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
            b[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
            b[0][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
            for (c in 0..7) {
                b[1][c] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)
            }

            // White pawns
            for (c in 0..7) {
                b[6][c] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
            }
            // White back rank
            b[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
            b[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
            b[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
            b[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
            b[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
            b[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
            b[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
            b[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
            return b
        }

        fun cloneBoard(source: Array<Array<ChessPiece?>>): Array<Array<ChessPiece?>> {
            return Array(8) { r ->
                Array(8) { c ->
                    source[r][c]
                }
            }
        }
    }

    fun reset() {
        board = createInitialBoard()
        capturedWhite.clear()
        capturedBlack.clear()
        currentTurn = PieceColor.WHITE
        clearSelection()
        winner = null
        isDraw = false
        isCheck = false
        isCheckGameOver = false
        lastMove = null
        enPassantTarget = null
        pendingPromotion = null

        updateCheckStatus()
    }

    fun clearSelection() {
        selectedPosition = null
        availableLegalMoves = emptyList()
        validMoves = emptyList()
        selectedPieceHasNoMoves = false
        selectedPieceIsEnemy = false
        selectionStatusText = null
    }

    /**
     * User taps square (row, col)
     */
    fun selectOrMove(row: Int, col: Int): Boolean {
        if (winner != null || isDraw) return false
        if (pendingPromotion != null) return false

        val target = Position(row, col)
        val currentSelected = selectedPosition
        val movingPiece = currentSelected?.let { board[it.row][it.col] }

        // If user tapped on the currently selected piece, toggle it off!
        if (currentSelected == target) {
            clearSelection()
            return true
        }

        // If we have our own piece selected and click on one of its valid destinations -> Move!
        if (currentSelected != null &&
            movingPiece != null &&
            movingPiece.color == currentTurn &&
            target in validMoves
        ) {
            val move = availableLegalMoves.firstOrNull { it.to == target }
            if (move != null) {
                val isPromotion = move.isPromotion || (movingPiece.type == PieceType.PAWN && (row == 0 || row == 7))
                if (isPromotion) {
                    pendingPromotion = move
                    return true
                } else {
                    executeMove(move)
                    clearSelection()
                    return true
                }
            }
            return false
        } else {
            // User taps any piece to inspect / select
            val piece = board[row][col]
            if (piece != null) {
                selectedPosition = target
                val moves = calculateLegalMoves(target)
                availableLegalMoves = moves
                validMoves = moves.map { it.to }
                selectedPieceIsEnemy = (piece.color != currentTurn)
                selectedPieceHasNoMoves = moves.isEmpty()

                selectionStatusText = if (selectedPieceIsEnemy) {
                    if (selectedPieceHasNoMoves) {
                        "${piece.type.persianName} ${piece.color.persianName} مسدود است و حرکتی ندارد (نوبت حریف)"
                    } else {
                        "${piece.type.persianName} ${piece.color.persianName} — ${moves.size} حرکت ممکن (نوبت حریف)"
                    }
                } else {
                    if (selectedPieceHasNoMoves) {
                        "🚫 این مهره مسدود است و هیچ حرکتی ندارد!"
                    } else {
                        "${piece.type.persianName} ${piece.color.persianName} — ${moves.size} حرکت ممکن"
                    }
                }
                return true
            } else {
                clearSelection()
                return false
            }
        }
    }

    fun completePromotion(promotedType: PieceType) {
        val move = pendingPromotion ?: return
        executeMove(move, promotedType)
        pendingPromotion = null
        clearSelection()
    }

    fun executeMove(move: Move, promotionPieceType: PieceType = PieceType.QUEEN) {
        val (from, to) = move.from to move.to
        val piece = board[from.row][from.col] ?: return

        val newBoard = cloneBoard(board)
        var nextEnPassant: Position? = null

        // Handle En Passant capture
        if (move.isEnPassant) {
            val capturedPawnRow = from.row
            val capturedPawnCol = to.col
            val captured = newBoard[capturedPawnRow][capturedPawnCol]
            if (captured != null) {
                if (captured.color == PieceColor.WHITE) capturedWhite.add(captured)
                else capturedBlack.add(captured)
                newBoard[capturedPawnRow][capturedPawnCol] = null
            }
        } else {
            // Regular capture
            val captured = newBoard[to.row][to.col]
            if (captured != null) {
                if (captured.color == PieceColor.WHITE) capturedWhite.add(captured)
                else capturedBlack.add(captured)
            }
        }

        // Handle Castling rook movement
        if (move.isCastling) {
            val row = from.row
            if (to.col == 6) {
                val rook = newBoard[row][7]
                newBoard[row][5] = rook?.copy(hasMoved = true)
                newBoard[row][7] = null
            } else if (to.col == 2) {
                val rook = newBoard[row][0]
                newBoard[row][3] = rook?.copy(hasMoved = true)
                newBoard[row][0] = null
            }
        }

        // Check if pawn double moved (enables en passant next turn)
        if (piece.type == PieceType.PAWN && Math.abs(from.row - to.row) == 2) {
            val skippedRow = (from.row + to.row) / 2
            nextEnPassant = Position(skippedRow, from.col)
        }

        // Place moved piece on target square
        if (move.isPromotion || (piece.type == PieceType.PAWN && (to.row == 0 || to.row == 7))) {
            newBoard[to.row][to.col] = ChessPiece(promotionPieceType, piece.color, hasMoved = true)
        } else {
            newBoard[to.row][to.col] = piece.copy(hasMoved = true)
        }
        newBoard[from.row][from.col] = null

        board = newBoard
        enPassantTarget = nextEnPassant
        lastMove = Pair(from, to)

        // Switch turn
        currentTurn = currentTurn.opposite()

        // Check & End Game State
        updateCheckStatus()

        // User explicit rule: "و وفتی کیش کردیم هم تمام"
        // When check is delivered, the checking player wins immediately!
        if (isCheck) {
            winner = currentTurn.opposite()
            isCheckGameOver = true
        } else {
            checkEndGameConditions()
        }
    }

    fun getKingPosition(color: PieceColor): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    return Position(r, c)
                }
            }
        }
        return null
    }

    private fun updateCheckStatus() {
        val kingPos = getKingPosition(currentTurn)
        isCheck = if (kingPos != null) {
            isSquareAttacked(kingPos, currentTurn.opposite())
        } else {
            false
        }
    }

    private fun checkEndGameConditions() {
        var hasAnyLegalMove = false
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == currentTurn) {
                    val moves = calculateLegalMoves(Position(r, c))
                    if (moves.isNotEmpty()) {
                        hasAnyLegalMove = true
                        break
                    }
                }
            }
            if (hasAnyLegalMove) break
        }

        if (!hasAnyLegalMove) {
            if (isCheck) {
                // Checkmate!
                winner = currentTurn.opposite()
            } else {
                // Stalemate!
                isDraw = true
            }
        }
    }

    /**
     * Calculates strictly legal moves for any piece on the board.
     */
    fun calculateLegalMoves(pos: Position): List<Move> {
        val piece = board[pos.row][pos.col] ?: return emptyList()

        val pseudoMoves = generatePseudoMoves(pos, piece)
        val legalMoves = mutableListOf<Move>()

        for (m in pseudoMoves) {
            if (isMoveLegal(m, piece.color)) {
                legalMoves.add(m)
            }
        }

        // Add Castling moves if King
        val kingInCheck = isKingInCheck(piece.color)
        if (piece.type == PieceType.KING && !piece.hasMoved && !kingInCheck) {
            val row = pos.row
            val enemyColor = piece.color.opposite()

            // King-side (O-O)
            val kingSideRook = board[row][7]
            if (kingSideRook != null && kingSideRook.type == PieceType.ROOK &&
                kingSideRook.color == piece.color && !kingSideRook.hasMoved
            ) {
                if (board[row][5] == null && board[row][6] == null) {
                    if (!isSquareAttacked(Position(row, 5), enemyColor) &&
                        !isSquareAttacked(Position(row, 6), enemyColor)
                    ) {
                        legalMoves.add(Move(pos, Position(row, 6), isCastling = true))
                    }
                }
            }

            // Queen-side (O-O-O)
            val queenSideRook = board[row][0]
            if (queenSideRook != null && queenSideRook.type == PieceType.ROOK &&
                queenSideRook.color == piece.color && !queenSideRook.hasMoved
            ) {
                if (board[row][1] == null && board[row][2] == null && board[row][3] == null) {
                    if (!isSquareAttacked(Position(row, 3), enemyColor) &&
                        !isSquareAttacked(Position(row, 2), enemyColor)
                    ) {
                        legalMoves.add(Move(pos, Position(row, 2), isCastling = true))
                    }
                }
            }
        }

        return legalMoves
    }

    fun isKingInCheck(color: PieceColor): Boolean {
        val kingPos = getKingPosition(color) ?: return false
        return isSquareAttacked(kingPos, color.opposite())
    }

    private fun isMoveLegal(move: Move, myColor: PieceColor): Boolean {
        val (from, to) = move.from to move.to
        val movingPiece = board[from.row][from.col] ?: return false
        val destPiece = board[to.row][to.col]

        // Temporarily apply move
        board[to.row][to.col] = movingPiece
        board[from.row][from.col] = null

        var capturedEnPassantPiece: ChessPiece? = null
        if (move.isEnPassant) {
            capturedEnPassantPiece = board[from.row][to.col]
            board[from.row][to.col] = null
        }

        // Find king position
        val kingPos = if (movingPiece.type == PieceType.KING) {
            to
        } else {
            getKingPosition(myColor)
        }

        val inCheck = if (kingPos != null) {
            isSquareAttacked(kingPos, myColor.opposite())
        } else {
            true
        }

        // Revert move
        board[from.row][from.col] = movingPiece
        board[to.row][to.col] = destPiece
        if (move.isEnPassant) {
            board[from.row][to.col] = capturedEnPassantPiece
        }

        return !inCheck
    }

    fun isSquareAttacked(target: Position, attackerColor: PieceColor): Boolean {
        val (tr, tc) = target

        // 1. Attacked by Pawns
        val pawnDir = if (attackerColor == PieceColor.WHITE) -1 else 1
        val enemyPawnRow = tr - pawnDir
        if (enemyPawnRow in 0..7) {
            for (dc in listOf(-1, 1)) {
                val enemyPawnCol = tc + dc
                if (enemyPawnCol in 0..7) {
                    val p = board[enemyPawnRow][enemyPawnCol]
                    if (p != null && p.color == attackerColor && p.type == PieceType.PAWN) {
                        return true
                    }
                }
            }
        }

        // 2. Attacked by Knights
        val knightDeltas = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((dr, dc) in knightDeltas) {
            val nr = tr + dr
            val nc = tc + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p != null && p.color == attackerColor && p.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // 3. Attacked by King
        val kingDeltas = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1, 0 to 1,
            1 to -1, 1 to 0, 1 to 1
        )
        for ((dr, dc) in kingDeltas) {
            val nr = tr + dr
            val nc = tc + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p != null && p.color == attackerColor && p.type == PieceType.KING) {
                    return true
                }
            }
        }

        // 4. Attacked along Straight lines (Rook / Queen)
        val straightDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in straightDirs) {
            var currR = tr + dr
            var currC = tc + dc
            while (currR in 0..7 && currC in 0..7) {
                val p = board[currR][currC]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                currR += dr
                currC += dc
            }
        }

        // 5. Attacked along Diagonals (Bishop / Queen)
        val diagDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in diagDirs) {
            var currR = tr + dr
            var currC = tc + dc
            while (currR in 0..7 && currC in 0..7) {
                val p = board[currR][currC]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                currR += dr
                currC += dc
            }
        }

        return false
    }

    fun isSquareAttackedOnBoard(
        targetBoard: Array<Array<ChessPiece?>>,
        target: Position,
        attackerColor: PieceColor
    ): Boolean {
        val (tr, tc) = target

        // 1. Pawns
        val pawnDir = if (attackerColor == PieceColor.WHITE) -1 else 1
        val enemyPawnRow = tr - pawnDir
        if (enemyPawnRow in 0..7) {
            for (dc in listOf(-1, 1)) {
                val enemyPawnCol = tc + dc
                if (enemyPawnCol in 0..7) {
                    val p = targetBoard[enemyPawnRow][enemyPawnCol]
                    if (p != null && p.color == attackerColor && p.type == PieceType.PAWN) {
                        return true
                    }
                }
            }
        }

        // 2. Knights
        val knightDeltas = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        for ((dr, dc) in knightDeltas) {
            val nr = tr + dr
            val nc = tc + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = targetBoard[nr][nc]
                if (p != null && p.color == attackerColor && p.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // 3. King
        val kingDeltas = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
        for ((dr, dc) in kingDeltas) {
            val nr = tr + dr
            val nc = tc + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = targetBoard[nr][nc]
                if (p != null && p.color == attackerColor && p.type == PieceType.KING) {
                    return true
                }
            }
        }

        // 4. Rook / Queen
        val straightDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in straightDirs) {
            var currR = tr + dr
            var currC = tc + dc
            while (currR in 0..7 && currC in 0..7) {
                val p = targetBoard[currR][currC]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                currR += dr
                currC += dc
            }
        }

        // 5. Bishop / Queen
        val diagDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in diagDirs) {
            var currR = tr + dr
            var currC = tc + dc
            while (currR in 0..7 && currC in 0..7) {
                val p = targetBoard[currR][currC]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                currR += dr
                currC += dc
            }
        }

        return false
    }

    fun getAllLegalMovesFor(color: PieceColor): List<Move> {
        val result = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == color) {
                    result.addAll(calculateLegalMoves(Position(r, c)))
                }
            }
        }
        return result
    }

    private fun generatePseudoMoves(pos: Position, piece: ChessPiece): List<Move> {
        val moves = mutableListOf<Move>()
        val (r, c) = pos

        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) -1 else 1
                val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
                val promoRow = if (piece.color == PieceColor.WHITE) 0 else 7

                // 1 step forward
                val nextR = r + dir
                if (nextR in 0..7 && board[nextR][c] == null) {
                    val isPromo = nextR == promoRow
                    moves.add(Move(pos, Position(nextR, c), isPromotion = isPromo))

                    // 2 steps forward
                    val twoNextR = r + 2 * dir
                    if (r == startRow && board[twoNextR][c] == null) {
                        moves.add(Move(pos, Position(twoNextR, c)))
                    }
                }

                // Diagonal captures
                for (dCol in listOf(-1, 1)) {
                    val capC = c + dCol
                    if (nextR in 0..7 && capC in 0..7) {
                        val enemy = board[nextR][capC]
                        val isPromo = nextR == promoRow
                        if (enemy != null && enemy.color != piece.color) {
                            moves.add(Move(pos, Position(nextR, capC), isPromotion = isPromo))
                        } else if (enPassantTarget != null && enPassantTarget == Position(nextR, capC)) {
                            // En Passant
                            moves.add(Move(pos, Position(nextR, capC), isEnPassant = true))
                        }
                    }
                }
            }

            PieceType.ROOK -> {
                addRayMoves(moves, pos, piece.color, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
            }

            PieceType.BISHOP -> {
                addRayMoves(moves, pos, piece.color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
            }

            PieceType.QUEEN -> {
                addRayMoves(
                    moves,
                    pos,
                    piece.color,
                    listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1)
                )
            }

            PieceType.KNIGHT -> {
                val jumps = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((dr, dc) in jumps) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0..7 && nc in 0..7) {
                        val target = board[nr][nc]
                        if (target == null || target.color != piece.color) {
                            moves.add(Move(pos, Position(nr, nc)))
                        }
                    }
                }
            }

            PieceType.KING -> {
                val deltas = listOf(
                    -1 to -1, -1 to 0, -1 to 1,
                    0 to -1, 0 to 1,
                    1 to -1, 1 to 0, 1 to 1
                )
                for ((dr, dc) in deltas) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0..7 && nc in 0..7) {
                        val target = board[nr][nc]
                        if (target == null || target.color != piece.color) {
                            moves.add(Move(pos, Position(nr, nc)))
                        }
                    }
                }
            }
        }

        return moves
    }

    private fun addRayMoves(
        moves: MutableList<Move>,
        from: Position,
        myColor: PieceColor,
        directions: List<Pair<Int, Int>>
    ) {
        for ((dr, dc) in directions) {
            var currR = from.row + dr
            var currC = from.col + dc
            while (currR in 0..7 && currC in 0..7) {
                val piece = board[currR][currC]
                if (piece == null) {
                    moves.add(Move(from, Position(currR, currC)))
                } else {
                    if (piece.color != myColor) {
                        moves.add(Move(from, Position(currR, currC)))
                    }
                    break
                }
                currR += dr
                currC += dc
            }
        }
    }
}
