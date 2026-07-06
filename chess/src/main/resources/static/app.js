const PIECE_SYMBOLS = {
    KING: '帅',
    ROOK: '车',
    KNIGHT: '马',
    CANNON: '炮',
    PAWN: '兵',
    GUARD: '仕',
    BISHOP: '相'
};

const BLACK_SYMBOLS = {
    KING: '将',
    ROOK: '车',
    KNIGHT: '马',
    CANNON: '炮',
    PAWN: '卒',
    GUARD: '士',
    BISHOP: '象'
};

let socket = null;
let selectedCell = null;
let boardState = {};
let myColor = null;
let currentTurn = null;
let gameStatus = 'waiting';
let opponentReady = false;
let myReady = false;
let timerInterval = null;
let deadlineTime = null;
let heartbeatInterval = null;
let loggedInUser = null;
let roomId = null;
let capturedRedPieces = [];
let capturedBlackPieces = [];
let currentMyName = '';
let currentOpponentName = '';

const loginScreen = document.getElementById('loginScreen');
const gameScreen = document.getElementById('gameScreen');
const board = document.getElementById('board');
const statusLine = document.getElementById('gameStatus');
const currentTurnEl = document.getElementById('currentTurn');
const yourColorEl = document.getElementById('yourColor');
const timeLeftEl = document.getElementById('timeLeft');
const messageEl = document.getElementById('message');
const moveLogEl = document.getElementById('moveLog');
const redPlayerEl = document.getElementById('redPlayer');
const blackPlayerEl = document.getElementById('blackPlayer');
const usernameDisplay = document.getElementById('usernameDisplay');

const gameOverModal = document.getElementById('gameOverModal');
const modalIcon = document.getElementById('modalIcon');
const modalTitle = document.getElementById('modalTitle');
const modalBody = document.getElementById('modalBody');
const modalRematchButton = document.getElementById('modalRematchButton');
const modalCloseButton = document.getElementById('modalCloseButton');

if(modalCloseButton){
modalCloseButton.addEventListener('click', () => {
    gameOverModal.classList.remove('active');
});
}

if(modalRematchButton){
modalRematchButton.addEventListener('click', () => {
    gameOverModal.classList.remove('active'); // 先关闭弹窗
	send({ messageType: 'Resign' });
	roomId = null;
	gameStatus = 'waiting';
	myReady = false;
    opponentReady = false;
    selectedCell = null;
    boardState = {};
	capturedRedPieces = [];
	capturedBlackPieces = [];
	
	const redPool = document.getElementById('capturedRedPieces');
    const blackPool = document.getElementById('capturedBlackPieces');
    if (redPool) redPool.innerHTML = '';
    if (blackPool) blackPool.innerHTML = '';

	moveLogEl.innerHTML = '';
	    board.innerHTML = '';
	    renderEmptyBoard();
		
	const readyBtn = document.getElementById('readyButton');
	    if (readyBtn) {
	        readyBtn.disabled = true; // 匹配中时，准备按钮应当是禁用状态
	        readyBtn.textContent = '准备';
	    }
	
	setTimeout(() =>{
	send({ 
	        messageType: 'startMatch',
	        roomId: null 
	    }); 
	    
	    statusLine.textContent = '重新匹配中...';
	    // 激活取消匹配按钮，禁用开始匹配按钮
	    setButtonStates({ startMatch: false, cancelMatch: true });
		}, 100);
});
}

document.getElementById('tabLogin').addEventListener('click', () => {
    document.getElementById('tabLogin').classList.add('active');
    document.getElementById('tabRegister').classList.remove('active');
    document.getElementById('authButton').textContent = '登录';
    document.getElementById('nickName').style.display = 'none';
});

document.getElementById('tabRegister').addEventListener('click', () => {
    document.getElementById('tabRegister').classList.add('active');
    document.getElementById('tabLogin').classList.remove('active');
    document.getElementById('authButton').textContent = '注册';
    document.getElementById('nickName').style.display = 'block';
});

