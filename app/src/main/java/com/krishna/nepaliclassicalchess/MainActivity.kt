package com.krishna.nepaliclassicalchess

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.krishna.nepaliclassicalchess.ui.theme.NepaliClassicalChessTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.abs

data class ChessPiece(
    val type: String,
    val isWhite: Boolean,
    val hasMoved: Boolean = false
) {
    val imageSymbol: String
        get() = if (isWhite) type.uppercase() else type.lowercase()
}

data class Position(
    val row: Int,
    val col: Int
)

// Helper functions moved outside of ChessGame composable
fun getPieceColor(piece: ChessPiece?): String? {
    return when {
        piece == null -> null
        piece.isWhite -> "white"
        else -> "black"
    }
}

fun isInBounds(row: Int, col: Int): Boolean {
    return row >= 0 && row < 8 && col >= 0 && col < 8
}

fun isKingInCheck(board: List<List<ChessPiece?>>, kingColor: String): Boolean {
    // Find king position
    var kingRow = -1
    var kingCol = -1
    for (r in 0..7) {
        for (c in 0..7) {
            val piece = board[r][c]
            if (piece != null && piece.type.uppercase() == "K" && getPieceColor(piece) == kingColor) {
                kingRow = r
                kingCol = c
                break
            }
        }
        if (kingRow != -1) break
    }

    // Check if any opponent piece can capture the king
    for (r in 0..7) {
        for (c in 0..7) {
            val piece = board[r][c]
            if (piece != null && getPieceColor(piece) != kingColor) {
                // We need to pass the board to getValidMoves
                val moves = getValidMoves(piece, r, c, board)
                if (moves.any { it.row == kingRow && it.col == kingCol }) {
                    return true
                }
            }
        }
    }
    return false
}

