package backend.synGo.config.security;

import backend.synGo.auth.util.CustomAuthenticationEntryPoint;
import backend.synGo.config.jwt.JwtAuthenticationFilter;
import backend.synGo.config.jwt.JwtProvider;
import backend.synGo.config.sse.SseAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Component
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    //private final SseAuthenticationFilter sseAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("start filter");
        //인증 없이 접근 가능한 url
        List<String> permitUrls = List.of(
                "/api/my/chatbot/stream",
                "/index.html",
                "/chatbot/**",
                "/styles.css",
                "/script.js",
                "/",
                "/ws-stomp/**",
                "/topic/**",
                "/sub/**", // ← 메시지 브로커 구독 경로
                "/pub/**", // ← 메시지 발행 경로
                "/api/auth/login",
                "/api/auth/signup",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/error" // 에러 페이지도 permitAll에 추가
        );

        http
                //요청 url 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/my/chatbot/stream",
                                "/index.html",
                                "/chatbot/**",
                                "/styles.css",
                                "/script.js",
                                "/",
                                "/ws-stomp/**",
                                "/topic/**",
                                "/sub/**", // ← 메시지 브로커 구독 경로
                                "/pub/**", // ← 메시지 발행 경로
                                "/api/auth/login",
                                "/api/auth/signup",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint))
                // JWT 필터만 사용 (SseAuthenticationFilter 제거)
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, permitUrls),
                        UsernamePasswordAuthenticationFilter.class
                )
                .csrf(AbstractHttpConfigurer::disable) // csrf 보호 비활성화 (JWT 사용 시 비활성화)
                .formLogin(AbstractHttpConfigurer::disable) // 스프링 시큐리티 기본 로그인 폼 비활성화
                .logout(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())); // ✅ 여기 주목
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // 또는 정확한 origin 명시
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