document.getElementById('authButton').addEventListener('click', () => {
    const userId = document.getElementById('userId').value.trim();
    const passWord = document.getElementById('passWord').value;
    const nickName = document.getElementById('nickName').value.trim();

    if (!userId || !passWord) {
        showLoginStatus('请填写用户名和密码', 'error');
        return;
    }

    const isRegister = document.getElementById('tabRegister').classList.contains('active');
    if (isRegister && !nickName) {
        showLoginStatus('请填写昵称', 'error');
        return;
    }

    connectAndAuth(userId, passWord, nickName, isRegister);
});

document.getElementById('connectButton').addEventListener('click', () => {
    connectWebSocket();
});

document.getElementById('startMatchButton').addEventListener('click', () => {
    send({ messageType: 'startMatch' });
    statusLine.textContent = '匹配中...';
    setButtonStates({ startMatch: false, cancelMatch: true });
});

document.getElementById('cancelMatchButton').addEventListener('click', () => {
    send({ messageType: 'cancelMatch' });
    statusLine.textContent = '已取消匹配';
    setButtonStates({ startMatch: true, cancelMatch: false });
});

document.getElementById('readyButton').addEventListener('click', () => {
    send({ messageType: 'Ready' });
    myReady = true;
    document.getElementById('readyButton').disabled = true;
    document.getElementById('readyButton').textContent = '已准备';
    messageEl.textContent = '等待对手准备...';
});

document.getElementById('resignButton').addEventListener('click', () => {
    if (confirm('确定要认输吗？')) {
        send({ messageType: 'Resign' });
    }
});

document.getElementById('logoutButton').addEventListener('click', () => {
    logout();
});

function connectAndAuth(userId, passWord, nickName, isRegister) {
    connectWebSocket(() => {
        const message = isRegister
            ? { messageType: 'register', userId, passWord, nickName }
            : { messageType: 'Login', userId, passWord };
        send(message);
    });
}

function connectWebSocket(onOpenCallback) {
    const wsUrl = document.getElementById('wsUrl').value;

    if (socket) {
        socket.close();
    }

    socket = new WebSocket(wsUrl);
    socket.addEventListener('open', () => {
        showLoginStatus('已连接到服务器', 'success');
        if (onOpenCallback) {
            onOpenCallback();
        }
    });

    socket.addEventListener('message', (event) => {
        try {
            const data = JSON.parse(event.data);
            handleMessage(data);
        } catch (e) {
            console.error('Invalid JSON:', event.data);
        }
    });

    socket.addEventListener('close', () => {
        showLoginStatus('连接已断开', 'error');
        clearTimers();
        setButtonStates({ startMatch: false, cancelMatch: false, ready: false, resign: false });
    });

    socket.addEventListener('error', () => {
        showLoginStatus('连接失败', 'error');
    });
}

function send(payload) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
        showLoginStatus('连接未建立', 'error');
        return;
    }
    socket.send(JSON.stringify(payload));
}

function handleMessage(data) {
    switch (data.messageType) {
        case 'loginResult':
            handleLoginResult(data);
            break;
        case 'matchSuccess':
            handleMatchSuccess(data);
            break;
        case 'roomInfo':
            handleRoomInfo(data);
            break;
        case 'gameStart':
            handleGameStart(data);
            break;
        case 'moveResult':
            handleMoveResult(data);
            break;
        case 'timeout':
            handleTimeout(data);
            break;
        case 'gameOver':
            handleGameOver(data);
            break;
        case 'pong':
            break;
        case 'error':
            handleError(data);
            break;
        default:
            console.log('Unknown message:', data);
    }
}

function handleLoginResult(data) {
    if (data.success) {
        loggedInUser = { userId: data.userId, nickName: data.nickName };
        showLoginStatus(`登录成功！欢迎, ${data.nickName}`, 'success');
		if (usernameDisplay) {
            usernameDisplay.textContent = `当前用户: ${data.nickName} (${data.userId})`;
		}
		
		setTimeout(() => {
            loginScreen.classList.remove('active');
            gameScreen.classList.add('active');
            startHeartbeat();
            setButtonStates({ startMatch: true });
        }, 1000);
    } else {
        showLoginStatus(data.message || '登录失败', 'error');
    }
}

