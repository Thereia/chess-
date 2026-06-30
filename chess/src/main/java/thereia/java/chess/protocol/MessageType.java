package thereia.java.chess.protocol;

public enum MessageType {
    startMatch,
    Ready,
    move,
    Resign,
    matchSuccess,
    gameStart,
    moveResult,
    timeout,
    gameOver,
    error
}
