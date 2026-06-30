package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class TimeoutMessage {

    private final String messageType;
    private final String color;

    public TimeoutMessage(String messageType, String color) {
        this.messageType = messageType;
        this.color = color;
    }

}
