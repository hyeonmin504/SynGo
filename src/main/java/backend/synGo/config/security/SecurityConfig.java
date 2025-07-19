package backend.synGo.config.security;

import backend.synGo.auth.oauth2.OAuth2AuthorizationFailureHandler;
import backend.synGo.auth.oauth2.OAuth2AuthorizationSuccessHandler;
import backend.synGo.auth.service.CustomOAuth2UserService;
import backend.synGo.auth.util.CustomAuthenticationEntryPoint;
import backend.synGo.config.jwt.JwtAuthenticationFilter;
import backend.synGo.config.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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
    // OAuth2 관련 빈들 추가
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthorizationSuccessHandler oAuth2AuthorizationSuccessHandler;
    private final OAuth2AuthorizationFailureHandler oAuth2AuthorizationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("start filter");
        //인증 없이 접근 가능한 url
        List<String> permitUrls = List.of(
                "/api/my/chatbot/stream","/oauth/**",
                "/favicon_io/favicon.ico", "/login/**", "/nav/**", "/oauth2/**", "/actuator/**", "/index.html",
                "/main.css", "/app.js", "/chatbot/**", "/", "/ws-stomp/**", "/topic/**", "/sub/**", "/pub/**",
                "/api/auth/login", "/api/auth/signup", "/swagger-ui/**", "/v3/api-docs/**"
        );

        http
                //요청 url 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/my/chatbot/stream","/oauth/**",
                                "/favicon_io/favicon.ico", "/login/**", "/nav/**", "/oauth2/**", "/actuator/**", "/index.html",
                                "/main.css", "/app.js", "/chatbot/**", "/", "/ws-stomp/**", "/topic/**", "/sub/**", "/pub/**",
                                "/api/auth/login", "/api/auth/signup", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, permitUrls),
                        UsernamePasswordAuthenticationFilter.class
                )
                // OAuth2 로그인 설정 추가
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthorizationSuccessHandler)
                        .failureHandler(oAuth2AuthorizationFailureHandler)
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
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
