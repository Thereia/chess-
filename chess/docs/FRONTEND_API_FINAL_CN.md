# 前后端联调接口文档（最终版，按当前代码实现）

这份文档只描述“当前后端代码真实实现出来的接口和运行顺序”。

目的不是和公共接口完全一致，而是保证前端可以按照这份文档直接联调。

如果代码与旧文档、课件、公共接口有冲突，以这份文档和当前服务端代码为准。

## 1. 总体说明

### 1.1 通信方式

- 协议：WebSocket
- 服务端端口：`8887`
- WebSocket 路径：`/`

完整默认地址：

```text
ws://localhost:8887/
```

也可以写成：

```text
ws://localhost:8887
```

当前后端只监听根路径，不使用 `/ws/game` 之类的额外路径。

### 1.2 消息格式

所有消息都是 JSON，对象中必须带 `messageType` 字段。

示例：

```json
{
  "messageType": "startMatch"
}
```

### 1.3 当前版本范围

当前版本已实现并可联调的消息：

- 客户端发服务端
  - `startMatch`
  - `Ready`
  - `move`
  - `Resign`
- 服务端发客户端
  - `matchSuccess`
  - `roomInfo`
  - `gameStart`
  - `moveResult`
  - `timeout`
  - `gameOver`
  - `error`

当前版本不处理的可选消息：

- `ping`
- `pong`
- `cancelMatch`
- `requestFirstHand`
- 登录注册相关消息

客户端如果发送这些未实现消息，服务端会返回 `error`。

## 2. 客户端状态流转

前端建议按下面这套状态机来写。

### 2.1 推荐状态

- `DISCONNECTED`：未连接
- `CONNECTED_IDLE`：已连接，但未开始匹配
- `MATCHING`：已发送 `startMatch`，正在等待第二位玩家
- `PREPARING`：匹配成功，等待双方点击准备
- `PLAYING`：已开局，可走子
- `ENDED`：对局结束

### 2.2 标准时序

```text
1. 客户端建立 WebSocket 连接
2. 客户端发送 startMatch
3. 若当前只有一个人，服务端暂时不回消息，客户端保持 MATCHING
4. 第二位玩家进入后，服务端给双方发送 matchSuccess
5. 双方进入 PREPARING，分别点击“准备”，各自发送 Ready
6. 若只有一方先准备，服务端给另一方发送 roomInfo
7. 当双方都 Ready 后，服务端给双方发送 gameStart
8. 进入 PLAYING，当前轮到谁走，谁就发送 move
9. 服务端返回 moveResult
10. 如果吃将结束，服务端紧接着广播 gameOver
11. 如果有人认输，服务端广播 gameOver
12. 如果有人超时，服务端广播 timeout
13. 客户端进入 ENDED
```

## 3. 客户端发服务端

## 3.1 startMatch

用途：开始匹配。

```json
{
  "messageType": "startMatch"
}
```

前端调用时机：

- WebSocket 已连接
- 用户点击“开始匹配”

服务端行为：

- 如果当前没有等待者：把当前连接记为等待中，不立刻回消息
- 如果当前已有一个等待者，且不是自己：创建房间，并给双方发送 `matchSuccess`
- 当前版本只支持同时一盘对局，房间固定为一个活动房间

前端注意：

- 发送 `startMatch` 后，如果暂时没收到消息，不代表失败，可能只是还在等第二个人
- 前端应自行显示“匹配中”

## 3.2 Ready

用途：匹配成功后，告知服务端“我准备好了”。

```json
{
  "messageType": "Ready"
}
```

前端调用时机：

- 已收到 `matchSuccess`
- 处于准备阶段
- 用户点击“准备”

服务端行为：

- 如果当前只有一方准备：不会给发送者回消息，而是给对手发一条 `roomInfo`
- 当双方都准备后：给双方各发一条 `gameStart`

前端注意：

- `Ready` 不是自动触发的，前端需要显式发送
- 当前版本里，先点击准备的人自己不一定立刻收到回包，这是正常现象
- 如果已经开局后又重复发 `Ready`，服务端会返回 `error`

## 3.3 move

用途：走子。

```json
{
  "messageType": "move",
  "fromX": "a",
  "fromY": 3,
  "toX": "a",
  "toY": 4,
  "isFlip": false
}
```

