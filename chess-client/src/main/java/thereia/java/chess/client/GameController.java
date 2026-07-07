package thereia.java.chess.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class GameController {

    @FXML
    private Button startMatchButton;

    @FXML
    private Button cancelMatchButton;

    @FXML
    private Button readyButton;

    @FXML
    private Button resignButton;

    @FXML
    private Button logoutButton;

    @FXML
    private GridPane boardGrid;

    @FXML
    private Label gameStatusLabel;

    @FXML
    private Label currentTurnLabel;

    @FXML
    private Label yourColorLabel;

    @FXML
    private Label timeLeftLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private VBox moveLogBox;

    @FXML
    private HBox capturedRedBox;

    @FXML
    private HBox capturedBlackBox;

    @FXML
    private Label usernameDisplay;

    @FXML
    private BorderPane mainPane;

    @FXML
    private HBox playerInfoBox;

    @FXML
    private VBox topBar;

    @FXML
    private HBox topColLabels;

    @FXML
    private HBox bottomColLabels;

    @FXML
    private VBox leftRowLabels;

    private Label redPlayerLabel;
    private Label blackPlayerLabel;

    private JieqiWebSocketClient webSocketClient;
    private String currentUserId;
    private String currentNickName;
    private String myColor;
    private String currentTurn;
    private String gameStatus;
    private String roomId;
    private String opponentName;
    private Map<String, PieceInfo> boardState = new HashMap<>();
    private String selectedCell = null;
    private List<String> suggestedCells = new ArrayList<>();
    private java.util.Timer timer;
    private java.util.Timer heartbeatTimer;
    private long deadlineTime;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Stage gameOverStage;

    private static final Map<String, String> RED_SYMBOLS = Map.of(
            "KING", "帅", "ROOK", "车", "KNIGHT", "马",
            "CANNON", "炮", "PAWN", "兵", "GUARD", "仕", "BISHOP", "相"
    );

    private static final Map<String, String> BLACK_SYMBOLS = Map.of(
            "KING", "将", "ROOK", "车", "KNIGHT", "马",
            "CANNON", "炮", "PAWN", "卒", "GUARD", "士", "BISHOP", "象"
    );

    public void init(JieqiWebSocketClient client, String userId, String nickName) {
        this.webSocketClient = client;
        this.currentUserId = userId;
        this.currentNickName = nickName;

        webSocketClient.setMessageHandler(this::handleServerMessage);
        webSocketClient.setConnectionHandler(this::handleConnectionChange);

        usernameDisplay.setText("当前用户: " + nickName + " (" + userId + ")");
        gameStatus = "waiting";

        createPlayerLabels();
        setButtonStates(true, false, false, false);
        renderEmptyBoard();
        startHeartbeat();
    }

    private void handleConnectionChange(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                messageLabel.setText("连接已恢复");
                messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold; -fx-font-size: 14px;");
                
                if (gameStatus.equals("playing")) {
                    messageLabel.setText("连接已恢复，等待服务器同步...");
                }
            } else {
                messageLabel.setText("连接已断开，正在重连...");
                messageLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
        });
    }

    private void createPlayerLabels() {
        redPlayerLabel = new Label("红方: 等待...");
        redPlayerLabel.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; " +
                "-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 8 20; -fx-border-radius: 8; " +
                "-fx-border-color: #ef5350; -fx-border-width: 2px;");
        
        blackPlayerLabel = new Label("黑方: 等待...");
        blackPlayerLabel.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #212121; " +
                "-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 8 20; -fx-border-radius: 8; " +
                "-fx-border-color: #424242; -fx-border-width: 2px;");

        playerInfoBox.getChildren().addAll(redPlayerLabel, blackPlayerLabel);
    }

    @FXML
    void handleStartMatch(ActionEvent event) {
        webSocketClient.sendMessage("startMatch", null);
        gameStatusLabel.setText("匹配中...");
        setButtonStates(false, true, false, false);
        messageLabel.setText("正在寻找对手...");
    }

    @FXML
    void handleCancelMatch(ActionEvent event) {
        webSocketClient.sendMessage("cancelMatch", null);
        gameStatusLabel.setText("已取消匹配");
        setButtonStates(true, false, false, false);
        messageLabel.setText("已取消匹配");
    }

    @FXML
    void handleReady(ActionEvent event) {
        webSocketClient.sendMessage("Ready", null);
        readyButton.setDisable(true);
        readyButton.setText("已准备");
        messageLabel.setText("等待对手准备...");
    }

    @FXML
    void handleResign(ActionEvent event) {
        webSocketClient.sendMessage("Resign", null);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        stopHeartbeat();
        stopTimer();
        if (webSocketClient != null) {
            webSocketClient.stopReconnect();
            webSocketClient.close();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 450));
            stage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleServerMessage(JsonNode message) {
        Platform.runLater(() -> {
            try {
                String messageType = message.get("messageType").asText();

                switch (messageType) {
                    case "matchSuccess":
                        handleMatchSuccess(message);
                        break;
                    case "roomInfo":
                        handleRoomInfo(message);
                        break;
                    case "gameStart":
                        handleGameStart(message);
                        break;
                    case "moveResult":
                        handleMoveResult(message);
                        break;
                    case "timeout":
                        handleTimeout(message);
                        break;
                    case "gameOver":
                        handleGameOver(message);
                        break;
                    case "error":
                        String errorMsg = message.has("message") ? message.get("message").asText() :
                                         (message.has("reason") ? message.get("reason").asText() : "未知错误");
                        messageLabel.setText("错误: " + errorMsg);
                        break;
                    case "pong":
                        break;
                    default:
                        System.out.println("GameController: unknown message type: " + messageType);
                        break;
                }
            } catch (Exception e) {
                System.out.println("GameController.handleServerMessage error: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleMatchSuccess(JsonNode data) {
        roomId = data.has("roomId") ? data.get("roomId").asText() : null;
        opponentName = getOpponentName(data);

        gameStatusLabel.setText("匹配成功！");
        messageLabel.setText("对手: " + opponentName + "，请点击准备");

        redPlayerLabel.setText("红方: 准备中...");
        redPlayerLabel.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; " +
                "-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 8 20; -fx-border-radius: 8; " +
                "-fx-border-color: #ef5350; -fx-border-width: 2px;");
        
        blackPlayerLabel.setText("黑方: 准备中...");
        blackPlayerLabel.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #212121; " +
                "-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 8 20; -fx-border-radius: 8; " +
                "-fx-border-color: #424242; -fx-border-width: 2px;");

        yourColorLabel.setText("你的颜色: 等待分配...");
        yourColorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #666;");

        gameStatus = "preparing";
        setButtonStates(false, false, true, false);
    }

    private void handleRoomInfo(JsonNode data) {
        messageLabel.setText("对手已准备，请点击准备");
    }

    private void handleGameStart(JsonNode data) {
        myColor = data.get("yourColor").asText().toLowerCase();
        currentTurn = "red";
        gameStatus = "playing";

        updateTopBarColor();
        renderEmptyBoard();

        if (myColor.equals("red")) {
            yourColorLabel.setText("你的颜色: 🟥 红方");
            yourColorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #c62828; -fx-background-color: #ffebee; -fx-padding: 5 10; -fx-border-radius: 6;");
        } else {
            yourColorLabel.setText("你的颜色: ⬛ 黑方");
            yourColorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #212121; -fx-background-color: #e0e0e0; -fx-padding: 5 10; -fx-border-radius: 6;");
        }
        
        gameStatusLabel.setText("游戏开始！");
        gameStatusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #4caf50;");
        
        currentTurnLabel.setText("当前回合: " + (currentTurn.equals("red") ? "🟥 红方" : "⬛ 黑方"));
        currentTurnLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #f44336;");

        if (data.has("opponentNickName")) {
            opponentName = data.get("opponentNickName").asText();
        }

        updatePlayerLabels();
        
        if (currentTurn.equals(myColor)) {
            messageLabel.setText("轮到你走棋");
            messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            messageLabel.setText("等待对手走棋");
            messageLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold; -fx-font-size: 14px;");
        }

        setButtonStates(false, false, false, true);

        capturedRedBox.getChildren().clear();
        capturedBlackBox.getChildren().clear();
        moveLogBox.getChildren().clear();

        JsonNode initialBoard = data.get("initialBoard");
        if (initialBoard != null && initialBoard.isArray()) {
            boardState.clear();
            for (JsonNode pieceNode : initialBoard) {
                String x = pieceNode.get("x").asText();
                int y = pieceNode.get("y").asInt();
                String key = x + y;
                boardState.put(key, new PieceInfo(
                        pieceNode.get("color").asText().toLowerCase(),
                        pieceNode.get("piece").asText(),
                        pieceNode.get("visible").asBoolean()
                ));
            }
        }

        renderBoard();
        startTimer();
    }

    private void updateTopBarColor() {
        if (topBar != null) {
            if (myColor.equals("red")) {
                topBar.setStyle("-fx-background-color: linear-gradient(to right, #d32f2f, #f44336);");
            } else {
                topBar.setStyle("-fx-background-color: linear-gradient(to right, #212121, #424242);");
            }
        }
    }

    private void handleMoveResult(JsonNode data) {
        if (!data.get("success").asBoolean()) {
            String errorMsg = data.has("message") ? data.get("message").asText() : "走子无效";
            messageLabel.setText("错误: " + errorMsg);
            return;
        }

        JsonNode move = data.get("move");
        String fromX = move.get("fromX").asText();
        int fromY = move.get("fromY").asInt();
        String toX = move.get("toX").asText();
        int toY = move.get("toY").asInt();

        String from = fromX + fromY;
        String to = toX + toY;

        if (boardState.containsKey(to)) {
            PieceInfo captured = boardState.get(to);
            addCapturedPiece(captured, data.has("capturedPiece") ? data.get("capturedPiece").asText() : null);
        }

        PieceInfo movingPiece = boardState.get(from);
        if (movingPiece != null) {
            String flipResult = data.has("flipResult") ? data.get("flipResult").asText() : null;
            boardState.put(to, new PieceInfo(
                    movingPiece.color,
                    flipResult != null ? flipResult : movingPiece.type,
                    true
            ));
            boardState.remove(from);
        }

        if (data.has("currentTurn")) {
            currentTurn = data.get("currentTurn").asText().toLowerCase();
            currentTurnLabel.setText("当前回合: " + (currentTurn.equals("red") ? "🟥 红方" : "⬛ 黑方"));
            currentTurnLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #f44336;");
            
            if (currentTurn.equals(myColor)) {
                messageLabel.setText("轮到你走棋");
                messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else {
                messageLabel.setText("等待对手走棋");
                messageLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
            startTimer();
        }

        String flipText = data.has("flipResult") ? data.get("flipResult").asText() : null;
        String capturedText = data.has("capturedPiece") ? data.get("capturedPiece").asText() : null;
        addMoveLog(from, to, flipText, capturedText);

        renderBoard();
    }

    private void handleTimeout(JsonNode data) {
        String loserId = data.get("loserId").asText();

        gameStatusLabel.setText("超时！");
        gameStatus = "ended";

        stopTimer();
        setButtonStates(false, false, false, false);

        if (loserId.equals(currentUserId)) {
            messageLabel.setText("你超时了，游戏结束");
            showGameOverModal("LOSE", "由于你思索时间过长，遗憾超时输掉了对局。");
        } else {
            messageLabel.setText("对手超时，你获胜！");
            showGameOverModal("WIN", "由于对手思索超时，恭喜你获得了本局的胜利！");
        }
    }

    private void handleGameOver(JsonNode data) {
        gameStatusLabel.setText("游戏结束");
        gameStatus = "ended";

        stopTimer();
        setButtonStates(false, false, false, false);

        if (!data.has("winner")) {
            messageLabel.setText("和棋！");
            showGameOverModal("DRAW", "双方握手言和，本局平局。");
        } else if (data.get("winnerId").asText().equals(currentUserId)) {
            messageLabel.setText("恭喜你获胜！");
            showGameOverModal("WIN", "你成功击败了对手，赢得了本局胜利！");
        } else {
            messageLabel.setText("你输了");
            showGameOverModal("LOSE", "棋差一招，遗憾败北，再接再厉！");
        }
    }

    private void updatePlayerLabels() {
        if (myColor.equals("red")) {
            redPlayerLabel.setText("红方: " + currentNickName);
            blackPlayerLabel.setText("黑方: " + opponentName);
        } else {
            redPlayerLabel.setText("红方: " + opponentName);
            blackPlayerLabel.setText("黑方: " + currentNickName);
        }
    }

    private static final int CELL_SIZE = 60;
    private static final int MARGIN = 30;
    private static final int BOARD_WIDTH = 8 * CELL_SIZE;
    private static final int BOARD_HEIGHT = 9 * CELL_SIZE;

    private void renderEmptyBoard() {
        boardGrid.getChildren().clear();
        boardGrid.getColumnConstraints().clear();
        boardGrid.getRowConstraints().clear();

        Canvas boardCanvas = new Canvas(BOARD_WIDTH + MARGIN * 2, BOARD_HEIGHT + MARGIN * 2);
        drawBoardOnCanvas(boardCanvas.getGraphicsContext2D());
        boardGrid.add(boardCanvas, 0, 0, 9, 10);

        for (int col = 0; col < 9; col++) {
            ColumnConstraints cc = new ColumnConstraints(CELL_SIZE);
            boardGrid.getColumnConstraints().add(cc);
        }

        for (int row = 0; row < 10; row++) {
            RowConstraints rc = new RowConstraints(CELL_SIZE);
            boardGrid.getRowConstraints().add(rc);
        }

        boolean isRed = myColor != null && myColor.equals("red");
        for (int boardRow = 0; boardRow < 10; boardRow++) {
            int renderRow = isRed ? 9 - boardRow : boardRow;
            for (int col = 0; col < 9; col++) {
                char x = (char) ('a' + col);
                int y = boardRow;
                String key = x + "" + y;
                Button cell = createBoardCell(key);
                boardGrid.add(cell, col, renderRow);
            }
        }
        updateCoordinateLabels();
    }

    private void updateCoordinateLabels() {
        boolean isRed = myColor != null && myColor.equals("red");
        
        if (leftRowLabels != null) {
            for (int i = 0; i < leftRowLabels.getChildren().size(); i++) {
                Label label = (Label) leftRowLabels.getChildren().get(i);
                int rowNum = isRed ? 9 - i : i;
                label.setText(String.valueOf(rowNum));
            }
        }
    }

    private void drawBoardOnCanvas(GraphicsContext gc) {
        boolean isRed = myColor != null && myColor.equals("red");
        
        if (isRed) {
            double centerY = MARGIN + BOARD_HEIGHT / 2;
            gc.save();
            gc.translate(MARGIN + BOARD_WIDTH / 2, centerY);
            gc.scale(1, -1);
            gc.translate(-(MARGIN + BOARD_WIDTH / 2), -centerY);
        }

        gc.setFill(Color.rgb(212, 167, 106));
        gc.fillRect(MARGIN, MARGIN, BOARD_WIDTH, BOARD_HEIGHT);

        gc.setStroke(Color.rgb(93, 58, 26));
        gc.setLineWidth(2);

        for (int i = 0; i <= 9; i++) {
            double y = MARGIN + i * CELL_SIZE;
            gc.strokeLine(MARGIN, y, MARGIN + BOARD_WIDTH, y);
        }

        for (int i = 0; i <= 8; i++) {
            double x = MARGIN + i * CELL_SIZE;
            if (i == 0 || i == 8) {
                gc.strokeLine(x, MARGIN, x, MARGIN + BOARD_HEIGHT);
            } else {
                gc.strokeLine(x, MARGIN, x, MARGIN + 4 * CELL_SIZE);
                gc.strokeLine(x, MARGIN + 5 * CELL_SIZE, x, MARGIN + BOARD_HEIGHT);
            }
        }

        gc.setLineWidth(1.5);
        gc.strokeLine(MARGIN + 3 * CELL_SIZE, MARGIN, MARGIN + 5 * CELL_SIZE, MARGIN + 2 * CELL_SIZE);
        gc.strokeLine(MARGIN + 5 * CELL_SIZE, MARGIN, MARGIN + 3 * CELL_SIZE, MARGIN + 2 * CELL_SIZE);
        gc.strokeLine(MARGIN + 3 * CELL_SIZE, MARGIN + BOARD_HEIGHT, MARGIN + 5 * CELL_SIZE, MARGIN + 7 * CELL_SIZE);
        gc.strokeLine(MARGIN + 5 * CELL_SIZE, MARGIN + BOARD_HEIGHT, MARGIN + 3 * CELL_SIZE, MARGIN + 7 * CELL_SIZE);

        drawRiverDecoration(gc);

        if (isRed) {
            gc.restore();
        }
    }

    private void drawRiverDecoration(GraphicsContext gc) {
        gc.setFill(Color.rgb(93, 58, 26));
        gc.setFont(Font.font("KaiTi", FontWeight.BOLD, 32));
        
        double riverY = MARGIN + 4.5 * CELL_SIZE;
        double halfWidth = BOARD_WIDTH / 2;
        double centerX = MARGIN + halfWidth;
        
        boolean isRed = myColor != null && myColor.equals("red");
        if (isRed) {
            gc.save();
            double centerY = MARGIN + BOARD_HEIGHT / 2;
            gc.translate(MARGIN + BOARD_WIDTH / 2, centerY);
            gc.scale(1, -1);
            gc.translate(-(MARGIN + BOARD_WIDTH / 2), -centerY);
        }
        
        gc.fillText("楚河", centerX - 80, riverY + 12);
        gc.fillText("汉界", centerX + 15, riverY + 12);
        
        if (isRed) {
            gc.restore();
        }
    }

    private Button createBoardCell(String key) {
        Button cell = new Button();
        cell.setPrefSize(CELL_SIZE, CELL_SIZE);
        cell.setUserData(key);
        cell.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        cell.setBorder(new Border(new BorderStroke(Color.TRANSPARENT,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0))));
        cell.setOnAction(e -> handleCellClick(cell));
        return cell;
    }

    
    private void renderBoard() {
        for (javafx.scene.Node node : boardGrid.getChildren()) {
            if (node instanceof Button) {
                Button cell = (Button) node;
                String key = (String) cell.getUserData();

                cell.setText("");
                cell.setGraphic(null);

                StackPane cellPane = new StackPane();
                cellPane.setPrefSize(CELL_SIZE, CELL_SIZE);
                cellPane.setAlignment(Pos.CENTER);

                if (selectedCell != null && selectedCell.equals(key)) {
                    Circle highlight = new Circle(CELL_SIZE / 2 - 2);
                    highlight.setFill(Color.rgb(255, 150, 150, 0.3));
                    cellPane.getChildren().add(highlight);
                } else if (suggestedCells.contains(key)) {
                    Circle highlight = new Circle(CELL_SIZE / 2 - 2);
                    highlight.setFill(Color.rgb(150, 255, 150, 0.3));
                    cellPane.getChildren().add(highlight);

                    Circle moveMark = new Circle(8);
                    if (boardState.containsKey(key)) {
                        moveMark.setFill(Color.rgb(220, 0, 0, 0.6));
                    } else {
                        moveMark.setFill(Color.rgb(0, 180, 0, 0.6));
                    }
                    moveMark.setStroke(Color.WHITE);
                    moveMark.setStrokeWidth(2);
                    cellPane.getChildren().add(moveMark);
                }

                if (boardState.containsKey(key)) {
                    PieceInfo piece = boardState.get(key);
                    StackPane piecePane = createPiece(piece);

                    if (selectedCell != null && selectedCell.equals(key)) {
                        piecePane.setTranslateY(-5);
                    }

                    cellPane.getChildren().add(piecePane);
                }

                cell.setGraphic(cellPane);
            }
        }
    }

    private StackPane createPiece(PieceInfo piece) {
        StackPane piecePane = new StackPane();
        piecePane.setPrefSize(52, 52);
        piecePane.setAlignment(Pos.CENTER);

        Circle pieceCircle = new Circle(24);
        
        if (piece.visible) {
            if (piece.color.equals("red")) {
                pieceCircle.setFill(Color.WHITE);
                pieceCircle.setStroke(Color.rgb(180, 0, 0));
                pieceCircle.setStrokeWidth(3);
            } else {
                pieceCircle.setFill(Color.rgb(255, 223, 0));
                pieceCircle.setStroke(Color.BLACK);
                pieceCircle.setStrokeWidth(3);
            }
        } else {
            if (piece.color.equals("red")) {
                pieceCircle.setFill(Color.rgb(255, 182, 193));
                pieceCircle.setStroke(Color.rgb(180, 0, 0));
                pieceCircle.setStrokeWidth(4);
            } else {
                pieceCircle.setFill(Color.rgb(80, 80, 80));
                pieceCircle.setStroke(Color.BLACK);
                pieceCircle.setStrokeWidth(4);
            }
        }

        pieceCircle.setEffect(new javafx.scene.effect.DropShadow(4, 2, 2, Color.rgb(0, 0, 0, 0.4)));
        piecePane.getChildren().add(pieceCircle);

        Label pieceLabel = new Label();
        if (piece.visible) {
            Map<String, String> symbols = piece.color.equals("red") ? RED_SYMBOLS : BLACK_SYMBOLS;
            pieceLabel.setText(symbols.getOrDefault(piece.type, piece.type));
        } else {
            pieceLabel.setText("?");
        }

        pieceLabel.setFont(Font.font("KaiTi", FontWeight.BOLD, 30));
        
        if (piece.color.equals("red") && piece.visible) {
            pieceLabel.setTextFill(Color.rgb(180, 0, 0));
        } else if (piece.color.equals("black") && piece.visible) {
            pieceLabel.setTextFill(Color.BLACK);
        } else if (piece.color.equals("red") && !piece.visible) {
            pieceLabel.setTextFill(Color.rgb(180, 0, 0));
        } else {
            pieceLabel.setTextFill(Color.WHITE);
        }

        piecePane.getChildren().add(pieceLabel);
        return piecePane;
    }

    private void handleCellClick(Button cell) {
        if (!gameStatus.equals("playing") || !currentTurn.equals(myColor)) {
            return;
        }

        String key = (String) cell.getUserData();

        if (selectedCell == null) {
            if (boardState.containsKey(key)) {
                PieceInfo piece = boardState.get(key);
                if (piece.color.equals(myColor)) {
                    selectedCell = key;
                    calculateSuggestedCells(key, piece);
                    renderBoard();
                }
            }
            return;
        }

        if (selectedCell.equals(key)) {
            selectedCell = null;
            suggestedCells.clear();
            renderBoard();
            return;
        }

        if (!suggestedCells.contains(key)) {
            return;
        }

        String fromX = selectedCell.substring(0, 1);
        int fromY = Integer.parseInt(selectedCell.substring(1));
        String toX = key.substring(0, 1);
        int toY = Integer.parseInt(key.substring(1));

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("fromX", fromX);
        payload.put("fromY", fromY);
        payload.put("toX", toX);
        payload.put("toY", toY);
        payload.put("isFlip", true);
        webSocketClient.sendMessage("move", payload);

        selectedCell = null;
        suggestedCells.clear();
        renderBoard();
    }

    private void calculateSuggestedCells(String key, PieceInfo piece) {
        suggestedCells.clear();
        int x = key.charAt(0) - 'a';
        int y = Integer.parseInt(key.substring(1));

        String pType = piece.visible ? piece.type : "PAWN";

        switch (pType) {
            case "KING":
                suggestKingMoves(x, y, piece.color);
                break;
            case "PAWN":
                suggestPawnMoves(x, y, piece.color);
                break;
            case "KNIGHT":
                suggestKnightMoves(x, y, piece.color);
                break;
            case "BISHOP":
                suggestBishopMoves(x, y, piece);
                break;
            case "GUARD":
                suggestGuardMoves(x, y, piece);
                break;
            case "ROOK":
                suggestRookMoves(x, y, piece.color);
                break;
            case "CANNON":
                suggestCannonMoves(x, y, piece.color);
                break;
        }
    }

    private void suggestKingMoves(int x, int y, String color) {
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int minX = 3, maxX = 5;
        int minY, maxY;
        if (color.equals("red")) {
            minY = 0;
            maxY = 2;
        } else {
            minY = 7;
            maxY = 9;
        }
        for (int[] offset : offsets) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (nx >= minX && nx <= maxX && ny >= minY && ny <= maxY) {
                markCellIfMovable(nx, ny, color);
            }
        }
    }

    private void suggestPawnMoves(int x, int y, String color) {
        int forwardDy = color.equals("red") ? 1 : -1;
        markCellIfMovable(x, y + forwardDy, color);

        boolean crossedRiver = color.equals("red") ? y >= 5 : y <= 4;
        if (crossedRiver) {
            markCellIfMovable(x - 1, y, color);
            markCellIfMovable(x + 1, y, color);
        }
    }

    private void suggestKnightMoves(int x, int y, String color) {
        int[][] moves = {
                {1, 2, 0, 1}, {-1, 2, 0, 1},
                {1, -2, 0, -1}, {-1, -2, 0, -1},
                {2, 1, 1, 0}, {2, -1, 1, 0},
                {-2, 1, -1, 0}, {-2, -1, -1, 0}
        };
        for (int[] move : moves) {
            int nx = x + move[0];
            int ny = y + move[1];
            int legX = x + move[2];
            int legY = y + move[3];
            if (nx >= 0 && nx <= 8 && ny >= 0 && ny <= 9) {
                if (isEmptyCell(legX, legY)) {
                    markCellIfMovable(nx, ny, color);
                }
            }
        }
    }

    private void suggestBishopMoves(int x, int y, PieceInfo piece) {
        int[][] moves = {{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};
        for (int[] move : moves) {
            int nx = x + move[0];
            int ny = y + move[1];
            int eyeX = x + move[0] / 2;
            int eyeY = y + move[1] / 2;
            if (nx >= 0 && nx <= 8 && ny >= 0 && ny <= 9) {
                if (piece.visible) {
                    boolean crossesRiver = piece.color.equals("red") ? ny >= 5 : ny <= 4;
                    if (crossesRiver) {
                        continue;
                    }
                }
                if (isEmptyCell(eyeX, eyeY)) {
                    markCellIfMovable(nx, ny, piece.color);
                }
            }
        }
    }

    private void suggestGuardMoves(int x, int y, PieceInfo piece) {
        int[][] moves = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        int minX = 3, maxX = 5;
        int minY, maxY;
        if (piece.color.equals("red")) {
            minY = 0;
            maxY = 2;
        } else {
            minY = 7;
            maxY = 9;
        }
        for (int[] move : moves) {
            int nx = x + move[0];
            int ny = y + move[1];
            if (nx >= minX && nx <= maxX && ny >= minY && ny <= maxY) {
                markCellIfMovable(nx, ny, piece.color);
            }
        }
    }

    private void suggestRookMoves(int x, int y, String color) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            while (nx >= 0 && nx <= 8 && ny >= 0 && ny <= 9) {
                if (isEmptyCell(nx, ny)) {
                    markCellIfMovable(nx, ny, color);
                } else {
                    if (!isOwnPiece(nx, ny, color)) {
                        markCellIfMovable(nx, ny, color);
                    }
                    break;
                }
                nx += dir[0];
                ny += dir[1];
            }
        }
    }

    private void suggestCannonMoves(int x, int y, String color) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            boolean foundPlatform = false;
            while (nx >= 0 && nx <= 8 && ny >= 0 && ny <= 9) {
                if (isEmptyCell(nx, ny)) {
                    if (!foundPlatform) {
                        markCellIfMovable(nx, ny, color);
                    }
                } else {
                    if (!foundPlatform) {
                        foundPlatform = true;
                    } else {
                        if (!isOwnPiece(nx, ny, color)) {
                            markCellIfMovable(nx, ny, color);
                        }
                        break;
                    }
                }
                nx += dir[0];
                ny += dir[1];
            }
        }
    }

    private boolean isEmptyCell(int x, int y) {
        if (x < 0 || x > 8 || y < 0 || y > 9) {
            return false;
        }
        String key = (char) ('a' + x) + "" + y;
        return !boardState.containsKey(key);
    }

    private boolean isOwnPiece(int x, int y, String color) {
        if (x < 0 || x > 8 || y < 0 || y > 9) {
            return false;
        }
        String key = (char) ('a' + x) + "" + y;
        PieceInfo p = boardState.get(key);
        return p != null && p.color.equals(color);
    }

    private void markCellIfMovable(int nx, int ny, String myColor) {
        if (nx >= 0 && nx <= 8 && ny >= 0 && ny <= 9) {
            String key = (char) ('a' + nx) + "" + ny;
            if (!boardState.containsKey(key) || !boardState.get(key).color.equals(myColor)) {
                suggestedCells.add(key);
            }
        }
    }

    private void addMoveLog(String from, String to, String flipResult, String capturedPiece) {
        String text = from + " -> " + to;

        if (flipResult != null) {
            Map<String, String> symbols = currentTurn.equals("red") ? RED_SYMBOLS : BLACK_SYMBOLS;
            text += " (翻出: " + symbols.getOrDefault(flipResult, flipResult) + ")";
        }

        if (capturedPiece != null && !capturedPiece.equals("NULL") && !capturedPiece.equals("null")) {
            Map<String, String> symbols = currentTurn.equals("red") ? BLACK_SYMBOLS : RED_SYMBOLS;
            text += " (吃: " + symbols.getOrDefault(capturedPiece, capturedPiece) + ")";
        }

        Label label = new Label(text);
        label.setFont(Font.font("Monospaced", 12));
        label.setStyle("-fx-padding: 3 0; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");
        moveLogBox.getChildren().add(0, label);
    }

    private void addCapturedPiece(PieceInfo piece, String capturedPieceType) {
        Label label = new Label();
        label.setPrefSize(26, 26);
        label.setAlignment(Pos.CENTER);

        String displayType = (capturedPieceType != null && !capturedPieceType.equals("NULL") && !capturedPieceType.equals("null")) 
                ? capturedPieceType 
                : piece.type;
        
        boolean isVisible = piece.visible || (capturedPieceType != null && !capturedPieceType.equals("NULL") && !capturedPieceType.equals("null"));

        if (isVisible) {
            Map<String, String> symbols = piece.color.equals("red") ? RED_SYMBOLS : BLACK_SYMBOLS;
            label.setText(symbols.getOrDefault(displayType, displayType));
        } else {
            label.setText("?");
        }

        label.setFont(Font.font("KaiTi", FontWeight.BOLD, 14));

        if (piece.color.equals("red")) {
            if (isVisible) {
                label.setTextFill(Color.rgb(180, 0, 0));
                label.setStyle("-fx-background-color: linear-gradient(to bottom, #fff8dc, #f5deb3); " +
                        "-fx-background-radius: 50%; -fx-border-color: #8b4513; -fx-border-width: 1px;");
            } else {
                label.setTextFill(Color.rgb(180, 0, 0));
                label.setStyle("-fx-background-color: linear-gradient(to bottom, #ffb6c1, #ff69b4); " +
                        "-fx-background-radius: 50%; -fx-border-color: #c62828; -fx-border-width: 2px;");
            }
        } else {
            if (isVisible) {
                label.setTextFill(Color.BLACK);
                label.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #dcdcdc); " +
                        "-fx-background-radius: 50%; -fx-border-color: #333; -fx-border-width: 1px;");
            } else {
                label.setTextFill(Color.WHITE);
                label.setStyle("-fx-background-color: linear-gradient(to bottom, #616161, #424242); " +
                        "-fx-background-radius: 50%; -fx-border-color: #212121; -fx-border-width: 2px;");
            }
        }

        if (piece.color.equals("red")) {
            capturedRedBox.getChildren().add(label);
        } else {
            capturedBlackBox.getChildren().add(label);
        }
    }

    private void startTimer() {
        stopTimer();
        deadlineTime = System.currentTimeMillis() + 60000;

        timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    long remaining = deadlineTime - System.currentTimeMillis();
                    if (remaining <= 0) {
                        timeLeftLabel.setText("剩余时间: 00:00");
                        stopTimer();
                    } else {
                        int seconds = (int) (remaining / 1000);
                        int mins = seconds / 60;
                        int secs = seconds % 60;
                        timeLeftLabel.setText(String.format("剩余时间: %02d:%02d", mins, secs));

                        if (seconds <= 10) {
                            timeLeftLabel.setTextFill(Color.rgb(220, 0, 0));
                        } else {
                            timeLeftLabel.setTextFill(Color.rgb(229, 57, 53));
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTimer = new java.util.Timer();
        heartbeatTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (webSocketClient != null && webSocketClient.isOpen()) {
                        webSocketClient.sendPing();
                    }
                });
            }
        }, 0, 10000);
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    private void setButtonStates(boolean startMatch, boolean cancelMatch, boolean ready, boolean resign) {
        startMatchButton.setDisable(!startMatch);
        cancelMatchButton.setDisable(!cancelMatch);
        readyButton.setDisable(!ready);
        resignButton.setDisable(!resign);
        if (!ready) {
            readyButton.setText("已准备");
        } else {
            readyButton.setText("准备");
        }
    }

    private String getOpponentName(JsonNode data) {
        if (data.has("opponentNickName")) return data.get("opponentNickName").asText();
        if (data.has("opponentNickname")) return data.get("opponentNickname").asText();
        if (data.has("opponentName")) return data.get("opponentName").asText();
        if (data.has("opponentId")) return data.get("opponentId").asText();
        return "对手";
    }

    private void showGameOverModal(String resultType, String description) {
        if (gameOverStage != null && gameOverStage.isShowing()) {
            gameOverStage.close();
        }

        gameOverStage = new Stage();
        gameOverStage.setTitle("游戏结束");
        gameOverStage.initModality(Modality.APPLICATION_MODAL);
        gameOverStage.setResizable(false);

        VBox modalContent = new VBox(20);
        modalContent.setPadding(new Insets(35));
        modalContent.setAlignment(Pos.CENTER);
        modalContent.setStyle("-fx-background-color: #f7f2e8;");

        Label iconLabel = new Label();
        iconLabel.setFont(Font.font(50));

        Label titleLabel = new Label();
        titleLabel.setFont(Font.font("System Bold", 24));

        Label bodyLabel = new Label(description);
        bodyLabel.setFont(Font.font(14));
        bodyLabel.setWrapText(true);
        bodyLabel.setMaxWidth(300);
        bodyLabel.setStyle("-fx-text-fill: #4a4a4a;");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button rematchButton = new Button("再来一局");
        rematchButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 10 24; -fx-border-radius: 6;");
        rematchButton.setOnAction(e -> handleRematch());

        Button closeButton = new Button("返回大厅");
        closeButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 10 24; -fx-border-radius: 6;");
        closeButton.setOnAction(e -> handleBackToLobby());

        buttonBox.getChildren().addAll(rematchButton, closeButton);

        switch (resultType) {
            case "WIN":
                iconLabel.setText("🏆");
                titleLabel.setText("对局胜利");
                titleLabel.setTextFill(Color.rgb(211, 47, 47));
                break;
            case "LOSE":
                iconLabel.setText("🍂");
                titleLabel.setText("对局失败");
                titleLabel.setTextFill(Color.GRAY);
                break;
            case "DRAW":
                iconLabel.setText("🤝");
                titleLabel.setText("握手言和");
                titleLabel.setTextFill(Color.rgb(139, 69, 19));
                break;
            default:
                iconLabel.setText("🏁");
                titleLabel.setText("游戏结束");
                titleLabel.setTextFill(Color.rgb(139, 69, 19));
        }

        modalContent.getChildren().addAll(iconLabel, titleLabel, bodyLabel, buttonBox);
        Scene scene = new Scene(modalContent, 360, 280);
        scene.setFill(Color.rgb(139, 69, 19));
        gameOverStage.setScene(scene);
        gameOverStage.show();
    }

    private void handleRematch() {
        if (gameOverStage != null) {
            gameOverStage.close();
        }

        roomId = null;
        gameStatus = "waiting";

        capturedRedBox.getChildren().clear();
        capturedBlackBox.getChildren().clear();
        moveLogBox.getChildren().clear();
        boardState.clear();

        renderEmptyBoard();

        readyButton.setDisable(true);
        readyButton.setText("准备");

        gameStatusLabel.setText("重新匹配中...");
        messageLabel.setText("正在寻找新对手...");
        setButtonStates(false, true, false, false);

        webSocketClient.sendMessage("startMatch", null);
    }

    private void handleBackToLobby() {
        if (gameOverStage != null) {
            gameOverStage.close();
        }

        stopHeartbeat();
        stopTimer();

        roomId = null;
        gameStatus = "waiting";

        capturedRedBox.getChildren().clear();
        capturedBlackBox.getChildren().clear();
        moveLogBox.getChildren().clear();
        boardState.clear();

        renderEmptyBoard();

        gameStatusLabel.setText("未开始");
        currentTurnLabel.setText("等待匹配");
        yourColorLabel.setText("你的颜色: 未知");
        timeLeftLabel.setText("剩余时间: --:--");
        messageLabel.setText("点击开始匹配");
        redPlayerLabel.setText("红方: 等待...");
        blackPlayerLabel.setText("黑方: 等待...");

        setButtonStates(true, false, false, false);
    }

    private static class PieceInfo {
        String color;
        String type;
        boolean visible;

        PieceInfo(String color, String type, boolean visible) {
            this.color = color;
            this.type = type;
            this.visible = visible;
        }
    }
}