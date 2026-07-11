package com.group28.leaderboard.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/style.css", "/leaderboard.js").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/", "/leaderboard", "/api/leaderboard").authenticated()
                .requestMatchers("/judge", "/judge/**").hasAnyRole("JUDGE", "ADMIN")
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder encoder) {
        String pw = encoder.encode("1234");
        return new InMemoryUserDetailsManager(
            User.builder().username("participant1").password(pw).roles("PARTICIPANT").build(),
            User.builder().username("participant2").password(pw).roles("PARTICIPANT").build(),
            User.builder().username("participant3").password(pw).roles("PARTICIPANT").build(),
            User.builder().username("judge1").password(pw).roles("JUDGE").build(),
            User.builder().username("judge2").password(pw).roles("JUDGE").build(),
            User.builder().username("judge3").password(pw).roles("JUDGE").build(),
            User.builder().username("admin").password(pw).roles("ADMIN").build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
