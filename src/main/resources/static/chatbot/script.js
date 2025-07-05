// 전역 상태 관리
let isStreaming = false;
let currentAssistantDiv = null;
let abortController = null;
let streamingTimeout = null;
let streamCompleted = false;

// DOM 로드 완료 후 이벤트 리스너 등록
document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners();
});

function initializeEventListeners() {
    // Enter 키 처리
    document.getElementById('message').addEventListener('keydown', function(event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            sendMessage();
        }
    });

    // 이미지 미리보기
    document.getElementById('imageInput').addEventListener('change', function(event) {
        previewImages(event.target.files);
    });

    // 페이지 언로드 시 스트림 정리
    window.addEventListener('beforeunload', function() {
        if (isStreaming && abortController) {
            abortController.abort();
        }
    });
}

function previewImages(files) {
    const previewDiv = document.getElementById('image-preview');
    previewDiv.innerHTML = '';

    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const reader = new FileReader();
        reader.onload = function(e) {
            const img = document.createElement('img');
            img.src = e.target.result;
            img.className = 'image-preview';
            img.alt = file.name;
            previewDiv.appendChild(img);
        };
        reader.readAsDataURL(file);
    }
}

function cancelStream() {
    console.log('스트림 취소 요청됨');
    if (abortController) {
        abortController.abort();
    }
    forceStopStreaming();
}

function forceStopStreaming() {
    console.log('스트림 강제 종료');
    isStreaming = false;
    streamCompleted = true;
    currentAssistantDiv = null;

    if (abortController) {
        abortController.abort();
        abortController = null;
    }

    if (streamingTimeout) {
        clearTimeout(streamingTimeout);
        streamingTimeout = null;
    }

    setStreamingState(false);
}

async function sendMessage() {
    console.log('sendMessage 호출됨, isStreaming:', isStreaming);

    // 중복 요청 방지
    if (isStreaming) {
        console.log('이미 스트리밍 중이므로 중단');
        showStatus('이미 응답 중입니다.', 'warning');
        return;
    }

    const message = document.getElementById('message').value.trim();
    const imageFiles = document.getElementById('imageInput').files;

    // 입력 검증
    if (!message && imageFiles.length === 0) {
        showStatus('메시지나 이미지를 입력하세요.', 'error');
        return;
    }

    // 상태 초기화
    isStreaming = true;
    streamCompleted = false;
    abortController = new AbortController();
    setStreamingState(true);

    const displayMessage = message || '[이미지 분석 요청]';

    // 사용자 메시지 표시
    appendMessage('user', displayMessage);

    if (imageFiles.length > 0) {
        const imageNames = Array.from(imageFiles).map(f => f.name).join(', ');
        appendMessage('user', `📎 첨부된 이미지: ${imageNames}`, true);
    }

    // FormData 준비
    const formData = new FormData();
    if (message) formData.append('message', message);
    for (let i = 0; i < imageFiles.length; i++) {
        formData.append('images', imageFiles[i]);
    }

    // 타임아웃 설정 (30초)
    streamingTimeout = setTimeout(() => {
        console.log('스트리밍 타임아웃');
        if (!streamCompleted) {
            forceStopStreaming();
            appendMessage('assistant', '⏰ 응답 시간이 초과되었습니다.');
        }
    }, 30000);

    try {
        console.log('fetch 시작');
        const response = await fetch('http://localhost:8080/api/my/chatbot/stream', {
            method: 'POST',
            body: formData,
            signal: abortController.signal
        });

        console.log('응답 받음:', response.status, response.statusText);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        showStatus('응답을 받고 있습니다...', 'success');
        await processStream(response);

    } catch (error) {
        console.error('스트리밍 오류:', error);
        handleStreamError(error);
    } finally {
        console.log('finally 블록 실행');
        if (!streamCompleted) {
            forceStopStreaming();
        }
        clearInputs();
    }
}