function handleMatchSuccess(data) {
    roomId = data.roomId;
	
	
	currentMyName = loggedInUser.nickName;
	currentOpponentName = data.opponentNickName || data.opponentNickname || data.opponentName || data.opponentId || '对手';
	
    statusLine.textContent = '匹配成功！';
    currentTurnEl.textContent = '等待双方准备';
    messageEl.textContent = `对手: ${currentOpponentName}，请点击"准备"`;
	
	
	if (redPlayerEl) redPlayerEl.textContent = `红方: 准备中...`;
    if (blackPlayerEl) blackPlayerEl.textContent = `黑方: 准备中...`;
	
	

    gameStatus = 'preparing';
    setButtonStates({ startMatch: false, cancelMatch: false, ready: true });
}

function handleRoomInfo(data) {
    opponentReady = true;
    messageEl.textContent = '对手已准备，请点击"准备"';
}

function handleGameStart(data) {
    myColor = data.yourColor ? data.yourColor.toLowerCase() : 'red';
    currentTurn = 'red';
    gameStatus = 'playing';

    yourColorEl.textContent = `你的颜色: ${myColor === 'red' ? '红方' : '黑方'}`;
    statusLine.textContent = '游戏开始！';
    currentTurnEl.textContent = `当前回合: ${currentTurn === 'red' ? '红方' : '黑方'}`;

    setButtonStates({ ready: false, resign: true });

	if (data.opponentNickName || data.opponentNickname || data.opponentName) {
	        currentOpponentName = data.opponentNickName || data.opponentNickname || data.opponentName;
	    }
	
	if(myColor === 'red'){
	if (redPlayerEl) redPlayerEl.textContent = `红方: ${currentMyName}`;
	if (blackPlayerEl) blackPlayerEl.textContent = `黑方: ${currentOpponentName}`;
	}
	else{
		if (redPlayerEl) redPlayerEl.textContent = `红方: ${currentOpponentName}`;
		        if (blackPlayerEl) blackPlayerEl.textContent = `黑方: ${currentMyName}`;
	}
	
	messageEl.textContent = currentTurn === myColor ? '轮到你走棋' : '等待对手走棋';
	updateTurnIndicator();
    renderBoard(data.initialBoard);
    startTimer();
}

function handleMoveResult(data) {
    if (!data.success) {
        messageEl.textContent = `错误: ${data.message || '走子无效'}`;
        setTimeout(() => {
            if (gameStatus === 'playing') {
                messageEl.textContent = currentTurn === myColor ? '轮到你走棋' : '等待对手走棋';
            }
        }, 2000);
        return;
    }

    const move = data.move;
    const from = `${move.fromX}${move.fromY}`;
    const to = `${move.toX}${move.toY}`;

	if (boardState[to]) {
		const targetY = parseInt(move.toY);
	    let victimColor = boardState[to].color ? boardState[to].color.toLowerCase() : (targetY >= 5 ? 'black' : 'red');
		const isTargetHidden = (boardState[to].visible === false || boardState[to].type === 'reverse');
		let finalType = data.capturedPiece || (data.move && data.move.capturedPiece) || move.capturedPiece || data.piece;
		if (!finalType || finalType === 'reverse'){
			if(!isTargetHidden) finalType = boardState[to].type;
			else finalType = 'HIDDEN_CAPTURED';
		}
		
		if(finalType){
			const poolType = finalType === 'HIDDEN_CAPTURED' ? 'HIDDEN_CAPTURED' : finalType.toUpperCase();
		    if (victimColor === 'red') {
		        capturedRedPieces.push(poolType);
	        } else if (victimColor === 'black'){
                capturedBlackPieces.push(poolType);
			}
        }
		let hasRedKing = false;
		    let hasBlackKing = false;
		    for (const coord in boardState) {
		        const p = boardState[coord];
		        if (p.visible) {
		            if (p.type === 'KING' && p.color === 'red') hasRedKing = true;
		            if (p.type === 'KING' && p.color === 'black') hasBlackKing = true;
		        }
		    }

		    // 如果发现有任何一方的将帅不在了，且游戏还在进行中
		    if (gameStatus === 'playing') {
		        if (!hasRedKing || !hasBlackKing) {
		            gameStatus = 'ended';
		            clearTimers();
		            setButtonStates({ resign: false });
		            
		            // 判断当前玩家的输赢
		            const amIRed = (myColor === 'red');
		            const isWin = (!hasBlackKing && amIRed) || (!hasRedKing && !amIRed);
		            
		            if (isWin) {
		                messageEl.textContent = '恭喜你获胜！';
		                showGameOverModal('WIN', '你成功击杀了敌方首领，赢得了胜利！');
		            } else {
		                messageEl.textContent = '你输了';
		                showGameOverModal('LOSE', '您的将帅已被击杀，遗憾败北！');
		            }
		        }
		    }
		renderCapturedPieces();
		delete boardState[to];
	}
    
	updateBoardState(from, to, data.flipResult);

    addMoveLog(move, data.flipResult, data.capturedPiece);

    if (data.currentTurn) {
        currentTurn = data.currentTurn.toLowerCase();
        currentTurnEl.textContent = `当前回合: ${currentTurn === 'red' ? '红方' : '黑方'}`;
        messageEl.textContent = currentTurn === myColor ? '轮到你走棋' : '等待对手走棋';
        updateTurnIndicator();
        startTimer();
    }

    renderBoardFromState();
}