字段说明：

- `fromX`：起点列，`a` 到 `i`
- `fromY`：起点行，`0` 到 `9`
- `toX`：终点列，`a` 到 `i`
- `toY`：终点行，`0` 到 `9`
- `isFlip`：客户端提示值，服务端不完全信任，最终是否翻子由服务端决定

前端调用时机：

- 已收到 `gameStart`
- 当前处于 `PLAYING`
- 当前轮到自己
- 用户完成一次走子选择

服务端行为：

- 校验是否在房间中
- 校验当前是否处于对局中
- 校验是否轮到当前玩家
- 校验走法是否合法
- 合法则执行走子并广播 `moveResult`
- 若本步直接结束游戏，还会继续发送 `gameOver`
- 成功走子后重置下一手超时计时

前端注意：

- 失败的 `move` 只会回给发送者本人
- 成功的 `moveResult` 会广播给双方
- 当前版本不支持“原地翻子”
- 当前版本不发送 `nextTurn`，前端应根据已知规则和合法走子结果推进回合显示

## 3.4 Resign

用途：认输。

```json
{
  "messageType": "Resign"
}
```

前端调用时机：

- 已开局
- 用户点击认输

服务端行为：

- 当前发送者直接判负
- 服务端给双方发送 `gameOver`

前端注意：

- 如果还没开局就发送 `Resign`，当前后端会返回 `error`

## 4. 服务端发客户端

## 4.1 matchSuccess

用途：匹配成功。

服务端在第二位玩家进入房间时，给双方各发一条。

示例：

```json
{
  "messageType": "matchSuccess",
  "roomId": "room-1",
  "opponentId": "B",
  "opponentNickname": "B"
}
```

字段说明：

- `roomId`：当前固定是 `room-1`
- `opponentId`：对手的 sessionId
- `opponentNickname`：当前版本直接等于对手的 sessionId

前端收到后应该做什么：

- 从 `MATCHING` 切到 `PREPARING`
- 显示匹配成功
- 显示对手信息
- 显示“准备”按钮

## 4.2 roomInfo

用途：准备阶段的房间状态通知。

示例：

```json
{
  "messageType": "roomInfo",
  "opponentReady": true
}
```

当前实现语义：

- 这条消息只在准备阶段使用
- 当前实现只会在“对手已经点击 Ready”时发给你
- 当前版本出现这条消息时，`opponentReady` 实际上就是 `true`

前端收到后应该做什么：

- 保持 `PREPARING`
- 把“对手已准备”标记显示出来

## 4.3 gameStart

用途：双方都准备后，正式开局。

示例：

```json
{
  "messageType": "gameStart",
  "redPlayerId": "A",
  "blackPlayerId": "B",
  "yourColor": "red",
  "firstHand": true,
  "initialBoard": [
    {
      "x": "a",
      "y": 0,
      "color": "RED",
      "piece": "ROOK",
      "visible": false
    }
  ]
}
```

字段说明：

- `redPlayerId`：红方玩家 id
- `blackPlayerId`：黑方玩家 id
- `yourColor`：你的颜色，值为 `red` 或 `black`
- `firstHand`：你是否先手；红方为 `true`，黑方为 `false`
- `initialBoard`：初始棋盘上所有非空格子的列表

`initialBoard` 每项字段：

- `x`：列，`a` 到 `i`
- `y`：行，`0` 到 `9`
- `color`：当前代码实际返回 `RED` / `BLACK` 大写
- `piece`：当前代码实际返回 `KING` / `ROOK` / `KNIGHT` / `CANNON` / `PAWN` / `GUARD` / `BISHOP` 大写
- `visible`：是否已明牌

前端收到后应该做什么：

- 切到 `PLAYING`
- 建立本地棋盘
- 根据 `yourColor` 记录自己的阵营
- 根据 `firstHand` 判断开局是否轮到自己

重要说明：

- 当前代码里将帅开局就是明牌，因此两个王的位置会有 `visible=true`
- 其余初始暗子通常是 `visible=false`

## 4.4 moveResult

用途：返回走子结果。

### 4.4.1 成功示例

