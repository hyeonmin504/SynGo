let stompClient = null;
let isConnected = false;  // 연결 상태 플래그

function connect() {
    if (isConnected) {
        console.log("🔄 이미 연결되어 있습니다.");
        return;  // 중복 연결 방지
    }

    const tokenRaw = $("#jwtToken").val();
    const token = tokenRaw ? tokenRaw.trim() : "";

    stompClient = new StompJs.Client({
        webSocketFactory: () => new SockJS('/ws-stomp'),
        connectHeaders: {
            Authorization: token ? `Bearer ${token}` : ''
        },
        reconnectDelay: 5000,
        debug: (str) => console.log('[DEBUG]', str),
        onConnect: () => {
            console.log("✅ Connected");
            $("#connect").prop("disabled", true);
            $("#disconnect").prop("disabled", false);
            isConnected = true;
        },
        onDisconnect: () => {
            console.log("❌ Disconnected");
            $("#connect").prop("disabled", false);
            $("#disconnect").prop("disabled", true);
            isConnected = false;
        },
        onStompError: (frame) => {
            console.error("🚨 STOMP error", frame.headers['message']);
        },
        onWebSocketError: (event) => {
            console.error("🚨 WebSocket error", event);
        }
    });

    stompClient.activate();
}

function disconnect() {
    if (stompClient && isConnected) {
        stompClient.deactivate();
        isConnected = false; // 상태 업데이트
    }
}

function getTokenHeader() {
    const tokenRaw = $("#jwtToken").val();
    const token = tokenRaw ? tokenRaw.trim() : "";
    return {
        Authorization: token ? `Bearer ${token}` : ""
    };
}

// REST API 호출 함수들
function fetchMonthData(groupId, year, month) {
    fetch(`/api/groups/${groupId}/date/month?year=${year}&month=${month}`, {
            headers: getTokenHeader()
        })
        .then(res => res.json())
        .then(data => {
            console.log("🔄 한달 데이터 API 응답:", data);
            if (data.code === 200 && data.data) {
                const slot = data.data;

                // 전체 데이터를 JSON 문자열로 보기 좋게 변환
                const fullJson = JSON.stringify(slot, null, 2);  // 2는 들여쓰기 간격
                console.log("🔄 month 상세 데이터:", fullJson);
                $("#dataDetail").text(fullJson);
            } else {
                $("#dataDetail").text(`[슬롯 상세] API 오류: ${data.message}`);
            }
        })
        .catch(err => console.error("API 호출 실패:", err));
}

function fetchDayData(groupId, year, month, day) {
    fetch(`/api/groups/${groupId}/date/day?year=${year}&month=${month}&day=${day}`, {
            headers: getTokenHeader()
        })
        .then(res => res.json())
        .then(data => {
            console.log("🔄 하루 데이터 API 응답:", data);
            if (data.code === 200 && data.data) {
                const slot = data.data;

                // 전체 데이터를 JSON 문자열로 보기 좋게 변환
                const fullJson = JSON.stringify(slot, null, 2);  // 2는 들여쓰기 간격
                console.log("🔄 Day 상세 데이터:", fullJson);
                $("#dataDetail").text(fullJson);
            } else {
                $("#dataDetail").text(`[슬롯 상세] API 오류: ${data.message}`);
            }
        })
        .catch(err => console.error("API 호출 실패:", err));
}

function fetchSlotData(groupId, slotId) {
    fetch(`/api/groups/${groupId}/slots/${slotId}`, {
        headers: getTokenHeader()
    })
    .then(res => res.json())
    .then(data => {
        console.log("🔄 슬롯 상세 API 응답:", data);

        if (data.code === 200 && data.data) {
            const slot = data.data;

            // 전체 데이터를 JSON 문자열로 보기 좋게 변환
            const fullJson = JSON.stringify(slot, null, 2);  // 2는 들여쓰기 간격
            console.log("🔄 슬롯 상세 데이터:", fullJson);
            $("#dataDetail").text(fullJson);
        } else {
            $("#dataDetail").text(`[슬롯 상세] API 오류: ${data.message}`);
        }
    });
}

function subscribeTo(channel, onMessage) {
    if (!stompClient || !stompClient.connected) {
        console.warn("⚠️ WebSocket이 연결되지 않았습니다.");
        return;
    }

    console.log("📬 구독 시작:", channel);

    stompClient.subscribe(channel, (message) => {
        const body = JSON.parse(message.body);
        console.log("📩 메시지 수신:", body);
        $("#messages").append("[" + channel + "] " + JSON.stringify(body) + "\n");

        if (onMessage && typeof onMessage === "function") {
            onMessage(body);
        }
    });
}

$(document).ready(() => {
    $("form").on('submit', e => e.preventDefault()); // 폼 제출 막기

    $("#connect").click(connect);
    $("#disconnect").click(disconnect);

    $("#subscribeMonth").click(() => {
        const groupId = $("#groupId").val();
        const year = $("#year").val();
        const month = $("#month").val();
        const topic = `/sub/groups/${groupId}/date/month?year=${year}&month=${month}`;

        subscribeTo(topic, () => {
            fetchMonthData(groupId, year, month);
        });
    });

    $("#subscribeDay").click(() => {
        const groupId = $("#groupId").val();
        const year = $("#year").val();
        const month = $("#month").val();
        const day = $("#day").val();
        const topic = `/sub/groups/${groupId}/date/day?year=${year}&month=${month}&day=${day}`;

        subscribeTo(topic, () => {
            fetchDayData(groupId, year, month, day);
        });
    });

    $("#subscribeSlot").click(() => {
        const groupId = $("#groupId").val();
        const slotId = $("#slotId").val();
        const topic = `/sub/groups/${groupId}/slots/${slotId}`;

        subscribeTo(topic, () => {
            fetchSlotData(groupId, slotId);
        });
    });
});