function handleTimeout(data) {
    statusLine.textContent = '超时！';
    messageEl.textContent = data.loserId === loggedInUser.userId ? '你超时了，游戏结束' : '对手超时，你获胜！';
    gameStatus = 'ended';
    clearTimers();
    setButtonStates({ resign: false });
	
	if (data.loserId === loggedInUser.userId) {
		showGameOverModal('LOSE', '由于你思索时间过长，遗憾超时输掉了对局。');
		messageEl.textContent = '你超时了，游戏结束';
    } else {
		showGameOverModal('WIN', '由于对手思索超时，恭喜你获得了本局的胜利！');
		messageEl.textContent = '对手超时，你获胜！';
    }
}

function showGameOverModal(resultType, description) {
	if (!gameOverModal) return;
	// 根据不同的对局结果类型，更换对应的图标与标题颜色
    switch (resultType) {
        case 'WIN':
            modalIcon.textContent = '🏆';
            modalTitle.textContent = '对局胜利';
            modalTitle.style.color = '#d32f2f'; // 鲜艳红
            break;
        case 'LOSE':
            modalIcon.textContent = '🍂';
            modalTitle.textContent = '对局失败';
            modalTitle.style.color = '#757575'; // 失败灰
            break;
        case 'DRAW':
            modalIcon.textContent = '🤝';
            modalTitle.textContent = '握手言和';
            modalTitle.style.color = '#8b4513'; // 经典棕
            break;
        case 'ABORT':
            modalIcon.textContent = '🔌';
            modalTitle.textContent = '对局中断';
            modalTitle.style.color = '#e65100'; // 警示橙
            break;
        default:
            modalIcon.textContent = '🏁';
            modalTitle.textContent = '游戏结束';
            modalTitle.style.color = '#8b4513';
    }

    modalBody.textContent = description;
    gameOverModal.classList.add('active'); // 激活弹窗
}

function handleGameOver(data) {
    statusLine.textContent = '游戏结束';
	gameStatus = 'ended';
	clearTimers();
    setButtonStates({ resign: false });

    if (!data.winner) {
        messageEl.textContent = '和棋！';
		showGameOverModal('DRAW', '双方握手言和，本局平局。');
    } else if (data.winnerId === loggedInUser.userId) {
        messageEl.textContent = '恭喜你获胜！';
		showGameOverModal('WIN', '你成功击败了对手，赢得了本局胜利！');
    } else {
        messageEl.textContent = '你输了';
		showGameOverModal('LOSE', '棋差一招，遗憾败北，再接再厉！');
    }

    gameStatus = 'ended';
    clearTimers();
    setButtonStates({ resign: false });
}

function handleError(data) {
    console.error('Error:', data);
    messageEl.textContent = `错误: ${data.message}`;
}