async function processStream(response) {
    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';
    let hasReceivedData = false;

    console.log('스트림 읽기 시작');
    console.log('Response headers:', Object.fromEntries(response.headers.entries()));

    try {
        while (!streamCompleted && !abortController.signal.aborted) {
            const { value, done } = await reader.read();

            console.log('Reader read result:', { value: value ? value.length + ' bytes' : 'null', done });

            if (done) {
                console.log('스트림 완료 (done=true)');
                if (!hasReceivedData) {
                    console.warn('⚠️ 데이터를 전혀 받지 못했습니다!');
                    appendMessage('assistant', '❌ 서버에서 응답 데이터를 받지 못했습니다.');
                }
                streamCompleted = true;
                break;
            }

            if (abortController.signal.aborted) {
                console.log('스트림 중단됨 (signal.aborted)');
                break;
            }

            const chunk = decoder.decode(value, { stream: true });
            console.log('Raw chunk received:', JSON.stringify(chunk));
            buffer += chunk;
            hasReceivedData = true;

            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            console.log('Lines to process:', lines.length);

            for (const line of lines) {
                console.log('Processing line:', JSON.stringify(line));

                if (!line.trim() || streamCompleted) continue;

                // SSE 형식 처리 개선 (공백 있음/없음 모두 처리)
                if (line.startsWith('data:')) {
                    let jsonString;
                    if (line.startsWith('data: ')) {
                        jsonString = line.substring(6).trim();
                    } else if (line.startsWith('data:')) {
                        jsonString = line.substring(5).trim();
                    }

                    console.log('SSE 데이터 수신:', jsonString);

                    if (jsonString === '[DONE]' || jsonString === '{"done": true}' ||
                        jsonString.includes('"done":true') || jsonString.includes('Stream completed')) {
                        console.log('스트림 완료 신호 수신');
                        streamCompleted = true;
                        break;
                    }

                    try {
                        const json = JSON.parse(jsonString);
                        console.log('Parsed JSON:', json);
                        const content = extractContent(json);
                        console.log('Extracted content:', content);
                        if (content) {
                            appendStreamChunk('assistant', content);
                        } else {
                            console.warn('No content extracted from:', json);
                        }
                    } catch (e) {
                        console.warn('JSON 파싱 실패:', e, jsonString);
                        // JSON이 아닌 일반 텍스트인 경우
                        if (jsonString && !jsonString.startsWith('{')) {
                            console.log('Adding raw text:', jsonString);
                            appendStreamChunk('assistant', jsonString);
                        }
                    }
                } else {
                    // SSE 형식이 아닌 라인도 로깅
                    console.log('Non-SSE line:', JSON.stringify(line));
                }
            }
        }

        // 스트림 정상 완료
        console.log('스트림 정상 완료, hasReceivedData:', hasReceivedData);
        streamCompleted = true;

    } catch (readerError) {
        console.log('스트림 리더 오류:', readerError);
        if (readerError.name !== 'AbortError') {
            streamCompleted = true;
            throw readerError;
        }
    } finally {
        // reader 정리
        try {
            if (!streamCompleted) {
                await reader.cancel();
            }
            reader.releaseLock();
        } catch (e) {
            console.log('reader 정리 오류 (무시):', e.message);
        }

        // 완료 처리
        if (streamCompleted) {
            forceStopStreaming();
            showStatus(hasReceivedData ? '응답 완료' : '응답 데이터 없음', hasReceivedData ? 'success' : 'warning');
        }
    }
}

function handleStreamError(error) {
    if (error.name === 'AbortError') {
        console.log('요청이 중단됨');
        showStatus('요청이 중단되었습니다.', 'warning');
    } else if (error.message && error.message.includes('network error')) {
        console.log('네트워크 오류 - 정상 완료로 간주');
        streamCompleted = true;
        showStatus('응답 완료', 'success');
    } else {
        showStatus(`연결 오류: ${error.message}`, 'error');
        appendMessage('assistant', `❌ 연결 오류: ${error.message}`);
    }
}

function extractContent(json) {
    if (!json) return null;

    if (json.error) {
        return `오류: ${json.error}`;
    }

    if (Array.isArray(json)) {
        return json.map(extractContent).filter(Boolean).join('');
    }

    if (json?.result?.output?.content) {
        return json.result.output.content;
    }

    if (json?.results?.length > 0) {
        return json.results.map(r => r.output?.content).filter(Boolean).join('');
    }

    if (json.content) {
        return json.content;
    }

    return null;
}

function appendMessage(sender, text, isSubMessage = false) {
    const chatBox = document.getElementById('chat-box');
    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${sender}`;

    if (isSubMessage) {
        msgDiv.style.marginTop = '5px';
        msgDiv.style.opacity = '0.8';
    }

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.textContent = sender === 'user' ? '👤' : '🤖';

    const content = document.createElement('div');
    content.className = 'message-content';
    content.textContent = text;

    msgDiv.appendChild(avatar);
    msgDiv.appendChild(content);
    chatBox.appendChild(msgDiv);
    chatBox.scrollTop = chatBox.scrollHeight;
}

function appendStreamChunk(sender, chunkText) {
    const chatBox = document.getElementById('chat-box');

    if (!currentAssistantDiv) {
        currentAssistantDiv = document.createElement('div');
        currentAssistantDiv.className = `message ${sender}`;

        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        avatar.textContent = '🤖';

        const content = document.createElement('div');
        content.className = 'message-content';
        content.id = 'streaming-content';

        currentAssistantDiv.appendChild(avatar);
        currentAssistantDiv.appendChild(content);
        chatBox.appendChild(currentAssistantDiv);
    }

    const contentDiv = currentAssistantDiv.querySelector('#streaming-content');
    if (contentDiv) {
        contentDiv.textContent += chunkText;
        chatBox.scrollTop = chatBox.scrollHeight;
    }
}

function setStreamingState(streaming) {
    const sendButton = document.getElementById('send-button');
    const cancelButton = document.getElementById('cancel-button');
    const typingIndicator = document.getElementById('typing-indicator');

    sendButton.disabled = streaming;
    sendButton.textContent = streaming ? '응답 중...' : '전송';

    if (streaming) {
        cancelButton.style.display = 'inline-block';
        typingIndicator.classList.add('show');
    } else {
        cancelButton.style.display = 'none';
        typingIndicator.classList.remove('show');
    }
}

function clearInputs() {
    document.getElementById('message').value = '';
    document.getElementById('imageInput').value = '';
    document.getElementById('image-preview').innerHTML = '';
}

function showStatus(message, type) {
    const statusDiv = document.getElementById('auth-status');
    statusDiv.innerHTML = `<div class="status-indicator status-${type}">${message}</div>`;

    setTimeout(() => {
        if (statusDiv.innerHTML.includes(message)) {
            statusDiv.innerHTML = '';
        }
    }, 5000);
}

// 더미 함수들 (개발 모드)
function testToken() {
    showStatus('개발 모드: 인증이 비활성화되었습니다', 'warning');
}

function testAuthEndpoint() {
    showStatus('개발 모드: 인증이 비활성화되었습니다', 'warning');
}