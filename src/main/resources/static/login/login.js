// 상수 정의
const API_BASE_URL = 'http://localhost:8080';
const REDIRECT_URL = 'http://localhost:8080/chatbot/chatbot.html';

// DOM 요소들
const loginForm = document.getElementById('loginForm');
const signupForm = document.getElementById('signupForm');
const loadingDiv = document.getElementById('loading');
const messageDiv = document.getElementById('message');
const authSwitchLink = document.getElementById('auth-switch-link');
const authSwitchText = document.getElementById('auth-switch-text');

// 현재 모드 (login 또는 signup)
let currentMode = 'login';

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    initializePage();
    bindEvents();
});

// 페이지 초기화
function initializePage() {
    // OAuth2 결과 확인
    checkOAuth2Result();

    // 이미 로그인된 상태 확인
    checkExistingLogin();
}

// 이벤트 바인딩
function bindEvents() {
    // 로그인 폼 제출
    loginForm.addEventListener('submit', handleLogin);

    // 회원가입 폼 제출
    signupForm.addEventListener('submit', handleSignup);

    // 로그인/회원가입 전환
    authSwitchLink.addEventListener('click', function(e) {
        e.preventDefault();
        toggleAuthMode();
    });

    // 비밀번호 확인 실시간 검증
    const passwordField = document.getElementById('signup-password');
    const checkPasswordField = document.getElementById('signup-check-password');

    if (checkPasswordField) {
        checkPasswordField.addEventListener('input', function() {
            validatePasswordMatch();
        });

        passwordField.addEventListener('input', function() {
            if (checkPasswordField.value) {
                validatePasswordMatch();
            }
        });
    }
}

// OAuth2 결과 확인
function checkOAuth2Result() {
    const urlParams = new URLSearchParams(window.location.search);
    const accessToken = urlParams.get('accessToken');
    const refreshToken = urlParams.get('refreshToken');
    const provider = urlParams.get('provider');
    const error = urlParams.get('error');
    const message = urlParams.get('message');

    console.log('URL 파라미터 확인:', {
        accessToken: accessToken ? '존재' : '없음',
        refreshToken: refreshToken ? '존재' : '없음',
        provider,
        error,
        message,
        fullSearch: window.location.search
    });

    if (error) {
        showMessage('OAuth2 로그인 실패: ' + (message || error), 'error');
        // ✅ 에러 메시지 표시 후 약간의 딜레이를 두고 URL 정리
        setTimeout(() => {
            window.history.replaceState({}, document.title, window.location.pathname);
        }, 5000); // 5초 후에 URL 정리
    } else if (accessToken && refreshToken) {
        // 토큰 저장
        saveTokens(accessToken, refreshToken, provider || 'GOOGLE');
        showMessage('로그인 성공! 잠시 후 페이지로 이동합니다.', 'success');

        // 리다이렉트
        setTimeout(() => {
            window.location.href = REDIRECT_URL;
        }, 2000);
    }
}

// 기존 로그인 상태 확인 함수 수정
function checkExistingLogin() {
    // OAuth 결과가 있으면 checkOAuth2Result에서 처리하므로 건너뛰기
    const urlParams = new URLSearchParams(window.location.search);
    const hasOAuthResult = urlParams.get('accessToken') || urlParams.get('error');

    if (hasOAuthResult) {
        return; // OAuth 결과가 있으면 여기서 중단
    }

    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
        showMessage('이미 로그인되어 있습니다. 페이지로 이동합니다.', 'success');
        setTimeout(() => {
            window.location.href = REDIRECT_URL;
        }, 1500);
    }
}

// 로그인 처리
async function handleLogin(e) {
    e.preventDefault();

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    // 유효성 검사
    if (!email || !password) {
        showMessage('이메일과 비밀번호를 입력해주세요.', 'error');
        return;
    }

    if (!isValidEmail(email)) {
        showMessage('올바른 이메일 형식을 입력해주세요.', 'error');
        return;
    }

    showLoading(true);

    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                email: email,
                password: password
            })
        });

        const data = await response.json();

        if (data.code === 200 && data.data) {
            // 로그인 성공
            saveTokens(data.data.accessToken, data.data.refreshToken, 'LOCAL');
            showMessage('로그인 성공!', 'success');

            setTimeout(() => {
                window.location.href = REDIRECT_URL;
            }, 1000);
        } else {
            // 로그인 실패
            showMessage(data.message || '로그인에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Login error:', error);
        showMessage('서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요.', 'error');
    } finally {
        showLoading(false);
    }
}