```json
{
  "messageType": "moveResult",
  "success": true,
  "move": {
    "fromX": "a",
    "fromY": 3,
    "toX": "a",
    "toY": 4,
    "flip": false
  },
  "flipResult": null,
  "valid": true,
  "code": 0,
  "message": "ok",
  "capturedPiece": null
}
```

### 4.4.2 失败示例

```json
{
  "messageType": "moveResult",
  "success": false,
  "move": {
    "fromX": "a",
    "fromY": 3,
    "toX": "a",
    "toY": 4,
    "flip": false
  },
  "flipResult": null,
  "valid": false,
  "code": 2001,
  "message": "illegal movement",
  "capturedPiece": null
}
```

字段说明：

- `success`：是否执行成功
- `move`：服务端确认后的这一步
- `flipResult`：如果移动的暗子在本步翻开，则这里返回翻出的真实类型；否则为 `null`
- `valid`：当前实现里，成功时是 `true`，失败时是 `false`
- `code`：错误码；成功时固定 `0`
- `message`：说明信息；成功时固定 `"ok"`
- `capturedPiece`：如果本步吃子，则返回被吃棋子的类型；否则为 `null`

重要注意：

- `move` 对象里的布尔字段，序列化后的字段名实际是 `flip`，不是 `isFlip`
- 这是当前代码真实行为，前端解析时必须按 `flip` 读

类型值实际格式：

- `flipResult` / `capturedPiece` 当前代码实际返回大写枚举值
- 可取值：
  - `KING`
  - `ROOK`
  - `KNIGHT`
  - `CANNON`
  - `PAWN`
  - `GUARD`
  - `BISHOP`

前端收到后应该做什么：

- 若 `success=true`：
  - 更新棋盘
  - 如有 `flipResult`，把该移动棋子标成明牌并更新真实类型
  - 如有 `capturedPiece`，更新吃子表现
  - 推进本地回合显示
- 若 `success=false`：
  - 不更新棋盘
  - 保持当前回合
  - 根据 `code` / `message` 提示用户

## 4.5 gameOver

用途：正常终局消息。

当前用于两种情况：

- 认输结束
- 吃将结束

### 4.5.1 认输示例

```json
{
  "messageType": "gameOver",
  "winner": "black",
  "reason": "resign",
  "winnerId": "B"
}
```

### 4.5.2 吃将结束示例

```json
{
  "messageType": "gameOver",
  "winner": "red",
  "reason": "checkmate",
  "winnerId": "A"
}
```

### 4.5.3 无吃子和棋示例

```json
{
  "messageType": "gameOver",
  "winner": null,
  "reason": "drawNoCapture",
  "winnerId": null
}
```

字段说明：

- `winner`：赢家颜色，可能为 `red` / `black` / `null`
- `reason`：结束原因
- `winnerId`：赢家玩家 id，和棋时为 `null`

当前代码中的 `reason` 可能值：

- `checkmate`
- `resign`
- `drawNoCapture`

前端收到后应该做什么：

- 切到 `ENDED`
- 弹出终局提示
- 禁止继续发送走子

## 4.6 timeout

用途：超时结束消息。

示例：

```json
{
  "messageType": "timeout",
  "loserId": "A",
  "winnerId": "B",
  "reason": "timeout"
}
```

字段说明：

- `loserId`：超时方 id
- `winnerId`：获胜方 id
- `reason`：固定为 `timeout`

前端收到后应该做什么：

- 切到 `ENDED`
- 提示超时结束
- 禁止继续走子

注意：

- 当前代码在超时时只发 `timeout`，不会再补发一条 `gameOver`

## 4.7 error

用途：协议级错误，或者时序错误。

示例：

```json
{
  "messageType": "error",
  "code": 3001,
  "message": "room not found"
}
```

当前常见错误码：

- `3001`：当前玩家不在房间里，却发送了依赖房间的消息
- `4001`：JSON 格式错误
- `4002`：未知 `messageType`
- `5000`：运行时阶段错误，例如：
  - 已开局后又发送 `Ready`
  - 未开局就发送 `Resign`

前端收到后应该做什么：

- 不要直接断开连接
- 根据 `code` / `message` 给提示
- 如果是时序错误，优先检查当前前端状态流转是否写错

