let stompClient = null;
let isConnected = false;
let blockReconnect = false;

// 페이지 로드 시 토큰 확인 및 상태 업데이트
document.addEventListener('DOMContentLoaded', function() {
    updateTokenStatus();
    initializeEventListeners();
});

function getCurrentToken() {
    return localStorage.getItem('accessToken');
}

function updateTokenStatus() {
    const token = getCurrentToken();
    const statusElement = document.getElementById('connection-status');

    if (token) {
        statusElement.style.color = '#28a745';
        statusElement.textContent = '● 토큰 인증됨 - 연결 가능';
    } else {
        statusElement.style.color = '#dc3545';
        statusElement.textContent = '● 토큰 없음 - 로그인 필요';
        // 토큰이 없으면 로그인 페이지로 리다이렉트
        window.location.href = '/login/login.html';
    }
}

function connect() {
    if (blockReconnect) {
        logMessage("🚫 이전 인증 실패로 인해 재연결이 차단되었습니다.", "error");
        return;
    }

    const token = getCurrentToken();

    if (!token) {
        logMessage("❌ JWT 토큰이 없습니다. 로그인 페이지로 이동합니다.", "error");
        window.location.href = '/login/login.html';
        return;
    }

    if (isConnected) {
        console.log("🔄 이미 연결되어 있습니다.");
        return;
    }

    stompClient = new StompJs.Client({
        webSocketFactory: () => new SockJS('/ws-stomp'),
        connectHeaders: {
            Authorization: `Bearer ${token}`
        },
        reconnectDelay: 60000,
        debug: (str) => console.log('[DEBUG]', str),
        onConnect: () => {
            console.log("✅ Connected");
            $("#connect").prop("disabled", true);
            $("#disconnect").prop("disabled", false);
            isConnected = true;
            updateConnectionStatus('connected');
            logMessage("✅ WebSocket 연결됨", "info");
        },
        onDisconnect: () => {
            console.log("❌ Disconnected");
            $("#connect").prop("disabled", false);
            $("#disconnect").prop("disabled", true);
            isConnected = false;
            updateConnectionStatus('disconnected');
        },
        onStompError: (frame) => {
            const msg = frame.headers['message'] || '';
            console.error("🚨 STOMP error", msg);

            if (msg.toLowerCase().includes("unauthorized") || msg.includes("401")) {
                logMessage("🚫 인증 실패. 토큰이 만료되었습니다.", "error");

                blockReconnect = true;
                isConnected = false;

                // 토큰 정리하고 로그인 페이지로 이동
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('provider');

                // stompClient 즉시 제거
                stompClient.deactivate().then(() => {
                    stompClient = null;
                    console.log("🧹 stompClient 제거 완료");
                    window.location.href = '/login/login.html';
                });

                $("#connect").prop("disabled", false);
                $("#disconnect").prop("disabled", true);
                updateConnectionStatus('error');
            }
        },
        onWebSocketError: (event) => {
            console.error("🚨 WebSocket error", event);
        }
    });

    stompClient.activate();
}

function disconnect() {
    if (stompClient && isConnected) {
        stompClient.deactivate().then(() => {
            logMessage("❌ WebSocket 연결 끊김", "warn");
            isConnected = false;
            $("#connect").prop("disabled", false);
            $("#disconnect").prop("disabled", true);
            updateConnectionStatus('disconnected');
        });
    }
}

function updateConnectionStatus(status) {
    const statusElement = document.getElementById('connection-status');

    switch(status) {
        case 'connected':
            statusElement.style.color = '#28a745';
            statusElement.textContent = '● WebSocket 연결됨';
            break;
        case 'disconnected':
            statusElement.style.color = '#ffc107';
            statusElement.textContent = '● WebSocket 연결 끊김';
            break;
        case 'error':
            statusElement.style.color = '#dc3545';
            statusElement.textContent = '● 연결 오류';
            break;
        default:
            updateTokenStatus();
    }
}

function getTokenHeader() {
    const token = getCurrentToken();
    return {
        Authorization: token ? `Bearer ${token}` : ""
    };
}

const subscriptions = {
    month: null,
    day: null,
    slot: null
};

// 기존 응답 저장용 전역 객체 초기화
const responseDataMap = {
    month: null,
    day: null,
    slot: null
};

function flashButton(btn) {
    btn.addClass("clicked");
    setTimeout(() => btn.removeClass("clicked"), 200);
}

function clearDataDetail() {
    responseDataMap.month = null;
    responseDataMap.day = null;
    responseDataMap.slot = null;
    renderDataDetail();
}

function renderDataDetail() {
    const container = document.getElementById("dataDetail");
    container.innerHTML = "";

    const dataList = [];

    if (responseDataMap.month) {
        dataList.push({ title: "📅 [한달 데이터]", content: responseDataMap.month });
    }
    if (responseDataMap.day) {
        dataList.push({ title: "📆 [하루 데이터]", content: responseDataMap.day });
    }
    if (responseDataMap.slot) {
        dataList.push({ title: "🕒 [슬롯 상세 데이터]", content: responseDataMap.slot });
    }

    const layout = document.createElement("div");
    layout.style.display = "flex";
    layout.style.gap = "10px";
    layout.style.justifyContent = "space-between";

    dataList.forEach(data => {
        const block = document.createElement("div");
        block.style.flex = "1";
        block.style.minWidth = "0";

        const title = document.createElement("h5");
        title.textContent = data.title;

        const pre = document.createElement("pre");
        pre.textContent = data.content;

        block.appendChild(title);
        block.appendChild(pre);
        layout.appendChild(block);
    });

    container.appendChild(layout);
}

