package backend.synGo.config.jwt;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.auth.form.TokenType;
import backend.synGo.exception.ExpiredTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.Long.*;
import static org.springframework.util.StringUtils.*;


@Slf4j
@Component
public class JwtProvider{
    //서명 키
    private final Key key;
    @Qualifier("jwtRedisTemplate")
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${security.jwt.access-token.expiration}")
    private long accessTokenMinutes;
    @Value("${security.jwt.refresh-token.expiration}")
    private long refreshTokenDay;

    public JwtProvider(
            @Value("${security.secret-key}") String secretKey, //접속 키
            RedisTemplate<String,String> redisTemplate) {
        //Base64로 디코딩
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        //HMAC-SHA256 방식으로 키 생성
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.redisTemplate = redisTemplate;
        log.info("JWT Secret Key Initialized Successfully!");
    }

    // AccessToken 생성 메서드
    public String createAccessToken(CustomUserDetails user) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + accessTokenMinutes * 60 * 1000);
        String accessToken = createToken(now, expiredAt, user)
                .claim("token_type", "access")
                .compact();;

        redisTemplate.opsForValue().set(
                "AT:" + user.getUserId(), //key=AT:userId
                accessToken,    // value=token
                Duration.ofMinutes(accessTokenMinutes)
        );
        return accessToken;
    }

    // RefreshToken 생성 메서드
    public String createRefreshToken(CustomUserDetails user) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + refreshTokenDay * 24 * 60 * 60 * 1000);
        String refreshToken = createToken(now, expiredAt, user)
                .claim("token_type", "refresh")
                .compact();

        //Redis에 저장 (key = RT:userId, value = 토큰)
        redisTemplate.opsForValue().set(
                "RT:" + user.getUserId(),     //토큰 관련 키를 쉽게 찾기 위함
                refreshToken,
                Duration.ofDays(refreshTokenDay).toMillis(),
                TimeUnit.MILLISECONDS
        );
        return refreshToken;
    }

    // JWT 생성 공통 로직 (Access, Refresh)
    private JwtBuilder createToken(Date now, Date expiredAt, CustomUserDetails  user) {
        return Jwts.builder()
                .issuer("SynGo")
                .issuedAt(now)
                .notBefore(now) // 추가: 유효 시작 시간
                .expiration(expiredAt)
                .subject(String.valueOf(user.getUserId()))
                .claim("username", user.getName())
                .claim("user_ip", user.getLastAccessIp())
                .id(UUID.randomUUID().toString()) // 추가: 고유 ID
                .signWith(key);
    }

    // JWT Token 타입 별 유효성 검증
    public boolean validateToken(String token, TokenType tokenType) {
        try {
            log.info("validateToken={}", token);
            // 기본 서명 검증
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            switch (tokenType) {
                case BL -> { //black_list 로 저장된 token
                    String isBlacklisted = redisTemplate.opsForValue().get("BL:" + token);
                    if (isBlacklisted.equals("\"logout\"")) {
                        log.warn("블랙리스트에 등록된 토큰입니다.");
                        return false;
                    }
                    return true;
                }
                case RT -> { //refresh_token 으로 저장된 토큰
                    String storedRefreshToken = getRefreshToken(parseLong(claims.getSubject()));
                    if (!hasText(storedRefreshToken)) {
                        log.warn("Redis에 존재하지 않는 Refresh Token입니다.");
                        return false;
                    }
                    return true;
                }
                case AT -> { //access_token 으로 저장된 토큰
                    String storedAccessToken = getAccessToken(parseLong(claims.getSubject()));
                    if (!hasText(storedAccessToken)) {
                        log.warn("Redis에 존재하지 않는 Access Token입니다.");
                        return false;
                    }
                    return true;
                }
                case TOKEN -> { //refresh,access 으로 저장된 토큰
                    String storedBlacklisted = redisTemplate.opsForValue().get("BL:" + token);
                    log.info("storedBlacklisted={}", storedBlacklisted);
                    String storedAccessToken = getAccessToken(parseLong(claims.getSubject()));
                    String storedRefreshToken = getRefreshToken(parseLong(claims.getSubject()));
                    if ("logout".equals(storedBlacklisted)) {
                        log.warn("블랙리스트에 등록된 토큰입니다.");
                        return false;
                    }
                    if (hasText(storedAccessToken) || hasText(storedRefreshToken)){
                        log.info("유효한 토큰입니다.");
                        return true;
                    }
                    log.warn("사용할 수 없는 토큰입니다.");
                    return false;
                }
                default -> {
                    log.warn("지원되지 않는 TokenType입니다: {}", tokenType);
                    return false;
                }
            }
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("토큰 만료");
        } catch (JwtException e) {
            log.warn("JWT 검증 실패: {}", e.getMessage());
        }
        log.info("validateToken=false");
        return false;
    }

    // JWT에서 Claims 데이터 가져오기
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload(); // Payload(Claims) 부분 반환
    }

    // JWT 토큰에서 인증 정보 추출 (Spring Security 연동)
    public Authentication getAuthentication(String token) {
        // 1. 토큰에서 Claims(클레임) 정보를 꺼낸다
        Claims claims = getClaims(token);

        // 2. token을 통해 userDtails 생성
        CustomUserDetails userDetails = new CustomUserDetails(
                parseLong(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("user_ip", String.class)
        );
        // 3. UserDetails를 바탕으로 인증 토큰 생성
        //    - principal: userDetails (인증된 사용자 정보)
        //    - credentials: 빈 문자열("") (비밀번호 같은 민감 정보는 토큰에 넣지 않음)
        //    - authorities: userDetails.getAuthorities() (권한 목록 "ROLE BASIC" 으로 설정)
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    // 헤더에서 토큰을 가져옴
    public String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7).trim(); // 토큰의 "Bearer " 제거 후 반환
        }
        return null;
    }

    // Redis에서 RefreshToken 조회
    public String getRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get("RT:" + userId);
    }

    // Redis에서 Access 조회
    public String getAccessToken(Long userId) {
        return redisTemplate.opsForValue().get("AT:" + userId);
    }

    // Redis에서 RefreshToken 삭제
    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete("RT:" + userId);
    }

    // Redis에서 AccessToken 삭제
    public void deleteAccessToken(Long userId) {
        redisTemplate.delete("AT:" + userId);
    }

    // 블랙리스트 추가
    public void blackListToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long expiration = claims.getExpiration().getTime() - System.currentTimeMillis();
        log.info("남은 expiration={}", expiration);
        redisTemplate.opsForValue().set("BL:" + token, "logout", expiration, TimeUnit.MILLISECONDS);
    }
}