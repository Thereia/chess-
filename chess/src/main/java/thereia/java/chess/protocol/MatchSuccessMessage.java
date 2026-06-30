package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class MatchSuccessMessage {

    private final String messageType;
    private final String roomId;

    public MatchSuccessMessage(String messageType, String roomId) {
        this.messageType = messageType;
        this.roomId = roomId;
    }

}
