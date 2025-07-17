class Navigation {
    constructor() {
        this.currentUser = null;
        this.init();
    }

    init() {
        this.createNavigation();
        this.loadUserInfo();
        this.bindEvents();
        this.setActiveLink();
    }

    createNavigation() {
        const nav = document.createElement('nav');
        nav.className = 'top-nav';
        nav.innerHTML = `
            <div class="nav-container">
                <a href="/chatbot/chatbot.html" class="nav-brand">
                    <img src="/favicon_io/favicon.ico" alt="SynGo" style="width: 24px; height: 24px;">
                    SynGo
                </a>
                <div class="nav-menu">
                    <a href="/chatbot/chatbot.html" class="nav-link" data-page="chatbot">🤖 AI 챗봇</a>
                    <a href="/index.html" class="nav-link" data-page="websocket">🧩 WebSocket 테스트</a>
                </div>
                <div class="user-info">
                    <div id="user-display" class="nav-loading">로딩 중...</div>
                    <button class="logout-btn" id="nav-logout-btn">로그아웃</button>
                </div>
            </div>
        `;

        // body의 첫 번째 자식으로 추가
        document.body.insertBefore(nav, document.body.firstChild);
    }

    async loadUserInfo() {
        // OAuth 결과 확인 (URL 파라미터에서)
        const urlParams = new URLSearchParams(window.location.search);
        const accessToken = urlParams.get('accessToken');
        const refreshToken = urlParams.get('refreshToken');
        const provider = urlParams.get('provider');

        if (accessToken && refreshToken) {
            // 토큰 저장
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshToken);
            localStorage.setItem('provider', provider || 'GOOGLE');

            // URL에서 토큰 파라미터 제거
            const newUrl = window.location.pathname;
            window.history.replaceState({}, document.title, newUrl);
        }

        // 저장된 토큰 확인
        const storedToken = localStorage.getItem('accessToken');
        const storedProvider = localStorage.getItem('provider');

        if (!storedToken) {
            this.redirectToLogin();
            return;
        }

        // 토큰을 전역에서 사용할 수 있도록 설정
        window.currentAccessToken = storedToken;

        // 기본 정보 표시
        this.displayBasicUserInfo(storedProvider);
    }

    displayUserInfo() {
        const userDisplay = document.getElementById('user-display');
        const user = this.currentUser;

        userDisplay.innerHTML = `
            <div style="display: flex; align-items: center; gap: 0.5rem;">
                ${user.profileImageUrl ?
                    `<img src="${user.profileImageUrl}" alt="Profile" class="user-avatar">` :
                    '<div class="user-avatar" style="background: rgba(255,255,255,0.3); display: flex; align-items: center; justify-content: center;">👤</div>'
                }
                <span class="user-name">${user.name || user.email || '사용자'}</span>
                <span style="font-size: 0.8rem; opacity: 0.8;">(${user.provider || 'LOCAL'})</span>
            </div>
        `;
    }

    displayBasicUserInfo(provider) {
        const userDisplay = document.getElementById('user-display');
        userDisplay.innerHTML = `
            <div style="display: flex; align-items: center; gap: 0.5rem;">
                <div class="user-avatar" style="background: rgba(255,255,255,0.3); display: flex; align-items: center; justify-content: center;">👤</div>
                <span class="user-name">사용자</span>
                <span style="font-size: 0.8rem; opacity: 0.8;">(${provider || 'LOCAL'})</span>
            </div>
        `;
    }

    handleTokenExpired() {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('provider');
        this.redirectToLogin();
    }

    redirectToLogin() {
        window.location.href = '/login/login.html';
    }

    bindEvents() {
        const logoutBtn = document.getElementById('nav-logout-btn');
        logoutBtn.addEventListener('click', () => this.logout());
    }

    async logout() {
        const accessToken = localStorage.getItem('accessToken');

        try {
             await fetch('http://localhost:8080/api/auth/logout', {
                 method: 'GET',
                 headers: {
                     'Authorization': `Bearer ${accessToken}`,
                     'Content-Type': 'application/json'
                 }
             });
             // 2. 로컬 스토리지 정리
             localStorage.clear();

             // 3. Google 로그아웃도 수행
             window.location.href = 'https://accounts.google.com/logout?continue=' +
                                       encodeURIComponent('http://localhost:8080/login/login.html');
        } catch (error) {
            console.error('로그아웃 API 호출 실패:', error);
        } finally {
            // 로컬 스토리지 정리
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('provider');
            localStorage.removeItem('loginTime');

            // 로그인 페이지로 이동
            this.redirectToLogin();
        }
    }

    setActiveLink() {
        const currentPath = window.location.pathname;
        const navLinks = document.querySelectorAll('.nav-link');

        navLinks.forEach(link => {
            link.classList.remove('active');
            const href = link.getAttribute('href');
            if (href === currentPath ||
                (currentPath.includes('/chatbot/') && href.includes('/chatbot/')) ||
                (currentPath === '/index.html' && href === '/index.html') ||
                (currentPath === '/' && href === '/index.html')) {
                link.classList.add('active');
            }
        });
    }

    // 인증된 fetch 함수 (다른 페이지에서 사용 가능)
    static async authenticatedFetch(url, options = {}) {
        const accessToken = localStorage.getItem('accessToken');

        if (!accessToken) {
            window.location.href = '/login/login.html';
            return;
        }

        const defaultHeaders = {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json'
        };

        const finalOptions = {
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers
            }
        };

        try {
            const response = await fetch(url, finalOptions);

            if (response.status === 401) {
                // 토큰 만료
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('provider');
                window.location.href = '/login/login.html';
                return;
            }

            return response;
        } catch (error) {
            console.error('Authenticated fetch error:', error);
            throw error;
        }
    }

    // 현재 토큰 가져오기 (다른 페이지에서 사용)
    static getCurrentToken() {
        return localStorage.getItem('accessToken');
    }
}

// 페이지 로드 시 네비게이션 초기화
document.addEventListener('DOMContentLoaded', () => {
    new Navigation();
});

// 전역에서 사용 가능하도록 export
window.Navigation = Navigation;