package backend.synGo.config.jwt;

import backend.synGo.auth.CustomUserDetails;
import backend.synGo.auth.CustomUserDetailsService;
import backend.synGo.domain.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class JwtProvider{
    private final Key key;
    private final RedisTemplate<String, String> redisTemplate;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${security.jwt.access-token.expiration}")
    private long accessTokenMinutes;
    @Value("${security.jwt.refresh-token.expiration}")
    private long refreshTokenDay;

    public JwtProvider(@Value("${security.jwt.secret-key}") String secretKey, RedisTemplate<String,String> redisTemplate, CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
        //Base64로 디코딩
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        //HMAC-SHA256 방식으로 키 생성
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.redisTemplate = redisTemplate;
        log.info("JWT Secret Key Initialized Successfully!");
    }

    // AccessToken 생성 메서드
    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + accessTokenMinutes * 60 * 1000);
        String accessToken = createToken(now, expiredAt, user);

        redisTemplate.opsForValue().set(
                "AT:" + user.getId(),
                accessToken,
                Duration.ofMinutes(accessTokenMinutes).toMillis(),
                TimeUnit.MILLISECONDS
        );

        return accessToken;
    }

    // RefreshToken 생성 메서드
    public String createRefreshToken(User user) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + refreshTokenDay * 24 * 60 * 60 * 1000);
        String refreshToken = Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(now)
                .setExpiration(expiredAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        //Redis에 저장 (key = RT:(user 이름), value = 토큰)
        redisTemplate.opsForValue().set(
                "RT:" + user.getId(),     //토큰 관련 키를 쉽게 찾기 위함
                refreshToken,
                Duration.ofDays(refreshTokenDay).toMillis(),
                TimeUnit.MILLISECONDS
        );

        return refreshToken;
    }

    // JWT 생성 공통 로직 (Access, Refresh)
    private String createToken(Date now, Date expiredAt, User user) {
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE) //헤더 타입 명시
                .setIssuer("SynGo") // 발급자
                .setIssuedAt(now) // 발급 시간
                .setExpiration(expiredAt) // 만료 시간
                .setSubject(String.valueOf(user.getId())) // 주제(사용자 Id)
                .claim("username", user.getName()) // 사용자 이름
                .claim("user_ip", user.getLastAccessIp()) // 사용자 마지막 IP
                .signWith(key, SignatureAlgorithm.HS256) // 서명
                .compact();
    }

    // JWT 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token); // 검증 및 파싱
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 만료={}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT 검증 실패={}", e.getMessage());
        }
        return false;
    }

    // JWT에서 Claims 데이터 가져오기
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody(); // Payload(Claims) 부분 반환
    }

    // JWT 토큰에서 인증 정보 추출 (Spring Security 연동)
    public Authentication getAuthentication(String token) {
        // 1. 토큰에서 Claims(클레임) 정보를 꺼낸다
        Claims claims = getClaims(token);
        // 2. 클레임에서 "user_name"이라는 키로 저장된 사용자 이름을 문자열로 추출
        String userId = claims.getSubject(); // userId 추출
        // 3. Spring Security에서 제공하는 UserDetails 구현체를 생성
        //    - username: 방금 꺼낸 사용자 이름
        //    - password: 빈 문자열("") (여기선 비밀번호가 필요 없으므로 빈 문자열)
        //          cf) JWT 토큰 자체가 인증된 증거(token)이므로, 이 시점에서 비밀번호는 필요하지 않습니다.
        //    - authorities(권한): 빈 리스트 (List.of()) — 권한이 없다고 가정
        //          ex) ROLE_USER, ROLE_ADMIN
        CustomUserDetails userDetails =
                customUserDetailsService.loadUserByUsername(userId);

        if (!userDetails.getUserLastAccessIp().equals(claims.get("user_ip",String.class))){
            //todo: ip 접속 환경이 다를 경우 어떻게 처리 할 것인지.
        }

        // 4. UserDetails를 바탕으로 인증 토큰 생성
        //    - principal: userDetails (인증된 사용자 정보)
        //    - credentials: 빈 문자열("") (비밀번호 같은 민감 정보는 토큰에 넣지 않음)
        //    - authorities: userDetails.getAuthorities() (권한 목록)
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    // Redis에서 RefreshToken 조회
    public String getRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get("RT:" + userId);
    }

    // Redis에서 RefreshToken 삭제
    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete("RT:" + userId);
    }
}
