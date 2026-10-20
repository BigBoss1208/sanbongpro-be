package com.example.becicd.config;

import com.example.becicd.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Value("${app.cors-origins}")
    private String corsOrigins;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(corsOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().configurationSource(corsConfigurationSource())
                .and()
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()

                // Endpoints công khai
                .antMatchers("/api/auth/**", "/api/san/**", "/api/health", "/api/thanh-toan/vnpay/return", "/api/thanh-toan/vnpay/ipn").permitAll()

                // Endpoints Quản lý (Cho phép các quyền Quản trị & Nhân viên)
                .antMatchers("/api/quan-ly/**").hasAnyAuthority(
                        "ADMIN", "NHAN_VIEN_PAGE", "NHAN_VIEN_SAN",
                        "ROLE_ADMIN", "ROLE_NHAN_VIEN_PAGE", "ROLE_NHAN_VIEN_SAN"
                )

                .antMatchers("/api/nhan-vien/**").hasAnyAuthority(
                    "ROLE_ADMIN", "ROLE_NHAN_VIEN_PAGE", "ROLE_NHAN_VIEN_SAN"
                )

                // Endpoints dành riêng cho Admin
                .antMatchers("/api/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN")

                .anyRequest().authenticated();

        http.exceptionHandling()
            .authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(401); response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Vui lòng đăng nhập\"}");
            })
            .accessDeniedHandler((request, response, exception) -> {
                response.setStatus(403); response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Không có quyền truy cập\"}");
            });
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
