package antifraud;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.httpBasic()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint()) // Handles auth error
                .and()
                .csrf().disable().headers().frameOptions().disable() // for Postman, the H2 console
                .and()
                .authorizeRequests() // manage access
                .requestMatchers(HttpMethod.POST, "/api/auth/user").permitAll()
                .requestMatchers("/actuator/shutdown").permitAll() // needs to run test
             //   .requestMatchers("/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/auth/list/**").hasAnyRole(User.Role.ADMINISTRATOR.name(),
                        User.Role.SUPPORT.name())               // other matchers
                .requestMatchers(HttpMethod.POST, "/api/antifraud/transaction").hasRole(User.Role.MERCHANT.name())
                .requestMatchers(HttpMethod.PUT, "/api/antifraud/transaction").hasRole(User.Role.SUPPORT.name())
                .requestMatchers(HttpMethod.PUT, "/api/auth/access/**").hasRole(User.Role.ADMINISTRATOR.name())
                .requestMatchers(HttpMethod.PUT, "/api/auth/role").hasRole(User.Role.ADMINISTRATOR.name())
                .requestMatchers(HttpMethod.DELETE, "/api/auth/user/*").hasRole(User.Role.ADMINISTRATOR.name())
                .requestMatchers(HttpMethod.POST, "/api/antifraud/suspicious-ip").hasRole(User.Role.SUPPORT.name())
                .requestMatchers(HttpMethod.GET, "/api/antifraud/suspicious-ip").hasRole(User.Role.SUPPORT.name())
                .requestMatchers(HttpMethod.DELETE, "/api/antifraud/suspicious-ip/*").hasRole(User.Role.SUPPORT.name())
                .requestMatchers(HttpMethod.POST, "/api/antifraud/stolencard").hasRole(User.Role.SUPPORT.name())
                .requestMatchers(HttpMethod.GET, "/api/antifraud/stolencard").hasRole(User.Role.SUPPORT.name())
                .requestMatchers(HttpMethod.DELETE, "/api/antifraud/stolencard/*").hasRole(User.Role.SUPPORT.name())
                .requestMatchers(HttpMethod.GET, "/api/antifraud/history/**").hasRole(User.Role.SUPPORT.name())
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS); // no session
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
