class Navigation {
    constructor() {
        this.currentUser = null;
        this.socialInfo = null;
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
                    <!-- 소셜 연동 버튼 추가 -->
                    <div id="social-link-section" class="social-link-section" style="display: none;">
                        <button class="social-link-btn" id="google-link-btn">
                            <img src="https://developers.google.com/identity/images/g-logo.png" alt="Google" style="width: 16px; height: 16px;">
                            구글 연동
                        </button>
                    </div>
                    <button class="logout-btn" id="nav-logout-btn">로그아웃</button>
                </div>
            </div>
        `;

        // body의 첫 번째 자식으로 추가
        document.body.insertBefore(nav, document.body.firstChild);
    }

    async loadUserInfo() {
        // ✅ OAuth 콜백 처리 먼저 확인
        const urlParams = new URLSearchParams(window.location.search);
        const oauthCode = urlParams.get('code');
        const oauthState = urlParams.get('state');

        // OAuth 콜백인 경우 연동 처리
        if (oauthCode && oauthState && oauthState.startsWith('link_')) {
            await this.handleOAuthCallback(oauthCode, oauthState);
            return;
        }

        // 기존 OAuth 결과 확인 (소셜 회원가입/로그인용)
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

        // ✅ 소셜 연동 정보 로드
        await this.loadSocialInfo();
    }

    async handleOAuthCallback(code, state) {
        try {
            console.log('OAuth 콜백 처리 중...', { code: code.substring(0, 10) + '...', state });

            // 저장된 토큰 확인
            const accessToken = localStorage.getItem('accessToken');
            if (!accessToken) {
                alert('로그인 상태가 아닙니다. 다시 로그인해주세요.');
                this.redirectToLogin();
                return;
            }

            // 현재 페이지 URL을 returnUrl로 전달
            const currentUrl = window.location.origin + window.location.pathname;

            // 연동 API 호출
            const response = await fetch(`http://localhost:8080/api/auth/link-google?returnUrl=${encodeURIComponent(currentUrl)}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    code: code,
                    state: state
                })
            });

            if (response.ok) {
                const result = await response.json();
                console.log('구글 계정 연동 성공:', result);

                // 성공 메시지 표시
                alert(`구글 계정 연동 완료!\n연동된 이메일: ${result.linkedEmail}`);

                // URL 파라미터 제거하고 현재 페이지 유지
                const cleanUrl = window.location.pathname;
                window.history.replaceState({}, document.title, cleanUrl);

                // 소셜 정보 다시 로드
                await this.loadSocialInfo();

            } else {
                const errorData = await response.json().catch(() => ({ message: '연동 실패' }));
                throw new Error(errorData.message || '구글 계정 연동에 실패했습니다.');
            }

        } catch (error) {
            console.error('OAuth 콜백 처리 중 오류:', error);
            alert('구글 계정 연동 중 오류가 발생했습니다: ' + error.message);

            // 원래 페이지 유지 (파라미터만 제거)
            const cleanUrl = window.location.pathname;
            window.history.replaceState({}, document.title, cleanUrl);
        }
    }

    async loadSocialInfo() {
        try {
            const response = await Navigation.authenticatedFetch('http://localhost:8080/api/auth/social-info');

            if (response && response.ok) {
                this.socialInfo = await response.json();
                console.log('소셜 정보 로드됨:', this.socialInfo);
                this.updateSocialLinkDisplay();
            } else {
                console.warn('소셜 정보 로드 실패');
                this.socialInfo = null;
                this.showSocialLinkButton();
            }
        } catch (error) {
            console.error('소셜 정보 로드 중 오류:', error);
            this.socialInfo = null;
            this.showSocialLinkButton();
        }
    }

    updateSocialLinkDisplay() {
        const socialSection = document.getElementById('social-link-section');

        if (this.socialInfo && this.socialInfo.isLinked) {
            // 이미 연동된 경우 - 소셜 정보를 사용자 표시에 포함
            this.displayLinkedSocialInfo();
            socialSection.style.display = 'none';
        } else {
            // 연동되지 않은 경우 - 연동 버튼 표시
            this.showSocialLinkButton();
        }
    }

    displayLinkedSocialInfo() {
        const userDisplay = document.getElementById('user-display');
        const social = this.socialInfo;

        userDisplay.innerHTML = `
            <div style="display: flex; align-items: center; gap: 0.5rem;">
                ${social.profileImageUrl ?
                    `<img src="${social.profileImageUrl}" alt="Profile" class="user-avatar">` :
                    '<div class="user-avatar" style="background: rgba(255,255,255,0.3); display: flex; align-items: center; justify-content: center;">👤</div>'
                }
                <div style="display: flex; flex-direction: column; align-items: flex-start;">
                    <span class="user-name">사용자</span>
                    <div style="display: flex; align-items: center; gap: 0.3rem; font-size: 0.75rem; opacity: 0.8;">
                        <img src="https://developers.google.com/identity/images/g-logo.png" alt="Google" style="width: 12px; height: 12px;">
                        <span>연동됨</span>
                        ${social.isExpired ? '<span style="color: #ffcccb;">만료</span>' : ''}
                    </div>
                </div>
            </div>
        `;
    }

    showSocialLinkButton() {
        const socialSection = document.getElementById('social-link-section');
        const storedProvider = localStorage.getItem('provider');

        // LOCAL 사용자이고 아직 연동되지 않은 경우에만 소셜 연동 버튼 표시
        if ((storedProvider === 'LOCAL' || !storedProvider) &&
            (!this.socialInfo || !this.socialInfo.isLinked)) {
            socialSection.style.display = 'flex';
        } else {
            socialSection.style.display = 'none';
        }
    }

    async linkGoogleAccount() {
        try {
            console.log('구글 계정 연동 시작...');

            // 1. 현재 페이지 URL을 포함해서 구글 OAuth URL 생성 요청
            const currentUrl = encodeURIComponent(window.location.href);
            const response = await Navigation.authenticatedFetch(
                `http://localhost:8080/api/auth/google-oauth-url?returnUrl=${currentUrl}`
            );

            if (response && response.ok) {
                const oauthUrl = await response.text();
                console.log('구글 OAuth URL:', oauthUrl);

                // 2. 현재 창에서 구글 OAuth 페이지로 이동
                window.location.href = oauthUrl;

            } else {
                throw new Error('OAuth URL 생성 실패');
            }
        } catch (error) {
            console.error('구글 계정 연동 중 오류:', error);
            alert('구글 계정 연동 중 오류가 발생했습니다: ' + error.message);
        }
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

        // ✅ 구글 연동 버튼 이벤트
        const googleLinkBtn = document.getElementById('google-link-btn');
        if (googleLinkBtn) {
            googleLinkBtn.addEventListener('click', () => this.linkGoogleAccount());
        }
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