function renderBoard(initialBoard) {
	boardState = {};
	capturedRedPieces = [];
	capturedBlackPieces = [];
	const redPool = document.getElementById('capturedRedPieces');
	const blackPool = document.getElementById('capturedBlackPieces');
	if (redPool) redPool.innerHTML = '';
    if (blackPool) blackPool.innerHTML = '';
    board.replaceChildren();

    for (let y = 9; y >= 0; y--) {
        // 创建正常的作战格子
        for (let col = 0; col < 9; col++) {
            const x = String.fromCharCode('a'.charCodeAt(0) + col);
            const cell = document.createElement('div');
            cell.className = 'cell';
            cell.dataset.x = x;
            cell.dataset.y = String(y);
            cell.dataset.coord = `${x}${y}`;
            cell.addEventListener('click', () => handleCellClick(cell));
            board.appendChild(cell);
        }

        // 同样在 y=5 行后完美插入 2 行河道格
        if (y === 5) {
            for (let r = 0; r < 2; r++) {
                for (let col = 0; col < 9; col++) {
                    const riverCell = document.createElement('div');
                    riverCell.className = 'cell river-cell';
                    riverCell.dataset.riverRow = String(r);
                    riverCell.dataset.riverCol = String(col);
                    board.appendChild(riverCell);
                }
            }
        }
    }

    initialBoard.forEach(piece => {
        const key = `${piece.x}${piece.y}`;
        boardState[key] = {
            color: piece.color.toLowerCase(),
            type: piece.piece,
            visible: piece.visible
        };
    });

    const boardContainer = document.querySelector('.board-container');
    if (myColor === 'black') {
        board.classList.add('flip-180');
        boardContainer.classList.add('flip-180');
    } else {
        board.classList.remove('flip-180');
        boardContainer.classList.remove('flip-180');
    }

    renderBoardFromState();
}

function renderBoardFromState() {
    // 只清理有坐标的作战格子，绝对不能清掉或改变 .river-cell 的结构
    const cells = board.querySelectorAll('.cell[data-coord]');
    cells.forEach(cell => {
        cell.replaceChildren();
        cell.classList.remove('selected', 'suggested');
    });

    // 重新把棋子放上棋盘
    for (const coord in boardState) {
        const p = boardState[coord];
        const cell = board.querySelector(`.cell[data-coord="${coord}"]`);
        if (cell) {
            const pieceEl = document.createElement('div');
            pieceEl.className = `piece ${p.color}`;
            
			
			
            if (p.visible) {
                const symbols = p.color === 'red' ? PIECE_SYMBOLS : BLACK_SYMBOLS;
                pieceEl.textContent = symbols[p.type] || p.type;
            } else {
                pieceEl.classList.add('hidden-piece');
                pieceEl.textContent = '?'; 
            }
            cell.appendChild(pieceEl);
        }
    }
}

function updateBoardState(from, to, flipResult) {
    const piece = boardState[from];
    if (piece) {
        boardState[to] = {
            color: piece.color,
            type: flipResult || piece.type,
            visible: true
        };
        delete boardState[from];
    }
}

