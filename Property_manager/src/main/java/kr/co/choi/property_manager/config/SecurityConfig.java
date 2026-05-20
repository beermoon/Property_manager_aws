package kr.co.choi.property_manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정.
 *
 * <p>두 개의 {@link SecurityFilterChain}을 분리해서 등록한다:
 * <ul>
 *   <li><b>API 체인</b> (@Order(1)) — {@code /api/**} → HTTP Basic + Stateless + CSRF off</li>
 *   <li><b>MVC 체인</b> (@Order(2)) — 그 외 → 폼 로그인 + 세션 + CSRF on</li>
 * </ul>
 *
 * <p>{@code @Order} 숫자가 낮을수록 먼저 매칭된다. {@code securityMatcher}로
 * 각 체인이 담당할 URL 패턴을 한정한다.
 *
 * <p>사용자 정보는 환경변수 {@code APP_USERNAME}, {@code APP_PASSWORD}로
 * 주입받아 메모리에 단일 사용자만 등록한다.
 * 비밀번호는 부팅 시 BCrypt로 해시화되므로, 환경변수에는 평문을 둔다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String username;
    private final String password;

    public SecurityConfig(
            @Value("${app.security.username}") String username,
            @Value("${app.security.password}") String password) {
        this.username = username;
        this.password = password;
    }

    // ============================================================
    //   사용자 / 비밀번호 인코더
    // ============================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user = User.builder()
                .username(username)
                .password(encoder.encode(password))    // 부팅 시점에 해시
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    // ============================================================
    //   API 체인 — /api/**
    // ============================================================

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                // REST는 stateless — 매 요청 자격증명 헤더로 인증
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF는 stateless API에 불필요 (토큰 없음, 세션 없음)
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    // ============================================================
    //   MVC 체인 — 그 외 모든 URL
    // ============================================================

    @Bean
    @Order(2)
    public SecurityFilterChain mvcSecurityChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스와 업로드된 이미지는 인증 없이 접근
                        .requestMatchers("/css/**", "/js/**", "/upload/**", "/uploads/**").permitAll()
                        // 로그인 페이지는 누구나
                        .requestMatchers("/login", "/error").permitAll()
                        // 그 외 모든 요청은 로그인 필요
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/properties", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );
        // CSRF는 기본 활성 (폼은 Thymeleaf 자동 토큰 주입)
        return http.build();
    }
}
