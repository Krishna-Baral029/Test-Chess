package com.krishna.nepaliclassicalchess

import org.junit.Test
import org.junit.Assert.*

class ChessBoardTest {
    
    @Test
    fun testInitialBoardSetup() {
        val board = createInitialBoard()
        
        // Check that there are 2 black knights
        val blackKnights = mutableListOf<Pair<Int, Int>>()
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                if (piece != null && piece.type == "n") {
                    blackKnights.add(Pair(row, col))
                }
            }
        }
        
        assertEquals("There should be 2 black knights", 2, blackKnights.size)
        assertTrue("Black knight should be at position (0,1)", blackKnights.contains(Pair(0, 1)))
        assertTrue("Black knight should be at position (0,6)", blackKnights.contains(Pair(0, 6)))
        
        // Check that there are 2 white knights
        val whiteKnights = mutableListOf<Pair<Int, Int>>()
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                if (piece != null && piece.type == "N") {
                    whiteKnights.add(Pair(row, col))
                }
            }
        }
        
        assertEquals("There should be 2 white knights", 2, whiteKnights.size)
        assertTrue("White knight should be at position (7,1)", whiteKnights.contains(Pair(7, 1)))
        assertTrue("White knight should be at position (7,6)", whiteKnights.contains(Pair(7, 6)))
    }
    
    @Test
    fun testAllPiecesPresent() {
        val board = createInitialBoard()
        
        // Check pawns
        for (i in 0..7) {
            assertNotNull("Black pawn should be at (1,$i)", board[1][i])
            assertEquals("Piece at (1,$i) should be black pawn", "p", board[1][i]?.type)
            assertFalse("Piece at (1,$i) should be black", board[1][i]?.isWhite ?: true)
            
            assertNotNull("White pawn should be at (6,$i)", board[6][i])
            assertEquals("Piece at (6,$i) should be white pawn", "P", board[6][i]?.type)
            assertTrue("Piece at (6,$i) should be white", board[6][i]?.isWhite ?: false)
        }
        
        // Check back row pieces
        val backRowPieces = listOf("r", "n", "b", "q", "k", "b", "n", "r")
        for (i in 0..7) {
            assertNotNull("Black piece should be at (0,$i)", board[0][i])
            assertEquals("Piece at (0,$i) should be ${backRowPieces[i]}", backRowPieces[i], board[0][i]?.type)
            assertFalse("Piece at (0,$i) should be black", board[0][i]?.isWhite ?: true)
            
            val whitePieceType = backRowPieces[i].uppercase()
            assertNotNull("White piece should be at (7,$i)", board[7][i])
            assertEquals("Piece at (7,$i) should be $whitePieceType", whitePieceType, board[7][i]?.type)
            assertTrue("Piece at (7,$i) should be white", board[7][i]?.isWhite ?: false)
        }
    }
}