package com.example

import com.example.presentation.games.ChessGameState
import com.example.presentation.games.PieceColor
import com.example.presentation.games.PieceType
import com.example.presentation.games.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessGameLogicTest {

    @Test
    fun testInitialSetup() {
        val game = ChessGameState()
        assertEquals(PieceColor.WHITE, game.currentTurn)
        assertFalse(game.isCheck)
        assertNull(game.winner)

        // White pawns on row 6
        for (c in 0..7) {
            val piece = game.board[6][c]
            assertNotNull(piece)
            assertEquals(PieceType.PAWN, piece?.type)
            assertEquals(PieceColor.WHITE, piece?.color)
        }

        // Kings
        assertEquals(PieceType.KING, game.board[7][4]?.type)
        assertEquals(PieceColor.WHITE, game.board[7][4]?.color)
        assertEquals(PieceType.KING, game.board[0][4]?.type)
        assertEquals(PieceColor.BLACK, game.board[0][4]?.color)
    }

    @Test
    fun testPawnInitialDoubleMoveAndTurnSwitch() {
        val game = ChessGameState()
        // White moves pawn e2 to e4 (row 6, col 4 to row 4, col 4)
        assertTrue(game.selectOrMove(6, 4)) // select
        assertTrue(game.selectOrMove(4, 4)) // move

        assertEquals(PieceColor.BLACK, game.currentTurn)
        assertNull(game.board[6][4])
        assertEquals(PieceType.PAWN, game.board[4][4]?.type)
        assertEquals(Position(5, 4), game.enPassantTarget)
    }

    @Test
    fun testSequentialMovesAndCapture() {
        val game = ChessGameState()
        // 1. e4 (6, 4 -> 4, 4)
        assertTrue(game.selectOrMove(6, 4))
        assertTrue(game.selectOrMove(4, 4))
        assertEquals(PieceColor.BLACK, game.currentTurn)

        // 1... d5 (1, 3 -> 3, 3)
        assertTrue(game.selectOrMove(1, 3))
        assertTrue(game.selectOrMove(3, 3))
        assertEquals(PieceColor.WHITE, game.currentTurn)

        // 2. exd5 (4, 4 -> 3, 3) White captures Black pawn on d5
        assertTrue(game.selectOrMove(4, 4))
        assertTrue(Position(3, 3) in game.validMoves)
        assertTrue(game.selectOrMove(3, 3))
        assertEquals(PieceColor.BLACK, game.currentTurn)
        assertEquals(PieceType.PAWN, game.board[3][3]?.type)
        assertEquals(PieceColor.WHITE, game.board[3][3]?.color)
        assertEquals(1, game.capturedBlack.size)

        // 2... Qxd5 (0, 3 -> 3, 3) Black Queen captures White pawn on d5
        assertTrue(game.selectOrMove(0, 3))
        assertTrue(Position(3, 3) in game.validMoves)
        assertTrue(game.selectOrMove(3, 3))
        assertEquals(PieceColor.WHITE, game.currentTurn)
        assertEquals(PieceType.QUEEN, game.board[3][3]?.type)
        assertEquals(PieceColor.BLACK, game.board[3][3]?.color)
        assertEquals(1, game.capturedWhite.size)
    }

    @Test
    fun testToggleDeselection() {
        val game = ChessGameState()
        assertTrue(game.selectOrMove(6, 4))
        assertEquals(Position(6, 4), game.selectedPosition)
        assertTrue(game.validMoves.isNotEmpty())

        // Tapping the same square again deselects
        assertTrue(game.selectOrMove(6, 4))
        assertNull(game.selectedPosition)
        assertTrue(game.validMoves.isEmpty())
    }

    @Test
    fun testFoolsMateCheckEndsGameImmediately() {
        // 1. f3 e5 2. g4 Qh4#
        val game = ChessGameState()

        // 1. f3 (row 6, col 5 -> row 5, col 5)
        game.selectOrMove(6, 5)
        game.selectOrMove(5, 5)

        // 1... e5 (row 1, col 4 -> row 3, col 4)
        game.selectOrMove(1, 4)
        game.selectOrMove(3, 4)

        // 2. g4 (row 6, col 6 -> row 4, col 6)
        game.selectOrMove(6, 6)
        game.selectOrMove(4, 6)

        // 2... Qh4# (row 0, col 3 -> row 4, col 7)
        game.selectOrMove(0, 3)
        game.selectOrMove(4, 7)

        // Check occurred! Game must end immediately with Black winner
        assertTrue("White should be in check", game.isCheck)
        assertEquals("Black should be the winner", PieceColor.BLACK, game.winner)
        assertTrue("Check must end game immediately", game.isCheckGameOver)
    }

    @Test
    fun testPieceCannotMoveShowsBlockedStatus() {
        val game = ChessGameState()
        // Initially, the White Rook at (7, 0) cannot move because it is trapped by pawns/pieces!
        assertTrue(game.selectOrMove(7, 0))
        assertTrue("Rook at (7,0) cannot move at game start", game.selectedPieceHasNoMoves)
        assertTrue(game.validMoves.isEmpty())
        assertNotNull(game.selectionStatusText)
        assertTrue(game.selectionStatusText!!.contains("مسدود"))

        // White Knight at (7, 1) CAN move at game start (to 5, 0 and 5, 2)
        assertTrue(game.selectOrMove(7, 1))
        assertFalse("Knight at (7,1) has valid moves", game.selectedPieceHasNoMoves)
        assertEquals(2, game.validMoves.size)
        assertTrue(Position(5, 0) in game.validMoves)
        assertTrue(Position(5, 2) in game.validMoves)
    }

    @Test
    fun testCastlingKingSide() {
        val game = ChessGameState()
        // Clear squares between king and rook for White
        // Move e2-e4
        game.selectOrMove(6, 4); game.selectOrMove(4, 4)
        // Black e7-e5
        game.selectOrMove(1, 4); game.selectOrMove(3, 4)
        // White Nf3 (row 7, col 6 -> row 5, col 5)
        game.selectOrMove(7, 6); game.selectOrMove(5, 5)
        // Black Nc6 (row 0, col 1 -> row 2, col 2)
        game.selectOrMove(0, 1); game.selectOrMove(2, 2)
        // White Be2 (row 7, col 5 -> row 6, col 4 or row 5, col 3)
        game.selectOrMove(7, 5); game.selectOrMove(6, 4)
        // Black Bc5 (row 0, col 5 -> row 3, col 2)
        game.selectOrMove(0, 5); game.selectOrMove(3, 2)

        // White King at (7, 4) should now have (7, 6) as a legal move (Castling O-O)
        assertTrue(game.selectOrMove(7, 4))
        assertTrue(Position(7, 6) in game.validMoves)

        // Execute castling
        assertTrue(game.selectOrMove(7, 6))

        // Check that king is at (7, 6) and rook is at (7, 5)
        assertEquals(PieceType.KING, game.board[7][6]?.type)
        assertEquals(PieceType.ROOK, game.board[7][5]?.type)
        assertNull(game.board[7][4])
        assertNull(game.board[7][7])
    }

    @Test
    fun testEnPassantCapture() {
        val game = ChessGameState()
        // 1. e4 (6,4 -> 4,4)
        game.selectOrMove(6, 4); game.selectOrMove(4, 4)
        // 1... a6 (1,0 -> 2,0)
        game.selectOrMove(1, 0); game.selectOrMove(2, 0)
        // 2. e5 (4,4 -> 3,4)
        game.selectOrMove(4, 4); game.selectOrMove(3, 4)
        // 2... d5 (1,3 -> 3,3) double move adjacent to e5
        game.selectOrMove(1, 3); game.selectOrMove(3, 3)

        // Now White can capture en passant at (2, 3)
        assertTrue(game.selectOrMove(3, 4))
        assertTrue(Position(2, 3) in game.validMoves)

        // Execute en passant
        assertTrue(game.selectOrMove(2, 3))

        // White pawn is at (2, 3)
        assertEquals(PieceType.PAWN, game.board[2][3]?.type)
        assertEquals(PieceColor.WHITE, game.board[2][3]?.color)
        // Black pawn at (3, 3) should be captured
        assertNull(game.board[3][3])
        assertEquals(1, game.capturedBlack.size)
    }

    @Test
    fun testKingCannotMoveIntoCheck() {
        val game = ChessGameState()
        // 1. e4 (6,4 -> 4,4)
        game.selectOrMove(6, 4); game.selectOrMove(4, 4)
        // 1... e5 (1,4 -> 3,4)
        game.selectOrMove(1, 4); game.selectOrMove(3, 4)
        // 2. Ke2 (7,4 -> 6,4)
        game.selectOrMove(7, 4); game.selectOrMove(6, 4)
        // 2... Qh4 (0,3 -> 4,7) attacks e4 and enters play
        game.selectOrMove(0, 3); game.selectOrMove(4, 7)

        // King at (6, 4) cannot move to (5, 4) if it was attacked, etc.
        // Also verify valid moves for King do not include attacked squares
        game.selectOrMove(6, 4)
        for (m in game.validMoves) {
            assertFalse(game.isSquareAttacked(m, PieceColor.BLACK))
        }
    }
}
