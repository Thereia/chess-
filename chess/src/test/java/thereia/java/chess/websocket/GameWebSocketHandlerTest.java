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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
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

    @Test
    void broadcastsMoveResultAfterValidMove() throws Exception {
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
        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"isFlip\":false}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> captorB = ArgumentCaptor.forClass(TextMessage.class);

        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        verify(sessionB, atLeastOnce()).sendMessage(captorB.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"moveResult\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"moveResult\""));
    }

    @Test
    void broadcastsGameOverAfterResign() throws Exception {
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
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Resign\"}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> captorB = ArgumentCaptor.forClass(TextMessage.class);

        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        verify(sessionB, atLeastOnce()).sendMessage(captorB.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"gameOver\"")
                        && payload.contains("\"reason\":\"resign\""));
        assertThat(captorB.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"gameOver\"")
                        && payload.contains("\"reason\":\"resign\""));
    }

    @Test
    void returnsRoomNotFoundWhenPlayerMovesWithoutRoom() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager);
        WebSocketSession sessionA = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");

        handler.afterConnectionEstablished(sessionA);
        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"isFlip\":false}"));

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());

        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .anyMatch(payload -> payload.contains("\"messageType\":\"error\"")
                        && payload.contains("\"code\":3001"));
    }

    @Test
    void schedulesTimeoutAfterBothPlayersReady() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        TimeoutScheduler scheduler = mock(TimeoutScheduler.class);
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, scheduler);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");
        doReturn(mockScheduledFuture()).when(scheduler)
                .schedule(org.mockito.ArgumentMatchers.any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));

        ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
        verify(scheduler).schedule(org.mockito.ArgumentMatchers.any(Runnable.class), delayCaptor.capture(),
                eq(TimeUnit.MILLISECONDS));
        assertThat(delayCaptor.getValue()).isBetween(59000L, 60000L);
    }

    @Test
    void reschedulesTimeoutAfterAcceptedMoveAndCancelsPreviousTask() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        TimeoutScheduler scheduler = mock(TimeoutScheduler.class);
        ScheduledFuture<?> firstFuture = mock(ScheduledFuture.class);
        ScheduledFuture<?> secondFuture = mock(ScheduledFuture.class);
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, scheduler);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("A");
        when(sessionB.getId()).thenReturn("B");
        doReturn(firstFuture).doReturn(secondFuture).when(scheduler)
                .schedule(org.mockito.ArgumentMatchers.any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);

        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"startMatch\"}"));
        handler.handleTextMessage(sessionA, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionB, new TextMessage("{\"messageType\":\"Ready\"}"));
        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"isFlip\":false}"));

        verify(firstFuture).cancel(false);
        ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
        verify(scheduler, org.mockito.Mockito.times(2))
                .schedule(org.mockito.ArgumentMatchers.any(Runnable.class), delayCaptor.capture(), eq(TimeUnit.MILLISECONDS));
        assertThat(delayCaptor.getAllValues()).allMatch(delay -> delay >= 59000L && delay <= 60000L);
    }

    @Test
    void ignoresStaleTimeoutTaskAfterMoveResetsDeadline() throws Exception {
        SessionRegistry sessionRegistry = new SessionRegistry();
        RoomManager roomManager = new RoomManager(new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));
        RecordingTimeoutScheduler scheduler = new RecordingTimeoutScheduler();
        GameWebSocketHandler handler = new GameWebSocketHandler(sessionRegistry, roomManager, scheduler);
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

        Runnable firstTask = scheduler.tasks().get(0);

        handler.handleTextMessage(sessionA,
                new TextMessage("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":3,\"toX\":\"a\",\"toY\":4,\"isFlip\":false}"));

        firstTask.run();

        ArgumentCaptor<TextMessage> captorA = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA, atLeastOnce()).sendMessage(captorA.capture());
        assertThat(captorA.getAllValues()).extracting(TextMessage::getPayload)
                .noneMatch(payload -> payload.contains("\"messageType\":\"timeout\""));
    }

    private static final class RecordingTimeoutScheduler implements TimeoutScheduler {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            tasks.add(command);
            return new NoOpScheduledFuture();
        }

        public List<Runnable> tasks() {
            return tasks;
        }
    }

    private static final class NoOpScheduledFuture implements ScheduledFuture<Object> {
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(60, TimeUnit.SECONDS);
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ScheduledFuture<?> mockScheduledFuture() {
        return mock(ScheduledFuture.class);
    }
}