// 회원가입 처리
async function handleSignup(e) {
    e.preventDefault();

    const name = document.getElementById('signup-name').value.trim();
    const email = document.getElementById('signup-email').value.trim();
    const password = document.getElementById('signup-password').value;
    const checkPassword = document.getElementById('signup-check-password').value;

    // 유효성 검사
    if (!name || !email || !password || !checkPassword) {
        showMessage('모든 필드를 입력해주세요.', 'error');
        return;
    }

    if (!isValidEmail(email)) {
        showMessage('올바른 이메일 형식을 입력해주세요.', 'error');
        return;
    }

    if (password !== checkPassword) {
        showMessage('비밀번호가 일치하지 않습니다.', 'error');
        return;
    }

    if (password.length < 8) {
        showMessage('비밀번호는 8자 이상이어야 합니다.', 'error');
        return;
    }

    showLoading(true);

    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/signup`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                name: name,
                email: email,
                password: password,
                check_password: checkPassword
            })
        });

        const data = await response.json();

        if (data.code === 200) {
            // 회원가입 성공
            showMessage('회원가입이 완료되었습니다! 로그인해주세요.', 'success');

            // 폼 초기화 후 로그인 모드로 전환
            signupForm.reset();
            setTimeout(() => {
                toggleAuthMode();
                // 회원가입한 이메일을 로그인 폼에 자동 입력
                document.getElementById('email').value = email;
            }, 2000);
        } else {
            // 회원가입 실패
            showMessage(data.message || '회원가입에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Signup error:', error);
        showMessage('서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요.', 'error');
    } finally {
        showLoading(false);
    }
}

// 로그인/회원가입 모드 전환
function toggleAuthMode() {
    if (currentMode === 'login') {
        // 회원가입 모드로 전환
        currentMode = 'signup';
        loginForm.style.display = 'none';
        signupForm.style.display = 'block';
        authSwitchText.innerHTML = '이미 계정이 있으신가요? <a href="#" id="auth-switch-link">로그인</a>';
        document.querySelector('.login-header p').textContent = '새 계정을 만드세요';
    } else {
        // 로그인 모드로 전환
        currentMode = 'login';
        signupForm.style.display = 'none';
        loginForm.style.display = 'block';
        authSwitchText.innerHTML = '계정이 없으신가요? <a href="#" id="auth-switch-link">회원가입</a>';
        document.querySelector('.login-header p').textContent = '계정에 로그인하세요';
    }

    // 새로운 링크에 이벤트 바인딩
    document.getElementById('auth-switch-link').addEventListener('click', function(e) {
        e.preventDefault();
        toggleAuthMode();
    });

    // 메시지 초기화
    clearMessage();
}

// 비밀번호 일치 검증
function validatePasswordMatch() {
    const password = document.getElementById('signup-password').value;
    const checkPassword = document.getElementById('signup-check-password').value;
    const checkPasswordField = document.getElementById('signup-check-password');

    if (checkPassword && password !== checkPassword) {
        checkPasswordField.style.borderColor = '#e74c3c';
        checkPasswordField.style.backgroundColor = '#fdf2f2';
    } else if (checkPassword && password === checkPassword) {
        checkPasswordField.style.borderColor = '#27ae60';
        checkPasswordField.style.backgroundColor = '#f2fdf2';
    } else {
        checkPasswordField.style.borderColor = '#e1e5e9';
        checkPasswordField.style.backgroundColor = '#fff';
    }
}

// 토큰 저장
function saveTokens(accessToken, refreshToken, provider) {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('provider', provider);
    localStorage.setItem('loginTime', new Date().toISOString());
}

// 메시지 표시
function showMessage(text, type = 'info') {
    messageDiv.innerHTML = `<div class="message ${type}">${text}</div>`;

    // 자동으로 메시지 제거 (에러는 더 오래 표시)
    const timeout = type === 'error' ? 7000 : 5000;
    setTimeout(() => {
        clearMessage();
    }, timeout);
}

// 메시지 제거
function clearMessage() {
    messageDiv.innerHTML = '';
}

// 로딩 상태 표시/숨김
function showLoading(show) {
    if (show) {
        loadingDiv.style.display = 'block';
        loginForm.style.display = 'none';
        signupForm.style.display = 'none';
    } else {
        loadingDiv.style.display = 'none';
        if (currentMode === 'login') {
            loginForm.style.display = 'block';
        } else {
            signupForm.style.display = 'block';
        }
    }
}

// 이메일 유효성 검사
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// 전역 에러 처리
window.addEventListener('error', function(e) {
    console.error('JavaScript Error:', e.error);
    showMessage('예상치 못한 오류가 발생했습니다.', 'error');
});

// 네트워크 상태 확인
window.addEventListener('online', function() {
    if (messageDiv.innerHTML.includes('서버 연결')) {
        showMessage('네트워크 연결이 복구되었습니다.', 'success');
    }
});

window.addEventListener('offline', function() {
    showMessage('네트워크 연결이 끊어졌습니다.', 'warning');
});