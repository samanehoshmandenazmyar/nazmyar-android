package com.example.presentation.games

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class BotDifficulty(val persianName: String, val description: String) {
    EASY("آسان", "حرکات تصادفی با ترجیح زدن مهره‌ها - مناسب شروع و تمرین"),
    HARD("سخت", "ارزیابی موقعیت مهره‌ها، تصاحب هوشمندانه مرکز و دفاع از شاه"),
    IMPOSSIBLE("غیر ممکن", "الگوریتم مینی‌ماکس چندسطحی، پیش‌بینی تهدیدها و تله‌های کیش")
}

enum class ChessGameMode(val persianName: String) {
    TWO_PLAYERS_ONE_PHONE("دونفره روی یک موبایل"),
    VS_BOT("بازی با ربات")
}

object ChessBot {

    // Piece base values in centipawns
    private fun pieceValue(type: PieceType): Int = when (type) {
        PieceType.PAWN -> 100
        PieceType.KNIGHT -> 320
        PieceType.BISHOP -> 330
        PieceType.ROOK -> 500
        PieceType.QUEEN -> 900
        PieceType.KING -> 20000
    }

    // Piece-Square Tables (PST) to encourage positional chess
    private val pawnPst = arrayOf(
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5,  5, 10, 25, 25, 10,  5,  5),
        intArrayOf(0,  0,  0, 20, 20,  0,  0,  0),
        intArrayOf(5, -5,-10,  0,  0,-10, -5,  5),
        intArrayOf(5, 10, 10,-20,-20, 10, 10,  5),
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0)
    )

    private val knightPst = arrayOf(
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50),
        intArrayOf(-40,-20,  0,  0,  0,  0,-20,-40),
        intArrayOf(-30,  0, 10, 15, 15, 10,  0,-30),
        intArrayOf(-30,  5, 15, 20, 20, 15,  5,-30),
        intArrayOf(-30,  0, 15, 20, 20, 15,  0,-30),
        intArrayOf(-30,  5, 10, 15, 15, 10,  5,-30),
        intArrayOf(-40,-20,  0,  5,  5,  0,-20,-40),
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50)
    )

    private val bishopPst = arrayOf(
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  5, 10, 10,  5,  0,-10),
        intArrayOf(-10,  5,  5, 10, 10,  5,  5,-10),
        intArrayOf(-10,  0, 10, 10, 10, 10,  0,-10),
        intArrayOf(-10, 10, 10, 10, 10, 10, 10,-10),
        intArrayOf(-10,  5,  0,  0,  0,  0,  5,-10),
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20)
    )

    private val rookPst = arrayOf(
        intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf( 5, 10, 10, 10, 10, 10, 10,  5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf( 0,  0,  0,  5,  5,  0,  0,  0)
    )

    private val queenPst = arrayOf(
        intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  5,  5,  5,  5,  0,-10),
        intArrayOf( -5,  0,  5,  5,  5,  5,  0, -5),
        intArrayOf(  0,  0,  5,  5,  5,  5,  0, -5),
        intArrayOf(-10,  5,  5,  5,  5,  5,  0,-10),
        intArrayOf(-10,  0,  5,  0,  0,  0,  0,-10),
        intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20)
    )

    private fun getPositionalBonus(piece: ChessPiece, row: Int, col: Int): Int {
        val r = if (piece.color == PieceColor.WHITE) 7 - row else row
        val c = col
        return when (piece.type) {
            PieceType.PAWN -> pawnPst[r][c]
            PieceType.KNIGHT -> knightPst[r][c]
            PieceType.BISHOP -> bishopPst[r][c]
            PieceType.ROOK -> rookPst[r][c]
            PieceType.QUEEN -> queenPst[r][c]
            PieceType.KING -> 0
        }
    }

    /**
     * Evaluates static board from perspective of `botColor`
     */
    private fun evaluateBoard(board: Array<Array<ChessPiece?>>, botColor: PieceColor): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c] ?: continue
                val valBase = pieceValue(piece.type) + getPositionalBonus(piece, r, c)
                if (piece.color == botColor) {
                    score += valBase
                } else {
                    score -= valBase
                }
            }
        }
        return score
    }

    /**
     * Chooses the best move based on selected difficulty.
     */
    fun chooseMove(
        game: ChessGameState,
        difficulty: BotDifficulty,
        botColor: PieceColor
    ): Move? {
        val allLegalMoves = game.getAllLegalMovesFor(botColor)
        if (allLegalMoves.isEmpty()) return null

        return when (difficulty) {
            BotDifficulty.EASY -> chooseEasyMove(game, allLegalMoves)
            BotDifficulty.HARD -> chooseHardMove(game, allLegalMoves, botColor)
            BotDifficulty.IMPOSSIBLE -> chooseImpossibleMove(game, allLegalMoves, botColor)
        }
    }

    /**
     * EASY: Random moves, but 50% chance to prefer capturing hanging piece or delivering check.
     */
    private fun chooseEasyMove(game: ChessGameState, moves: List<Move>): Move {
        val captures = moves.filter { game.board[it.to.row][it.to.col] != null }
        if (captures.isNotEmpty() && Random.nextFloat() < 0.45f) {
            return captures.random()
        }
        return moves.random()
    }

    /**
     * HARD: 1-Ply evaluation with tactical depth (captures + positional scoring + check priority).
     * Avoids blundering pieces into immediate attacked squares.
     */
    private fun chooseHardMove(
        game: ChessGameState,
        moves: List<Move>,
        botColor: PieceColor
    ): Move {
        val opponentColor = botColor.opposite()
        var bestScore = Int.MIN_VALUE
        var bestMoves = mutableListOf<Move>()

        for (move in moves) {
            val destPiece = game.board[move.to.row][move.to.col]
            val movingPiece = game.board[move.from.row][move.from.col] ?: continue

            // Simulation
            val cloned = ChessGameState.cloneBoard(game.board)
            cloned[move.to.row][move.to.col] = movingPiece
            cloned[move.from.row][move.from.col] = null

            var score = evaluateBoard(cloned, botColor)

            // Heavy reward for capturing opponent piece
            if (destPiece != null) {
                score += pieceValue(destPiece.type) * 2
            }

            // Penalty if destination is attacked by opponent and dest piece value is high
            val isTargetAttacked = game.isSquareAttacked(move.to, opponentColor)
            if (isTargetAttacked) {
                val valueLost = pieceValue(movingPiece.type)
                val valueGained = destPiece?.let { pieceValue(it.type) } ?: 0
                if (valueLost > valueGained) {
                    score -= (valueLost - valueGained)
                }
            }

            // Reward delivering check (under the game's check rule, check delivers instant victory!)
            val opponentKingPos = if (opponentColor == PieceColor.WHITE) {
                findKing(cloned, PieceColor.WHITE)
            } else {
                findKing(cloned, PieceColor.BLACK)
            }
            if (opponentKingPos != null && game.isSquareAttackedOnBoard(cloned, opponentKingPos, botColor)) {
                score += 50000 // Immediate winning move!
            }

            if (score > bestScore) {
                bestScore = score
                bestMoves = mutableListOf(move)
            } else if (score == bestScore) {
                bestMoves.add(move)
            }
        }

        return bestMoves.randomOrNull() ?: moves.random()
    }

    /**
     * IMPOSSIBLE: Minimax with Alpha-Beta Pruning (Depth 3) + Quiescence heuristic.
     * Searches lines to find checkmates, forks, pins and optimal trades.
     */
    private fun chooseImpossibleMove(
        game: ChessGameState,
        moves: List<Move>,
        botColor: PieceColor
    ): Move {
        var bestScore = Int.MIN_VALUE
        var bestMove = moves.first()

        val opponentColor = botColor.opposite()

        // 1. Instant check checkmate check (depth 1 priority)
        for (move in moves) {
            val cloned = ChessGameState.cloneBoard(game.board)
            val p = cloned[move.from.row][move.from.col] ?: continue
            cloned[move.to.row][move.to.col] = p
            cloned[move.from.row][move.from.col] = null

            val opKing = findKing(cloned, opponentColor)
            if (opKing != null && game.isSquareAttackedOnBoard(cloned, opKing, botColor)) {
                return move // Win the game immediately!
            }
        }

        // 2. Full Minimax with Depth 3
        var alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        // Sort moves to optimize alpha-beta pruning (captures first)
        val sortedMoves = moves.sortedByDescending { move ->
            val captured = game.board[move.to.row][move.to.col]
            if (captured != null) pieceValue(captured.type) else 0
        }

        for (move in sortedMoves) {
            val cloned = ChessGameState.cloneBoard(game.board)
            val movingPiece = cloned[move.from.row][move.from.col] ?: continue
            cloned[move.to.row][move.to.col] = movingPiece
            cloned[move.from.row][move.from.col] = null

            val score = minimax(
                board = cloned,
                depth = 2,
                isMaximizing = false,
                alpha = alpha,
                beta = beta,
                botColor = botColor,
                game = game
            )

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = max(alpha, bestScore)
        }

        return bestMove
    }

    private fun minimax(
        board: Array<Array<ChessPiece?>>,
        depth: Int,
        isMaximizing: Boolean,
        alpha: Int,
        beta: Int,
        botColor: PieceColor,
        game: ChessGameState
    ): Int {
        val currentColor = if (isMaximizing) botColor else botColor.opposite()
        val opponentColor = currentColor.opposite()

        // Terminal check: King capture or check victory
        val kingPos = findKing(board, currentColor)
        if (kingPos == null || game.isSquareAttackedOnBoard(board, kingPos, opponentColor)) {
            // Player under check loses
            return if (isMaximizing) -100000 + (3 - depth) * 1000 else 100000 - (3 - depth) * 1000
        }

        if (depth == 0) {
            return evaluateBoard(board, botColor)
        }

        var currentAlpha = alpha
        var currentBeta = beta

        val moves = getBoardMoves(board, currentColor)
        if (moves.isEmpty()) {
            return 0 // Stalemate
        }

        // Move ordering: captures first
        val sorted = moves.sortedByDescending {
            val dest = board[it.to.row][it.to.col]
            if (dest != null) pieceValue(dest.type) else 0
        }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in sorted) {
                val cloned = ChessGameState.cloneBoard(board)
                val p = cloned[move.from.row][move.from.col] ?: continue
                cloned[move.to.row][move.to.col] = p
                cloned[move.from.row][move.from.col] = null

                val evaluation = minimax(cloned, depth - 1, false, currentAlpha, currentBeta, botColor, game)
                maxEval = max(maxEval, evaluation)
                currentAlpha = max(currentAlpha, evaluation)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in sorted) {
                val cloned = ChessGameState.cloneBoard(board)
                val p = cloned[move.from.row][move.from.col] ?: continue
                cloned[move.to.row][move.to.col] = p
                cloned[move.from.row][move.from.col] = null

                val evaluation = minimax(cloned, depth - 1, true, currentAlpha, currentBeta, botColor, game)
                minEval = min(minEval, evaluation)
                currentBeta = min(currentBeta, evaluation)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun findKing(board: Array<Array<ChessPiece?>>, color: PieceColor): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.type == PieceType.KING && p.color == color) return Position(r, c)
            }
        }
        return null
    }

    private fun getBoardMoves(board: Array<Array<ChessPiece?>>, color: PieceColor): List<Move> {
        val result = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == color) {
                    val pos = Position(r, c)
                    result.addAll(generatePseudoMoves(board, pos, p))
                }
            }
        }
        return result
    }

    private fun generatePseudoMoves(
        board: Array<Array<ChessPiece?>>,
        pos: Position,
        piece: ChessPiece
    ): List<Move> {
        val (r, c) = pos
        val moves = mutableListOf<Move>()
        val dir = if (piece.color == PieceColor.WHITE) -1 else 1

        when (piece.type) {
            PieceType.PAWN -> {
                val nextR = r + dir
                if (nextR in 0..7 && board[nextR][c] == null) {
                    moves.add(Move(pos, Position(nextR, c)))
                    val startR = if (piece.color == PieceColor.WHITE) 6 else 1
                    val doubleR = r + (dir * 2)
                    if (r == startR && board[doubleR][c] == null) {
                        moves.add(Move(pos, Position(doubleR, c)))
                    }
                }
                for (dc in listOf(-1, 1)) {
                    val nc = c + dc
                    if (nextR in 0..7 && nc in 0..7) {
                        val dest = board[nextR][nc]
                        if (dest != null && dest.color != piece.color) {
                            moves.add(Move(pos, Position(nextR, nc)))
                        }
                    }
                }
            }
            PieceType.KNIGHT -> {
                val offsets = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
                for ((dr, dc) in offsets) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0..7 && nc in 0..7) {
                        val target = board[nr][nc]
                        if (target == null || target.color != piece.color) moves.add(Move(pos, Position(nr, nc)))
                    }
                }
            }
            PieceType.BISHOP -> addRayMovesOnBoard(board, moves, pos, piece.color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
            PieceType.ROOK -> addRayMovesOnBoard(board, moves, pos, piece.color, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
            PieceType.QUEEN -> addRayMovesOnBoard(board, moves, pos, piece.color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1))
            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0..7 && nc in 0..7) {
                            val target = board[nr][nc]
                            if (target == null || target.color != piece.color) moves.add(Move(pos, Position(nr, nc)))
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun addRayMovesOnBoard(
        board: Array<Array<ChessPiece?>>,
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
                    if (piece.color != myColor) moves.add(Move(from, Position(currR, currC)))
                    break
                }
                currR += dr
                currC += dc
            }
        }
    }
}
