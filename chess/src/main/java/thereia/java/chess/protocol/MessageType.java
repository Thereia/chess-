package thereia.java.chess.protocol;

public enum MessageType {
    Login,
    register,
    startMatch,
    Ready,
    move,
    Resign,
    loginResult,
    matchSuccess,
    roomInfo,
    gameStart,
    moveResult,
    timeout,
    gameOver,
    error
}