function handleCellClick(cell) {
    if (gameStatus !== 'playing' || currentTurn !== myColor) {
        return;
    }
	
	board.querySelectorAll('.cell.suggested').forEach(c => c.classList.remove('suggested'));

    if (!selectedCell) {
        const key = `${cell.dataset.x}${cell.dataset.y}`;
        const piece = boardState[key];

        if (piece && piece.color === myColor) {
            selectedCell = cell;
            cell.classList.add('selected');
			
			const currentXChar = cell.dataset.x;
			const currentX = currentXChar.charCodeAt(0) - 97;
			const currentY = Number(cell.dataset.y);
			
			const markCell = (nx, ny) =>{
				if (nx >= 0 && nx <= 8 && ny >= 0 && ny <= 9){
					const targetXStr = String.fromCharCode(97 + nx);
					const targetCell = board.querySelector(`.cell[data-coord="${targetXStr}${ny}"]`);
					if (targetCell) targetCell.classList.add('suggested');
				}
			};
			const pType = piece.visible ? piece.type : 'PAWN';
			switch (pType){
				case 'KING':
					markCell(currentX + 1, currentY);
					markCell(currentX, currentY);
					markCell(currentX, currentY + 1);
					markCell(currentX, currentY - 1);
					break;
					
				case 'PAWN':
					if (piece.color === 'red') {
					    markCell(currentX, currentY + 1); // 红兵向前
	                    if (currentY >= 5) { // 已过河
			                markCell(currentX - 1, currentY);
                            markCell(currentX + 1, currentY);
	                    }
				    } else {
	                        markCell(currentX, currentY - 1); // 黑卒向前
			                if (currentY <= 4) { // 已过河
			                    markCell(currentX - 1, currentY);
			                    markCell(currentX + 1, currentY);
		                    }
		            }
	                break;
				
				case 'KNIGHT':
					const knightOffsets = [
					    [-1, -2], [1, -2], [-2, -1], [2, -1],
	                    [-2, 1], [2, 1], [-1, 2], [1, 2]
		            ];
					knightOffsets.forEach(([dx, dy]) => markCell(currentX + dx, currentY + dy));
					break;
					
				case 'BISHOP':
					markCell(currentX + 2, currentY + 2);
					markCell(currentX + 2, currentY - 2);
					markCell(currentX - 2, currentY + 2);
					markCell(currentX - 2, currentY - 2);
					break;
					
				case 'GUARD':
					markCell(currentX + 1, currentY + 1);
					markCell(currentX + 1, currentY - 1);
					markCell(currentX - 1, currentY + 1);
					markCell(currentX - 1, currentY - 1);
					break;
				
				case 'ROOK':
					
				case 'CANNON':
					for (let i = 0; i < 9; i++){
						if (i !== currentX) markCell(i, currentY);
					}
					for (let j = 0; j < 10; j++){
						if (j !== currentY) markCell(currentX, j);
					}
					break;
			}
        }
        return;
    }

    if (selectedCell === cell) {
        selectedCell.classList.remove('selected');
        selectedCell = null;
        return;
    }

    const move = {
        messageType: 'move',
        fromX: selectedCell.dataset.x,
        fromY: Number(selectedCell.dataset.y),
        toX: cell.dataset.x,
        toY: Number(cell.dataset.y),
        isFlip: true
    };

    selectedCell.classList.remove('selected');
    selectedCell = null;
    send(move);
}

function addMoveLog(move, flipResult, capturedPiece) {
    const from = `${move.fromX}${move.fromY}`;
    const to = `${move.toX}${move.toY}`;
    let text = `${from} -> ${to}`;

	const currentSymbols = currentTurn === 'black' ? BLACK_SYMBOLS : PIECE_SYMBOLS;
    
	if (flipResult) {
		const flipText = currentSymbols[flipResult] || flipResult;
        text += ` (翻出: ${flipText})`;
    }
    if (capturedPiece) {
		const targetY = parseInt(move.toY);
		        const victimColor = targetY >= 5 ? 'black' : 'red'; // 粗略根据原初始半场判断（或跟随规则）
		        const victimSymbols = victimColor === 'black' ? BLACK_SYMBOLS : PIECE_SYMBOLS;
		        
		        const capturedText = victimSymbols[capturedPiece] || currentSymbols[capturedPiece] || capturedPiece;
        text += ` (吃: ${capturedText})`;
    }

    const div = document.createElement('div');
    div.textContent = text;
    moveLogEl.prepend(div);
}

function setButtonStates(states) {
    if (states.startMatch !== undefined) {
        document.getElementById('startMatchButton').disabled = !states.startMatch;
    }
    if (states.cancelMatch !== undefined) {
        document.getElementById('cancelMatchButton').disabled = !states.cancelMatch;
    }
    if (states.ready !== undefined) {
        document.getElementById('readyButton').disabled = !states.ready;
        document.getElementById('readyButton').textContent = states.ready ? '准备' : '已准备';
        myReady = !states.ready;
    }
    if (states.resign !== undefined) {
        document.getElementById('resignButton').disabled = !states.resign;
    }
}

function updateTurnIndicator() {
    document.body.classList.remove('turn-red', 'turn-black');
    document.body.classList.add(`turn-${currentTurn}`);
}

