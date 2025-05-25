package backend.synGo.config;

import backend.synGo.auth.util.CustomAuthenticationEntryPoint;
import backend.synGo.config.jwt.JwtAuthenticationFilter;
import backend.synGo.config.jwt.JwtProvider;
import backend.synGo.exception.handler.GlobalExceptionHandler;
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

import java.util.List;

@Slf4j
@Component
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("start filter");
        //인증 없이 접근 가능한 url
        List<String> permitUrls = List.of(
                "/",
                "/api/auth/login",
                "/api/auth/signup",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        );

        http
                //요청 url 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api/auth/login", "/api/auth/signup", "/swagger-ui/**","/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint))
                //UsernamePasswordAuthenticationFilter 이전에 JWT 필터 삽입
                .addFilterBefore(   //
                        new JwtAuthenticationFilter(jwtProvider, permitUrls, new GlobalExceptionHandler()),
                        UsernamePasswordAuthenticationFilter.class
                )
                .csrf(AbstractHttpConfigurer::disable) // csrf 보호 비활성화 (JWT 사용 시 비활성화)
                .formLogin(AbstractHttpConfigurer::disable) // 스프링 시큐리티 기본 로그인 폼 비활성화
                .logout(AbstractHttpConfigurer::disable); // 스프링 시큐리티 기본 로그 아웃 폼 비활성화

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