## 5. 棋盘与坐标规则

## 5.1 坐标系统

- 列：`a` 到 `i`
- 行：`0` 到 `9`
- `e0` 是红方将帅位置
- `e9` 是黑方将帅位置

服务端统一使用这一套全局坐标。

如果前端要做“黑方视角翻转棋盘”，也必须在发送 `move` 前换回这套全局坐标。

## 5.2 颜色字符串

当前代码里实际会出现两种大小写风格：

- `gameStart.yourColor`、`gameOver.winner`：小写
  - `red`
  - `black`
- `initialBoard[].color`：大写
  - `RED`
  - `BLACK`

前端不要假设所有颜色字段大小写完全一致。

## 5.3 棋子类型字符串

当前代码实际返回大写枚举名：

- `KING`
- `ROOK`
- `KNIGHT`
- `CANNON`
- `PAWN`
- `GUARD`
- `BISHOP`

这些值会出现在：

- `initialBoard[].piece`
- `moveResult.flipResult`
- `moveResult.capturedPiece`

## 6. 前端联调建议

## 6.1 最小接入顺序

建议前端按这个顺序做：

1. 建立 WebSocket 连接
2. 发送 `startMatch`
3. 收到 `matchSuccess` 后显示准备界面
4. 点击准备并发送 `Ready`
5. 收到 `roomInfo` 后显示“对手已准备”
6. 收到 `gameStart` 后初始化棋盘
7. 发 `move`，收 `moveResult`
8. 处理 `gameOver` / `timeout`
9. 处理 `error`

## 6.2 前端不要做的事

- 不要自己决定翻子真实结果
- 不要自己决定吃子后的真实类型显示
- 不要自己判定超时胜负
- 不要在未开局时允许发送 `move` 或 `Resign`
- 不要跳过 `Ready` 阶段

## 6.3 推荐的本地状态字段

前端建议至少维护：

- `socketConnected`
- `phase`
  - `DISCONNECTED`
  - `CONNECTED_IDLE`
  - `MATCHING`
  - `PREPARING`
  - `PLAYING`
  - `ENDED`
- `roomId`
- `selfPlayerId`
- `opponentId`
- `opponentNickname`
- `yourColor`
- `firstHand`
- `opponentReady`
- `board`
- `isYourTurn`
- `lastError`

## 7. 完整时序示例

## 7.1 匹配并开局

```text
客户端A连接
客户端B连接

A -> startMatch
服务端暂不回包

B -> startMatch
服务端 -> A: matchSuccess
服务端 -> B: matchSuccess

A -> Ready
服务端 -> B: roomInfo(opponentReady=true)

B -> Ready
服务端 -> A: gameStart
服务端 -> B: gameStart
```

## 7.2 合法走子

```text
当前走子方 -> move
服务端 -> 双方: moveResult(success=true)
如果未结束，继续下一回合
```

## 7.3 非法走子

```text
当前玩家 -> move
服务端 -> 发送者本人: moveResult(success=false, valid=false, code=...)
```

## 7.4 认输

```text
玩家 -> Resign
服务端 -> 双方: gameOver(reason=resign)
```

## 7.5 超时

```text
服务端内部计时
到时后 -> 双方: timeout
```

## 8. 当前已知实现特征

这些不是“理想设计”，而是当前代码的真实行为，前端必须按它适配：

1. `startMatch` 在只有一个人排队时，不回任何消息。
2. `Ready` 先到的一方，不一定收到自己的回包；通常是另一方收到 `roomInfo`。
3. `moveResult.move` 里的布尔字段名实际是 `flip`，不是 `isFlip`。
4. `initialBoard.color` 是大写 `RED/BLACK`。
5. `piece`、`flipResult`、`capturedPiece` 都是大写枚举名。
6. 超时只发 `timeout`，不再补 `gameOver`。
7. 当前只支持一个活动房间，房间号固定 `room-1`。
8. 对手昵称当前直接等于对手 sessionId。

## 9. 一句话联调结论

前端只要严格遵守这个顺序：

```text
连接 -> startMatch -> matchSuccess -> Ready -> roomInfo -> gameStart -> move -> moveResult -> gameOver/timeout
```

就能和当前后端代码正常联调。
