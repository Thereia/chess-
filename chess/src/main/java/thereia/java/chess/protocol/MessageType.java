package thereia.java.chess.protocol;

public enum MessageType {
    startMatch,
    Ready,
    move,
    Resign,
    matchSuccess,
    roomInfo,
    gameStart,
    moveResult,
    timeout,
    gameOver,
    error
}