fun getValidMoves(piece: ChessPiece, row: Int, col: Int, board: List<List<ChessPiece?>>): List<Position> {
    val moves = mutableListOf<Position>()
    val pieceType = piece.type.uppercase()
    val isWhite = piece.isWhite
    val direction = if (isWhite) -1 else 1

    fun addMove(r: Int, c: Int): Boolean {
        if (isInBounds(r, c)) {
            if (board[r][c] == null) {
                moves.add(Position(r, c))
                return true
            } else if (getPieceColor(board[r][c]) != getPieceColor(piece)) {
                moves.add(Position(r, c))
                return false
            }
            return false
        }
        return false
    }

    // Check if a square is under attack by opponent pieces
    fun isSquareAttacked(board: List<List<ChessPiece?>>, row: Int, col: Int, kingColor: String): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && getPieceColor(piece) != kingColor) {
                    // Get valid moves for this piece
                    val opponentMoves = getValidMoves(piece, r, c, board)
                    
                    // For pawns, we need to check capture moves specifically
                    if (piece.type.uppercase() == "P") {
                        val captureDirection = if (piece.isWhite) -1 else 1
                        // Check if this pawn can capture on the specified square
                        if ((r + captureDirection == row) && (c - 1 == col || c + 1 == col)) {
                            return true
                        }
                    } else {
                        // For other pieces, check if they can move to the square
                        if (opponentMoves.any { it.row == row && it.col == col }) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    // Check if castling is possible
    fun canCastle(board: List<List<ChessPiece?>>, isKingside: Boolean): Boolean {
        // King must not have moved
        if (piece.hasMoved) return false
        
        val kingRow = row
        val rookCol = if (isKingside) 7 else 0
        val rook = board[kingRow][rookCol]
        
        // Rook must exist and not have moved
        if (rook == null || rook.type.uppercase() != "R" || rook.hasMoved) return false
        
        // Squares between king and rook must be empty
        if (isKingside) {
            // Check f1/f8 and g1/g8 are empty (for kingside castling)
            if (board[kingRow][col + 1] != null || board[kingRow][col + 2] != null) return false
        } else {
            // Check b1/b8, c1/c8, and d1/d8 are empty (for queenside castling)
            if (board[kingRow][col - 1] != null || board[kingRow][col - 2] != null || board[kingRow][col - 3] != null) return false
        }
        
        // King must not be in check
        if (isKingInCheck(board, if (isWhite) "white" else "black")) return false
        
        // Squares king moves through must not be under attack
        val kingColor = if (isWhite) "white" else "black"
        if (isKingside) {
            // For kingside castling, check f1/f8 and g1/g8
            if (isSquareAttacked(board, kingRow, col + 1, kingColor)) return false
            if (isSquareAttacked(board, kingRow, col + 2, kingColor)) return false
        } else {
            // For queenside castling, check d1/d8 and c1/c8
            if (isSquareAttacked(board, kingRow, col - 1, kingColor)) return false
            if (isSquareAttacked(board, kingRow, col - 2, kingColor)) return false
        }
        
        return true
    }

    when (pieceType) {
        "P" -> { // Pawn
            // Forward move
            if (isInBounds(row + direction, col) && board[row + direction][col] == null) {
                moves.add(Position(row + direction, col))
                // Double move from starting position
                if ((isWhite && row == 6) || (!isWhite && row == 1)) {
                    if (board[row + 2 * direction][col] == null) {
                        moves.add(Position(row + 2 * direction, col))
                    }
                }
            }
            // Capture moves
            for (offset in listOf(-1, 1)) {
                if (isInBounds(row + direction, col + offset)) {
                    val targetPiece = board[row + direction][col + offset]
                    if (targetPiece != null && getPieceColor(targetPiece) != getPieceColor(piece)) {
                        moves.add(Position(row + direction, col + offset))
                    }
                }
            }
        }

        "R" -> { // Rook
            // Horizontal and vertical moves
            for (dir in listOf(
                listOf(0, 1), listOf(0, -1), listOf(1, 0), listOf(-1, 0)
            )) {
                for (i in 1..7) {
                    val newRow = row + dir[0] * i
                    val newCol = col + dir[1] * i
                    if (!isInBounds(newRow, newCol)) break
                    if (board[newRow][newCol] == null) {
                        moves.add(Position(newRow, newCol))
                    } else {
                        if (getPieceColor(board[newRow][newCol]) != getPieceColor(piece)) {
                            moves.add(Position(newRow, newCol))
                        }
                        break
                    }
                }
            }
        }

        "N" -> { // Knight
            val knightMoves = listOf(
                listOf(-2, -1), listOf(-2, 1), listOf(-1, -2), listOf(-1, 2),
                listOf(1, -2), listOf(1, 2), listOf(2, -1), listOf(2, 1)
            )
            for (move in knightMoves) {
                val newRow = row + move[0]
                val newCol = col + move[1]
                if (isInBounds(newRow, newCol)) {
                    if (board[newRow][newCol] == null || getPieceColor(board[newRow][newCol]) != getPieceColor(piece)) {
                        moves.add(Position(newRow, newCol))
                    }
                }
            }
        }

        "B" -> { // Bishop
            for (dir in listOf(
                listOf(1, 1), listOf(1, -1), listOf(-1, 1), listOf(-1, -1)
            )) {
                for (i in 1..7) {
                    val newRow = row + dir[0] * i
                    val newCol = col + dir[1] * i
                    if (!isInBounds(newRow, newCol)) break
                    if (board[newRow][newCol] == null) {
                        moves.add(Position(newRow, newCol))
                    } else {
                        if (getPieceColor(board[newRow][newCol]) != getPieceColor(piece)) {
                            moves.add(Position(newRow, newCol))
                        }
                        break
                    }
                }
            }
        }

        "Q" -> { // Queen
            // Combines rook and bishop moves
            for (dir in listOf(
                listOf(0, 1), listOf(0, -1), listOf(1, 0), listOf(-1, 0),
                listOf(1, 1), listOf(1, -1), listOf(-1, 1), listOf(-1, -1)
            )) {
                for (i in 1..7) {
                    val newRow = row + dir[0] * i
                    val newCol = col + dir[1] * i
                    if (!isInBounds(newRow, newCol)) break
                    if (board[newRow][newCol] == null) {
                        moves.add(Position(newRow, newCol))
                    } else {
                        if (getPieceColor(board[newRow][newCol]) != getPieceColor(piece)) {
                            moves.add(Position(newRow, newCol))
                        }
                        break
                    }
                }
            }
        }

        "K" -> { // King
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val newRow = row + dr
                    val newCol = col + dc
                    if (isInBounds(newRow, newCol)) {
                        if (board[newRow][newCol] == null || getPieceColor(board[newRow][newCol]) != getPieceColor(piece)) {
                            moves.add(Position(newRow, newCol))
                        }
                    }
                }
            }
            
            // Castling logic
            // Only allow castling if the king hasn't moved
            if (!piece.hasMoved) {
                // Kingside castling (O-O)
                if (canCastle(board, true)) {
                    moves.add(Position(row, if (isWhite) 6 else 6)) // g1 or g8
                }
                
                // Queenside castling (O-O-O)
                if (canCastle(board, false)) {
                    moves.add(Position(row, if (isWhite) 2 else 2)) // c1 or c8
                }
            }
        }
    }

    return moves
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPlayGame: () -> Unit
) {
    // Create initial board state for preview
    val previewBoard = remember { createInitialBoard() }
    
    // Use the same responsive design constants as in ChessGame
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // Use the same calculation as in ChessGame for consistency
    val maxBoardSize = minOf(screenWidth * 0.8f, screenHeight * 0.6f)
    val cellSize: Dp = maxBoardSize / 8
    val boardSize = cellSize * 8
    
    // Piece images mapping (same as in ChessGame)
    val pieceImages = mapOf(
        "K" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wk.png",
        "Q" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wq.png",
        "R" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wr.png",
        "B" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wb.png",
        "N" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wn.png",
        "P" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wp.png",
        "k" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bk.png",
        "q" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bq.png",
        "r" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/br.png",
        "b" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bb.png",
        "n" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bn.png",
        "p" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bp.png"
    )

    val pieceNames = mapOf(
        "K" to "White King", "Q" to "White Queen", "R" to "White Rook", "B" to "White Bishop", "N" to "White Knight", "P" to "White Pawn",
        "k" to "Black King", "q" to "Black Queen", "r" to "Black Rook", "b" to "Black Bishop", "n" to "Black Knight", "p" to "Black Pawn"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Test-Chess",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
                modifier = Modifier
                    .padding(bottom = 32.dp)
            )

            // Chess board preview with the same positioning as in ChessGame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .background(Color(0xFF3E2723), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top row labels (A-H) - same as in ChessGame
                    Row(
                        modifier = Modifier.width(boardSize + 40.dp)
                    ) {
                        // Spacer for left side labels
                        Spacer(modifier = Modifier.width(20.dp))
                        
                        // Coordinate labels
                        Row(
                            modifier = Modifier.width(boardSize),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("A", "B", "C", "D", "E", "F", "G", "H").forEach { letter ->
                                Text(
                                    text = letter,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD7CCC8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(boardSize / 8)
                                )
                            }
                        }
                        
                        // Spacer for right side labels
                        Spacer(modifier = Modifier.width(20.dp))
                    }

                    // Main content - same as in ChessGame
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left column labels (8-1) - same as in ChessGame
                        Column(
                            modifier = Modifier.width(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (i in 8 downTo 1) {  // Changed to 8 downTo 1 for correct numbering
                                Text(
                                    text = i.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD7CCC8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.height(boardSize / 8)
                                )
                            }
                        }

                        // Actual chess board - same as in ChessGame
                        Box(
                            modifier = Modifier
                                .size(boardSize)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723))
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color(0xFF8D6E63),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .shadow(8.dp, RoundedCornerShape(4.dp))
                        ) {
                            HomeScreenChessBoard(
                                board = previewBoard,
                                pieceImages = pieceImages,
                                pieceNames = pieceNames,
                                cellSize = cellSize
                            )
                        }

                        // Right column labels (8-1) - same as in ChessGame
                        Column(
                            modifier = Modifier.width(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (i in 8 downTo 1) {  // Changed to 8 downTo 1 for correct numbering
                                Text(
                                    text = i.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD7CCC8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.height(boardSize / 8)
                                )
                            }
                        }
                    }

                    // Bottom row labels (A-H) - same as in ChessGame
                    Row(
                        modifier = Modifier.width(boardSize + 40.dp)
                    ) {
                        // Spacer for left side labels
                        Spacer(modifier = Modifier.width(20.dp))
                        
                        // Coordinate labels
                        Row(
                            modifier = Modifier.width(boardSize),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("A", "B", "C", "D", "E", "F", "G", "H").forEach { letter ->
                                Text(
                                    text = letter,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD7CCC8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(boardSize / 8)
                                )
                            }
                        }
                        
                        // Spacer for right side labels
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                }
            }

            Button(
                onClick = { onPlayGame() },
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(200.dp)
                    .height(50.dp)
            ) {
                Text(
                    text = "Play Game",
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun HomeScreenChessBoard(
    board: List<List<ChessPiece?>>,
    pieceImages: Map<String, String>,
    pieceNames: Map<String, String>,
    cellSize: Dp
) {
    val pieceSize = cellSize - 6.dp
    
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        // Render chess board squares
        for (row in 0..7) {
            for (col in 0..7) {
                val isLightSquare = (row + col) % 2 == 0
                
                Box(
                    modifier = Modifier
                        .size(cellSize)
                        .offset(x = cellSize * col, y = cellSize * row)
                        .background(
                            if (isLightSquare) Color(0xFFD7CCC8) else Color(0xFF5D4037)
                        )
                )
            }
        }
        
        // Render pieces
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                if (piece != null) {
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .offset(x = cellSize * col, y = cellSize * row),
                        contentAlignment = Alignment.Center
                    ) {
                        key(piece.imageSymbol) {
                            AsyncImage(
                                model = pieceImages[piece.imageSymbol],
                                contentDescription = pieceNames[piece.imageSymbol],
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(pieceSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun createInitialBoard(): List<List<ChessPiece?>> {
    val board = Array(8) { arrayOfNulls<ChessPiece>(8) }

    // Set up pawns
    for (i in 0..7) {
        board[1][i] = ChessPiece("p", false, false) // Black pawns
        board[6][i] = ChessPiece("P", true, false)  // White pawns
    }

    // Set up other pieces
    // Black pieces (row 0)
    board[0][0] = ChessPiece("r", false, false) // Rook
    board[0][1] = ChessPiece("n", false, false) // Knight
    board[0][2] = ChessPiece("b", false, false) // Bishop
    board[0][3] = ChessPiece("q", false, false) // Queen
    board[0][4] = ChessPiece("k", false, false) // King
    board[0][5] = ChessPiece("b", false, false) // Bishop
    board[0][6] = ChessPiece("n", false, false) // Knight
    board[0][7] = ChessPiece("r", false, false) // Rook

    // White pieces (row 7)
    board[7][0] = ChessPiece("R", true, false) // Rook
    board[7][1] = ChessPiece("N", true, false) // Knight
    board[7][2] = ChessPiece("B", true, false) // Bishop
    board[7][3] = ChessPiece("Q", true, false) // Queen
    board[7][4] = ChessPiece("K", true, false) // King
    board[7][5] = ChessPiece("B", true, false) // Bishop
    board[7][6] = ChessPiece("N", true, false) // Knight
    board[7][7] = ChessPiece("R", true, false) // Rook

    // Debug: Print board state
    println("Initial board state:")
    for (row in board.indices) {
        for (col in board[row].indices) {
            val piece = board[row][col]
            print(if (piece != null) piece.imageSymbol else ".")
            print(" ")
        }
        println()
    }

    // Verify knights
    println("\nVerifying knights:")
    var blackKnights = 0
    var whiteKnights = 0

    for (row in 0..7) {
        for (col in 0..7) {
            val piece = board[row][col]
            if (piece != null) {
                if (piece.type == "n") {
                    blackKnights++
                    println("Found black knight at ($row,$col)")
                } else if (piece.type == "N") {
                    whiteKnights++
                    println("Found white knight at ($row,$col)")
                }
            }
        }
    }

    println("Total black knights: $blackKnights")
    println("Total white knights: $whiteKnights")

    if (blackKnights != 2) {
        println("ERROR: Expected 2 black knights, found $blackKnights")
    }

    if (whiteKnights != 2) {
        println("ERROR: Expected 2 white knights, found $whiteKnights")
    }

    return board.map { it.toList() }.toList()
}

@Composable
fun ChessGame(
    modifier: Modifier = Modifier,
    onMoveSound: () -> Unit,
    onCaptureSound: () -> Unit,
    onCheckSound: () -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    // Game state
    var board: List<List<ChessPiece?>> by remember { mutableStateOf(createInitialBoard()) }
    var selectedSquare: Position? by remember { mutableStateOf<Position?>(null) }
    var currentPlayer: String by remember { mutableStateOf("white") } // "white" or "black"
    var validMoves: List<Position> by remember { mutableStateOf<List<Position>>(emptyList()) }
    var gameStatus: String by remember { mutableStateOf("playing") } // "playing", "white wins", "black wins", "draw"
    var capturedPieces: Map<String, List<ChessPiece>> by remember {
        mutableStateOf<Map<String, List<ChessPiece>>>(
            mapOf("white" to listOf<ChessPiece>(), "black" to listOf<ChessPiece>())
        )
    }
    var kingInCheck: String? by remember { mutableStateOf<String?>(null) } // "white", "black", or null
    var temporaryKingInCheck: String? by remember { mutableStateOf<String?>(null) } // Temporary check visualization
    
    // Animation states
    var movingPiece: ChessPiece? by remember { mutableStateOf(null) }
    var moveFrom: Position? by remember { mutableStateOf(null) }
    var moveTo: Position? by remember { mutableStateOf(null) }
    val animationProgress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Piece images mapping (urls cleaned)
    val pieceImages = mapOf(
        "K" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wk.png",
        "Q" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wq.png",
        "R" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wr.png",
        "B" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wb.png",
        "N" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wn.png",
        "P" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/wp.png",
        "k" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bk.png",
        "q" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bq.png",
        "r" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/br.png",
        "b" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bb.png",
        "n" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bn.png",
        "p" to "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/bp.png"
    )

    val pieceNames = mapOf(
        "K" to "White King", "Q" to "White Queen", "R" to "White Rook", "B" to "White Bishop", "N" to "White Knight", "P" to "White Pawn",
        "k" to "Black King", "q" to "Black Queen", "r" to "Black Rook", "b" to "Black Bishop", "n" to "Black Knight", "p" to "Black Pawn"
    )

    fun wouldBeInCheck(board: List<List<ChessPiece?>>, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val newBoard: MutableList<MutableList<ChessPiece?>> = board.map { it.toMutableList() }.toMutableList()
        val movingPiece = newBoard[fromRow][fromCol]
        
        // Check if this is a castling move
        val isCastling = movingPiece != null && 
                        movingPiece.type.uppercase() == "K" && 
                        !movingPiece.hasMoved &&
                        abs(toCol - fromCol) == 2
        
        if (isCastling) {
            // Kingside castling
            if (toCol > fromCol) {
                // Move king two squares to the right
                newBoard[fromRow][toCol] = movingPiece?.copy(hasMoved = true)
                newBoard[fromRow][fromCol] = null
                
                // Move rook from h-file to f-file
                val rook = newBoard[fromRow][7]
                newBoard[fromRow][5] = rook?.copy(hasMoved = true)
                newBoard[fromRow][7] = null
            } 
            // Queenside castling
            else {
                // Move king two squares to the left
                newBoard[fromRow][toCol] = movingPiece?.copy(hasMoved = true)
                newBoard[fromRow][fromCol] = null
                
                // Move rook from a-file to d-file
                val rook = newBoard[fromRow][0]
                newBoard[fromRow][3] = rook?.copy(hasMoved = true)
                newBoard[fromRow][0] = null
            }
        } else {
            // Regular move
            newBoard[toRow][toCol] = movingPiece?.copy(hasMoved = true)
            newBoard[fromRow][fromCol] = null
        }

        val kingColor = getPieceColor(movingPiece)
        return if (kingColor != null) isKingInCheck(newBoard, kingColor) else false
    }

    fun handleSquareClick(row: Int, col: Int) {
        if (gameStatus != "playing") return

        val piece = board[row][col]
        val pieceColor = getPieceColor(piece)

        // If no square is selected and the clicked square has a piece of the current player's color
        if (selectedSquare == null && piece != null && pieceColor == currentPlayer) {
            selectedSquare = Position(row, col)
            val moves = getValidMoves(piece, row, col, board).filter {
                !wouldBeInCheck(board, row, col, it.row, it.col)
            }
            validMoves = moves
        }
        // If a square is already selected
        else if (selectedSquare != null) {
            val selectedRow = selectedSquare!!.row
            val selectedCol = selectedSquare!!.col

            // If clicking the same square, deselect it
            if (selectedRow == row && selectedCol == col) {
                selectedSquare = null
                validMoves = emptyList()
                return
            }

            // If clicking another piece of the same color, select that piece instead
            if (piece != null && pieceColor == currentPlayer) {
                selectedSquare = Position(row, col)
                val moves = getValidMoves(piece, row, col, board).filter {
                    !wouldBeInCheck(board, row, col, it.row, it.col)
                }
                validMoves = moves
                return
            }

            // Check if the move is valid
            val isValidMove = validMoves.any { it.row == row && it.col == col }

            if (isValidMove) {
                // Handle captures
                val capturedPiece = board[row][col]
                if (capturedPiece != null) {
                    onCaptureSound()
                } else {
                    onMoveSound()
                }

                // Update captured pieces
                if (capturedPiece != null) {
                    val updatedCapturedPieces = capturedPieces.toMutableMap()
                    updatedCapturedPieces[currentPlayer] = updatedCapturedPieces[currentPlayer]!! + capturedPiece
                    capturedPieces = updatedCapturedPieces
                }

                // Start animation
                movingPiece = board[selectedRow][selectedCol]
                moveFrom = Position(selectedRow, selectedCol)
                moveTo = Position(row, col)
                
                coroutineScope.launch {
                    animationProgress.snapTo(0f)
                    animationProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 500, // Longer animation for smoother experience
                            easing = FastOutSlowInEasing // Smoother easing
                        )
                    )
                    
                    // Make the move after animation completes
                    val newBoard: MutableList<MutableList<ChessPiece?>> = board.map { it.toMutableList() }.toMutableList()
                    val movingPieceValue = newBoard[selectedRow][selectedCol]
                    
                    // Check if this is a castling move
                    val isCastling = movingPieceValue != null && 
                                   movingPieceValue.type.uppercase() == "K" && 
                                   !movingPieceValue.hasMoved &&
                                   abs(col - selectedCol) == 2
                    
                    if (isCastling) {
                        // Kingside castling
                        if (col > selectedCol) {
                            // Move king two squares to the right
                            newBoard[row][col] = movingPieceValue?.copy(hasMoved = true)
                            newBoard[selectedRow][selectedCol] = null
                            
                            // Move rook from h-file to f-file
                            val rook = newBoard[row][7]
                            newBoard[row][5] = rook?.copy(hasMoved = true)
                            newBoard[row][7] = null
                        } 
                        // Queenside castling
                        else {
                            // Move king two squares to the left
                            newBoard[row][col] = movingPieceValue?.copy(hasMoved = true)
                            newBoard[selectedRow][selectedCol] = null
                            
                            // Move rook from a-file to d-file
                            val rook = newBoard[row][0]
                            newBoard[row][3] = rook?.copy(hasMoved = true)
                            newBoard[row][0] = null
                        }
                    } else {
                        // Regular move
                        newBoard[row][col] = movingPieceValue?.copy(hasMoved = true)
                        newBoard[selectedRow][selectedCol] = null

                        // Check for pawn promotion
                        if (movingPieceValue != null && movingPieceValue.type.uppercase() == "P" && (row == 0 || row == 7)) {
                            newBoard[row][col] = ChessPiece("Q", movingPieceValue.isWhite, true)
                        }
                    }

                    board = newBoard
                    movingPiece = null
                    moveFrom = null
                    moveTo = null
                    animationProgress.snapTo(0f)

                    // Switch player
                    val nextPlayer = if (currentPlayer == "white") "black" else "white"
                    currentPlayer = nextPlayer

                    // Check if the next player is in checkmate or stalemate
                    var hasLegalMoves = false
                    outerLoop@ for (r in 0..7) {
                        for (c in 0..7) {
                            val pieceToCheck = newBoard[r][c]
                            if (pieceToCheck != null && getPieceColor(pieceToCheck) == nextPlayer) {
                                val moves = getValidMoves(pieceToCheck, r, c, newBoard).filter {
                                    !wouldBeInCheck(newBoard, r, c, it.row, it.col)
                                }
                                if (moves.isNotEmpty()) {
                                    hasLegalMoves = true
                                    break@outerLoop
                                }
                            }
                        }
                    }

                    // Check if king is now in check
                    val isInCheck = isKingInCheck(newBoard, nextPlayer)
                    if (isInCheck) {
                        // Play check sound
                        onCheckSound()
                        
                        // Trigger haptic feedback
                        try {
                            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                vibratorManager.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            }
                            
                            if (vibrator.hasVibrator()) {
                                vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                            }
                        } catch (e: Exception) {
                            Log.e("ChessGame", "Failed to trigger haptic feedback", e)
                        }

                        // Show temporary check visualization for 2.5 seconds
                        temporaryKingInCheck = nextPlayer
                        kingInCheck = null // Clear permanent check visualization
                        
                        // Clear temporary check visualization after 2.5 seconds
                        coroutineScope.launch {
                            delay(2500)
                            temporaryKingInCheck = null
                        }
                    } else {
                        // Clear any existing check visualization if king is no longer in check
                        kingInCheck = null
                        temporaryKingInCheck = null
                    }

                    if (!hasLegalMoves) {
                        if (isInCheck) {
                            gameStatus = if (currentPlayer == "white") "White wins by checkmate!" else "Black wins by checkmate!"
                        } else {
                            gameStatus = "Draw by stalemate!"
                        }
                    }
                }

                selectedSquare = null
                validMoves = emptyList()
            } else {
                // Deselect regardless
                selectedSquare = null
                validMoves = emptyList()
            }
        }
    }

    fun resetGame() {
        board = createInitialBoard()
        selectedSquare = null
        currentPlayer = "white"
        validMoves = emptyList()
        gameStatus = "playing"
        capturedPieces = mapOf("white" to listOf(), "black" to listOf())
        kingInCheck = null
        temporaryKingInCheck = null
        movingPiece = null
        moveFrom = null
        moveTo = null
        coroutineScope.launch {
            animationProgress.snapTo(0f)
        }
    }

    // --- Responsive layout constants ---
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    val maxBoardSize = minOf(screenWidth * 0.8f, screenHeight * 0.6f)
    val cellSize: Dp = maxBoardSize / 8
    val boardSize = cellSize * 8
    val borderWidth: Dp = 2.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF5D4037))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button and title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBackToHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF795548)
                )
            ) {
                Text(
                    text = "Back",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "Test-Chess",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            // Empty spacer to balance the row
            Spacer(modifier = Modifier.width(74.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Captured pieces - Black (captured by white)
            Column(
                modifier = Modifier
                    .width(140.dp) // Reduced width for better fit
                    .background(Color(0xFF5D4037), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Captured by White",
                    fontSize = 14.sp, // Reduced font size
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow {
                    items(capturedPieces["white"] ?: emptyList()) { piece ->
                        Box(
                            modifier = Modifier
                                .size(35.dp) // Reduced size
                                .padding(2.dp)
                        ) {
                            AsyncImage(
                                model = pieceImages[piece.imageSymbol],
                                contentDescription = pieceNames[piece.imageSymbol],
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(35.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Captured pieces - White (captured by black)
            Column(
                modifier = Modifier
                    .width(140.dp) // Reduced width for better fit
                    .background(Color(0xFF5D4037), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF64B5F6), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Captured by Black",
                    fontSize = 14.sp, // Reduced font size
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow {
                    items(capturedPieces["black"] ?: emptyList()) { piece ->
                        Box(
                            modifier = Modifier
                                .size(35.dp) // Reduced size
                                .padding(2.dp)
                        ) {
                            AsyncImage(
                                model = pieceImages[piece.imageSymbol],
                                contentDescription = pieceNames[piece.imageSymbol],
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(35.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chess Board with Guaranteed Coordinate Alignment
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3E2723), RoundedCornerShape(8.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row labels (A-H)
                Row(
                    modifier = Modifier.width(boardSize + 40.dp)
                ) {
                    // Spacer for left side labels
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    // Coordinate labels
                    Row(
                        modifier = Modifier.width(boardSize),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("A", "B", "C", "D", "E", "F", "G", "H").forEach { letter ->
                            Text(
                                text = letter,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD7CCC8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(boardSize / 8)
                            )
                        }
                    }
                    
                    // Spacer for right side labels
                    Spacer(modifier = Modifier.width(20.dp))
                }

                // Main content
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left column labels (8-1)
                    Column(
                        modifier = Modifier.width(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 1..8) {
                            Text(
                                text = i.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD7CCC8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.height(boardSize / 8)
                            )
                        }
                    }

                    // Actual chess board
                    Box(
                        modifier = Modifier
                            .size(boardSize)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723))
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = Color(0xFF8D6E63),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .shadow(8.dp, RoundedCornerShape(4.dp))
                    ) {
                        ImprovedChessBoard(
                            board = board,
                            selectedSquare = selectedSquare,
                            validMoves = validMoves,
                            kingInCheck = kingInCheck,
                            temporaryKingInCheck = temporaryKingInCheck,
                            pieceImages = pieceImages,
                            pieceNames = pieceNames,
                            cellSize = cellSize,
                            movingPiece = movingPiece,
                            moveFrom = moveFrom,
                            moveTo = moveTo,
                            animationProgress = animationProgress.value,
                            onSquareClick = { row, col -> handleSquareClick(row, col) }
                        )
                    }

                    // Right column labels (8-1)
                    Column(
                        modifier = Modifier.width(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 1..8) {
                            Text(
                                text = i.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD7CCC8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.height(boardSize / 8)
                            )
                        }
                    }
                }

                // Bottom row labels (A-H)
                Row(
                    modifier = Modifier.width(boardSize + 40.dp)
                ) {
                    // Spacer for left side labels
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    // Coordinate labels
                    Row(
                        modifier = Modifier.width(boardSize),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("A", "B", "C", "D", "E", "F", "G", "H").forEach { letter ->
                            Text(
                                text = letter,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD7CCC8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(boardSize / 8)
                            )
                        }
                    }
                    
                    // Spacer for right side labels
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game status and reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (gameStatus == "playing") "Current Player: ${if (currentPlayer == "white") "White ♔" else "Black ♚"}" else gameStatus,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color(0xFF5D4037), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )

            Button(
                onClick = { resetGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB74D)
                ),
                modifier = Modifier
                    .shadow(4.dp)
            ) {
                Text(
                    text = "New Game",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChessGamePreview() {
    NepaliClassicalChessTheme {
        ChessGame(
            onMoveSound = {},
            onCaptureSound = {},
            onCheckSound = {},
            onBackToHome = {}
        )
    }
}

@Composable
fun ImprovedChessBoard(
    board: List<List<ChessPiece?>>,
    selectedSquare: Position?,
    validMoves: List<Position>,
    kingInCheck: String?,
    temporaryKingInCheck: String?,
    pieceImages: Map<String, String>,
    pieceNames: Map<String, String>,
    cellSize: Dp,
    movingPiece: ChessPiece?,
    moveFrom: Position?,
    moveTo: Position?,
    animationProgress: Float,
    onSquareClick: (Int, Int) -> Unit
) {
    fun getPieceColor(piece: ChessPiece?): String? {
        return when {
            piece == null -> null
            piece.isWhite -> "white"
            else -> "black"
        }
    }

    val boardSize = cellSize * 8
    val pieceSize = cellSize - 6.dp

    BoxWithConstraints(
        modifier = Modifier
            .size(boardSize)
    ) {
        // 8x8 Grid of squares with improved styling
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                val isLightSquare = (row + col) % 2 == 0
                val isSelected = selectedSquare?.row == row && selectedSquare?.col == col
                val isValidMove = validMoves.any { it.row == row && it.col == col }
                
                // Check for either permanent or temporary king in check visualization
                val isKingInCheckHere = (kingInCheck != null || temporaryKingInCheck != null) &&
                        piece != null &&
                        piece.type.uppercase() == "K" &&
                        getPieceColor(piece) == (kingInCheck ?: temporaryKingInCheck)

                // Position each square in the grid with improved styling
                Box(
                    modifier = Modifier
                        .size(cellSize)
                        .offset(x = cellSize * col, y = cellSize * row)
                        .background(
                            when {
                                isSelected -> Color(0xFFFFA000) // Amber for selected
                                isKingInCheckHere -> Color(0xFFF44336) // Red for check
                                isValidMove && piece == null -> Color(0xFF4CAF50) // Green for valid moves
                                isValidMove && piece != null -> Color(0xFF4CAF50) // Green for captures
                                isLightSquare -> Color(0xFFD7CCC8) // Light square color
                                else -> Color(0xFF5D4037) // Dark square color
                            }
                        )
                        .clickable { onSquareClick(row, col) }
                        .shadow(if (isSelected) 4.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isValidMove) {
                        // Enhanced visual indicator for valid moves
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = if (piece == null) Color(0x804CAF50) else Color(0x80F44336),
                                    shape = RoundedCornerShape(50)
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }
        }
        
        // Render static pieces
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                
                // Skip the moving piece's original position during animation
                if (movingPiece != null && 
                    moveFrom != null && 
                    moveFrom.row == row && 
                    moveFrom.col == col) {
                    continue
                }
                
                // Skip the destination square if it's being animated
                if (movingPiece != null && 
                    moveTo != null && 
                    moveTo.row == row && 
                    moveTo.col == col) {
                    continue
                }
                
                if (piece != null) {
                    val isSelected = selectedSquare?.row == row && selectedSquare?.col == col
                    
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .offset(x = cellSize * col, y = cellSize * row),
                        contentAlignment = Alignment.Center
                    ) {
                        key(piece.imageSymbol) {
                            AsyncImage(
                                model = pieceImages[piece.imageSymbol],
                                contentDescription = pieceNames[piece.imageSymbol],
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(pieceSize)
                                    .shadow(if (isSelected) 8.dp else 0.dp)
                                    // Add a subtle scale effect when selected
                                    .graphicsLayer(
                                        scaleX = if (isSelected) 1.05f else 1f,
                                        scaleY = if (isSelected) 1.05f else 1f
                                    )
                            )
                        }
                    }
                }
            }
        }
        
        // Render animated moving piece
        if (movingPiece != null && moveFrom != null && moveTo != null) {
            val startX = moveFrom.col * cellSize.value
            val startY = moveFrom.row * cellSize.value
            val endX = moveTo.col * cellSize.value
            val endY = moveTo.row * cellSize.value
            
            // Calculate current position based on animation progress
            val currentX = startX + (endX - startX) * animationProgress
            val currentY = startY + (endY - startY) * animationProgress
            
            // Add a subtle scaling effect during animation
            val scale = 1f + (0.1f * animationProgress * (1 - animationProgress))
            
            // Add a slight elevation during movement (using value-based calculation)
            val elevationFactor = animationProgress * (1 - animationProgress)
            val elevation = (16 * elevationFactor).dp
            
            Box(
                modifier = Modifier
                    .size(cellSize)
                    .offset(x = currentX.dp, y = currentY.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    ),
                contentAlignment = Alignment.Center
            ) {
                key(movingPiece.imageSymbol) {
                    AsyncImage(
                        model = pieceImages[movingPiece.imageSymbol],
                        contentDescription = pieceNames[movingPiece.imageSymbol],
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(pieceSize)
                            .shadow(elevation)
                    )
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private var toneGenerator: ToneGenerator? = null
    private val TAG = "ChessGame"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize tone generator for chess sounds (with error handling)
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ToneGenerator", e)
        }

        setContent {
            NepaliClassicalChessTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var showHomeScreen by remember { mutableStateOf(true) }

                    if (showHomeScreen) {
                        HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            onPlayGame = { showHomeScreen = false }
                        )
                    } else {
                        ChessGame(
                            modifier = Modifier.padding(innerPadding),
                            onMoveSound = {
                                try {
                                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to play move sound", e)
                                }
                            },
                            onCaptureSound = {
                                try {
                                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to play capture sound", e)
                                }
                            },
                            onCheckSound = {
                                try {
                                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to play check sound", e)
                                }
                            },
                            onBackToHome = { showHomeScreen = true }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release ToneGenerator", e)
        }
    }
}