function fetchMonthData(groupId, year, month) {
    clearDataDetail();
    fetch(`/api/groups/${groupId}/date/month?year=${year}&month=${month}`, {
        headers: getTokenHeader()
    })
    .then(res => {
        if (res.status === 401) {
            handleTokenExpired();
            return;
        }
        return res.json();
    })
    .then(data => {
        if (data && data.code === 200 && data.data) {
            responseDataMap.month = JSON.stringify(data.data, null, 2);
        } else {
            responseDataMap.month = `[한달 데이터] API 오류: ${data?.message || 'Unknown error'}`;
        }
        renderDataDetail();
    })
    .catch(err => {
        responseDataMap.month = `API 호출 실패: ${err}`;
        renderDataDetail();
    });
}

function fetchDayData(groupId, year, month, day) {
    clearDataDetail();
    fetch(`/api/groups/${groupId}/date/day?year=${year}&month=${month}&day=${day}`, {
        headers: getTokenHeader()
    })
    .then(res => {
        if (res.status === 401) {
            handleTokenExpired();
            return;
        }
        return res.json();
    })
    .then(data => {
        if (data && data.code === 200 && data.data) {
            responseDataMap.day = JSON.stringify(data.data, null, 2);
        } else {
            responseDataMap.day = `[하루 데이터] API 오류: ${data?.message || 'Unknown error'}`;
        }
        renderDataDetail();
    })
    .catch(err => {
        responseDataMap.day = `API 호출 실패: ${err}`;
        renderDataDetail();
    });
}

function fetchSlotData(groupId, slotId) {
    clearDataDetail();
    fetch(`/api/groups/${groupId}/slots/${slotId}`, {
        headers: getTokenHeader()
    })
    .then(res => {
        if (res.status === 401) {
            handleTokenExpired();
            return;
        }
        return res.json();
    })
    .then(data => {
        if (data && data.code === 200 && data.data) {
            responseDataMap.slot = JSON.stringify(data.data, null, 2);
        } else {
            responseDataMap.slot = `[슬롯 상세] API 오류: ${data?.message || 'Unknown error'}`;
        }
        renderDataDetail();
    })
    .catch(err => {
        responseDataMap.slot = `API 호출 실패: ${err}`;
        renderDataDetail();
    });
}

function handleTokenExpired() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('provider');
    window.location.href = '/login/login.html';
}

function subscribeTo(type, channel, onMessage) {
    if (!stompClient || !stompClient.connected) {
        const msg = `WebSocket 연결되지 않음 - 채널(${channel}) 구독 실패`;
        logMessage(msg, "error");
        return;
    }

    // 이미 구독 중이면 => 구독 해제
    if (subscriptions[type]) {
        subscriptions[type].unsubscribe();
        subscriptions[type] = null;
        logMessage(`📭 ${type} 구독 취소됨 (${channel})`, "warn");
        return;
    }

    // 새로 구독 시작
    const sub = stompClient.subscribe(channel, (message) => {
        try {
            const body = JSON.parse(message.body);
            logMessage(`📩 메시지 수신: [${channel}] ${JSON.stringify(body)}`, "info");

            if (onMessage && typeof onMessage === "function") {
                onMessage(body);
            }
        } catch (e) {
            logMessage(`메시지 처리 중 오류 발생: ${e}`, "error");
        }
    });

    subscriptions[type] = sub;
    logMessage(`📬 ${type} 구독 시작: ${channel}`, "info");
}

function logMessage(message, type = "info") {
    const prefix = {
        info: "[INFO] ",
        error: "[ERROR] ",
        warn: "[WARN] "
    }[type] || "";

    $("#messages").append(prefix + message + "\n");
}

function initializeEventListeners() {
    $("form").on('submit', e => e.preventDefault());

    $("#connect").click(connect);
    $("#disconnect").click(disconnect);

    $("#subscribeMonth").click(() => {
        flashButton($("#subscribeMonth"));

        const groupId = $("#groupId").val();
        const year = $("#year").val();
        const month = $("#month").val();
        const topic = `/sub/groups/${groupId}/date/month?year=${year}&month=${month}`;

        subscribeTo("month", topic, () => {
            fetchMonthData(groupId, year, month);
        });
    });

    $("#subscribeDay").click(() => {
        flashButton($("#subscribeDay"));

        const groupId = $("#groupId").val();
        const year = $("#year").val();
        const month = $("#month").val();
        const day = $("#day").val();
        const topic = `/sub/groups/${groupId}/date/day?year=${year}&month=${month}&day=${day}`;

        subscribeTo("day", topic, () => {
            fetchDayData(groupId, year, month, day);
        });
    });

    $("#subscribeSlot").click(() => {
        flashButton($("#subscribeSlot"));

        const groupId = $("#groupId").val();
        const slotId = $("#slotId").val();
        const topic = `/sub/groups/${groupId}/slots/${slotId}`;

        subscribeTo("slot", topic, () => {
            fetchSlotData(groupId, slotId);
        });
    });
}