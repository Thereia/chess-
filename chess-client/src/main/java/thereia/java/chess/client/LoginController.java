package thereia.java.chess.client;

import java.io.IOException;
import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField userIdField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField nickNameField;

    @FXML
    private RadioButton loginRadio;

    @FXML
    private RadioButton registerRadio;

    @FXML
    private ToggleGroup authToggleGroup;

    @FXML
    private Button authButton;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField serverUrlField;

    @FXML
    private Button connectButton;

    private JieqiWebSocketClient webSocketClient;
    private String currentUserId;
    private String currentNickName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        nickNameField.setVisible(false);

        loginRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                authButton.setText("登录");
                nickNameField.setVisible(false);
            }
        });

        registerRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                authButton.setText("注册");
                nickNameField.setVisible(true);
            }
        });

        doConnect();
    }

    @FXML
    void handleConnect(ActionEvent event) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.stopReconnect();
            webSocketClient.close();
            return;
        }
        doConnect();
    }

    private void doConnect() {
        String url = serverUrlField.getText().trim();
        if (url.isEmpty()) {
            showStatus("请输入服务器地址", "error");
            return;
        }

        try {
            URI uri = new URI(url);
            webSocketClient = new JieqiWebSocketClient(uri);

            webSocketClient.setConnectionHandler(connected -> {
                Platform.runLater(() -> {
                    if (connected) {
                        showStatus("已连接到服务器", "success");
                        connectButton.setText("断开连接");
                    } else {
                        showStatus("连接已断开，正在重连...", "error");
                        connectButton.setText("连接服务器");
                    }
                });
            });

            webSocketClient.setMessageHandler(this::handleServerMessage);

            webSocketClient.connect();
            showStatus("正在连接...", "info");

        } catch (Exception e) {
            showStatus("连接失败: " + e.getMessage(), "error");
        }
    }

    @FXML
    void handleAuth(ActionEvent event) {
        if (webSocketClient == null || !webSocketClient.isOpen()) {
            showStatus("请先连接服务器", "error");
            return;
        }

        String userId = userIdField.getText().trim();
        String password = passwordField.getText().trim();

        if (userId.isEmpty() || password.isEmpty()) {
            showStatus("请填写用户名和密码", "error");
            return;
        }

        boolean isRegisterMode = registerRadio.isSelected();
        
        if (isRegisterMode) {
            String nickName = nickNameField.getText().trim();
            if (nickName.isEmpty()) {
                showStatus("请填写昵称", "error");
                return;
            }

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("messageType", "register");
            payload.put("userId", userId);
            payload.put("passWord", password);
            payload.put("nickName", nickName);
            webSocketClient.sendRawMessage(payload.toString());
        } else {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("messageType", "Login");
            payload.put("userId", userId);
            payload.put("passWord", password);
            webSocketClient.sendRawMessage(payload.toString());
        }

        showStatus("正在" + (isRegisterMode ? "注册" : "登录") + "...", "info");
    }

    private void handleServerMessage(JsonNode message) {
        Platform.runLater(() -> {
            try {
                String messageType = message.get("messageType").asText();

                switch (messageType) {
                    case "loginResult":
                        handleLoginResult(message);
                        break;
                    case "error":
                        String errorMsg = message.has("message") ? message.get("message").asText() :
                                         (message.has("reason") ? message.get("reason").asText() : "未知错误");
                        showStatus("错误: " + errorMsg, "error");
                        break;
                    default:
                        showStatus("未知消息: " + messageType, "info");
                        break;
                }
            } catch (Exception e) {
                showStatus("处理消息失败: " + e.getMessage(), "error");
            }
        });
    }

    private void handleLoginResult(JsonNode result) {
        boolean success = result.get("success").asBoolean();

        if (success) {
            currentUserId = result.get("userId").asText();
            currentNickName = result.get("nickName").asText();
            showStatus("登录成功！欢迎, " + currentNickName, "success");

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GameView.fxml"));
                Parent root = loader.load();

                GameController controller = loader.getController();
                controller.init(webSocketClient, currentUserId, currentNickName);

                Stage stage = (Stage) userIdField.getScene().getWindow();
                stage.setScene(new Scene(root, 900, 700));
                stage.setResizable(true);
            } catch (IOException e) {
                showStatus("加载游戏界面失败: " + e.getMessage(), "error");
                webSocketClient.setMessageHandler(this::handleServerMessage);
            } catch (Exception e) {
                showStatus("初始化游戏界面失败: " + e.getMessage(), "error");
                webSocketClient.setMessageHandler(this::handleServerMessage);
            }
        } else {
            String reason = result.has("reason") ? result.get("reason").asText() : "登录失败";
            showStatus(reason, "error");
        }
    }

    private void showStatus(String text, String type) {
        statusLabel.setText(text);
        switch (type) {
            case "success":
                statusLabel.setStyle("-fx-text-fill: #4caf50;");
                break;
            case "error":
                statusLabel.setStyle("-fx-text-fill: #f44336;");
                break;
            default:
                statusLabel.setStyle("-fx-text-fill: #666;");
        }
    }
}
