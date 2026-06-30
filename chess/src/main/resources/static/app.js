const board = document.querySelector("#board");
const statusLine = document.querySelector("#status");
const log = document.querySelector("#log");

let socket = null;
let selected = null;

function renderEmptyBoard() {
    board.replaceChildren();
    for (let y = 9; y >= 0; y -= 1) {
        for (let col = 0; col < 9; col += 1) {
            const x = String.fromCharCode("a".charCodeAt(0) + col);
            const cell = document.createElement("button");
            cell.className = "cell";
            cell.type = "button";
            cell.dataset.x = x;
            cell.dataset.y = String(y);
            cell.textContent = `${x}${y}`;
            cell.addEventListener("click", () => handleCellClick(cell));
            board.append(cell);
        }
    }
}

function handleCellClick(cell) {
    if (!selected) {
        selected = cell;
        cell.classList.add("selected");
        return;
    }
    const move = {
        messageType: "move",
        fromX: selected.dataset.x,
        fromY: Number(selected.dataset.y),
        toX: cell.dataset.x,
        toY: Number(cell.dataset.y),
        isFlip: true
    };
    selected.classList.remove("selected");
    selected = null;
    send(move);
}

function send(payload) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
        statusLine.textContent = "Socket is not connected";
        return;
    }
    socket.send(JSON.stringify(payload));
}

document.querySelector("#connectButton").addEventListener("click", () => {
    socket = new WebSocket(document.querySelector("#wsUrl").value);
    socket.addEventListener("open", () => {
        statusLine.textContent = "Connected";
    });
    socket.addEventListener("message", (event) => {
        const item = document.createElement("div");
        item.textContent = event.data;
        log.prepend(item);
    });
    socket.addEventListener("close", () => {
        statusLine.textContent = "Disconnected";
    });
});

document.querySelector("#startButton").addEventListener("click", () => {
    send({messageType: "startMatch"});
});

document.querySelector("#resignButton").addEventListener("click", () => {
    send({messageType: "Resign"});
});

renderEmptyBoard();
