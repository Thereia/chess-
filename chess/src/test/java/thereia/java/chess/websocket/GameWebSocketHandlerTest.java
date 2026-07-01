package thereia.java.chess.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import thereia.java.chess.game.RoomManager;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameWebSocketHandlerTest {

    @TempDir
    Path dir;

    @Test
    void startsRoomAfterBothPlayersMatchAndReady() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> captorB = ArgumentCaptor.forClass(TextMessage.class);

        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        verify(sessionB, atLeastOnce()).sendMessage(captorB.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"matchSuccess\""));
        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"gameStart\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"matchSuccess\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"gameStart\""));
    }
}