function startTimer() {
    clearInterval(timerInterval);
    deadlineTime = Date.now() + 60000;

    timerInterval = setInterval(() => {
        const remaining = deadlineTime - Date.now();
        if (remaining <= 0) {
            timeLeftEl.textContent = '剩余时间: 00:00';
            clearInterval(timerInterval);
            return;
        }

        const seconds = Math.floor(remaining / 1000);
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        timeLeftEl.textContent = `剩余时间: ${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }, 1000);
}

function startHeartbeat() {
    heartbeatInterval = setInterval(() => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            send({ messageType: 'ping' });
        }
    }, 10000);
}

function clearTimers() {
    clearInterval(timerInterval);
    clearInterval(heartbeatInterval);
}

function showLoginStatus(msg, type) {
    const el = document.getElementById('loginStatus');
    el.textContent = msg;
    el.className = `status ${type}`;
}

function logout() {
    if (socket) {
        socket.close();
    }
    clearTimers();
    loggedInUser = null;
    myColor = null;
    gameStatus = 'waiting';
    boardState = {};
	
	if (usernameDisplay) {
        usernameDisplay.textContent = '当前用户: 未登录';
    }
	
	capturedRedPieces = [];
	capturedBlackPieces = [];
	const redPool = document.getElementById('capturedRedPieces');
	const blackPool = document.getElementById('capturedBlackPieces');
    if (redPool) redPool.innerHTML = '';
    if (blackPool) blackPool.innerHTML = '';

    gameScreen.classList.remove('active');
    loginScreen.classList.add('active');
    document.getElementById('userId').value = '';
    document.getElementById('passWord').value = '';
    document.getElementById('nickName').value = '';
    showLoginStatus('', '');

    redPlayerEl.textContent = '红方: 等待...';
    blackPlayerEl.textContent = '黑方: 等待...';
    statusLine.textContent = '未开始';
    currentTurnEl.textContent = '等待匹配';
    yourColorEl.textContent = '你的颜色: 未知';
    timeLeftEl.textContent = '剩余时间: --:--';
    messageEl.textContent = '点击"开始匹配"寻找对手';
    moveLogEl.innerHTML = '';
    board.innerHTML = '';
}

function renderEmptyBoard() {
    board.replaceChildren();
    for (let y = 9; y >= 0; y--) {
        // 先渲染正常的行
        for (let col = 0; col < 9; col++) {
            const x = String.fromCharCode('a'.charCodeAt(0) + col);
            const cell = document.createElement('div');
            cell.className = 'cell';
            cell.dataset.x = x;
            cell.dataset.y = String(y);
            cell.dataset.coord = `${x}${y}`;
            cell.addEventListener('click', () => handleCellClick(cell));
            board.appendChild(cell);
        }
        
        // 当渲染完 y=5 行后，紧接着在它下方插入 2 行独立的河道
        if (y === 5) {
            for (let r = 0; r < 2; r++) {
                for (let col = 0; col < 9; col++) {
                    const riverCell = document.createElement('div');
                    riverCell.className = 'cell river-cell';
                    riverCell.dataset.riverRow = String(r);
                    riverCell.dataset.riverCol = String(col);
                    board.appendChild(riverCell);
                }
            }
        }
    }
}

function renderCapturedPieces() {
    const redPool = document.getElementById('capturedRedPieces');
    const blackPool = document.getElementById('capturedBlackPieces');
    
	if (redPool) redPool.innerHTML = '';
	if (blackPool) blackPool.innerHTML = '';

    // 渲染红方被吃棋子（使用红方字库）
    capturedRedPieces.forEach(type => {
        const el = document.createElement('div');
        el.className = 'captured-mini-piece red';
		if (type === 'HIDDEN_CAPTURED') {
		            el.textContent = '?';
		            el.classList.add('hidden-captured-mini'); // 方便后续写样式美化
		        } else {
		            el.textContent = PIECE_SYMBOLS[type] || type;
		        }
		if (redPool) redPool.appendChild(el);
    });

    // 渲染黑方被吃棋子（使用黑方字库，如 卒、士、象、将）
    capturedBlackPieces.forEach(type => {
        const el = document.createElement('div');
        el.className = 'captured-mini-piece black';
		if (type === 'HIDDEN_CAPTURED') {
		            el.textContent = '?';
		            el.classList.add('hidden-captured-mini');
		        } else {
		            el.textContent = BLACK_SYMBOLS[type] || type;
		        }
		if (blackPool) blackPool.appendChild(el);
    });
}

renderEmptyBoard();