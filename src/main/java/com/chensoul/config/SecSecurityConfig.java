/*
 * Copyright © 2023-2024 chensoul.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chensoul.config;

import com.chensoul.persistence.dao.UserRepository;
import com.chensoul.security.CustomRememberMeServices;
import com.chensoul.security.google2fa.CustomAuthenticationProvider;
import com.chensoul.security.google2fa.CustomWebAuthenticationDetailsSource;
import com.chensoul.security.location.DifferentLocationChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecSecurityConfig {
    private final UserDetailsService userDetailsService;
    private final AuthenticationSuccessHandler myAuthenticationSuccessHandler;
    private final LogoutSuccessHandler myLogoutSuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final CustomWebAuthenticationDetailsSource authenticationDetailsSource;
    private final UserRepository userRepository;
    private final DifferentLocationChecker differentLocationChecker;

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(authProvider())
                .build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(new AntPathRequestMatcher("/resources/**", "/h2/**"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .securityContext((securityContext) -> securityContext.requireExplicitSave(true))
                .authorizeHttpRequests(authz -> {
                    authz.requestMatchers(HttpMethod.GET, "/roleHierarchy")
                            .hasRole("STAFF")
                            .requestMatchers("/login*", "/logout*", "/signin/**", "/signup/**", "/customLogin", "/user/registration*", "/registrationConfirm*", "/expiredAccount*", "/registration*", "/badUser*", "/user/resendRegistrationToken*", "/forgetPassword*",
                                    "/user/resetPassword*", "/user/savePassword*", "/updatePassword*", "/user/changePassword*", "/emailError*", "/resources/**", "/old/user/registration*", "/successRegister*", "/qrcode*", "/user/enableNewLoc*")
                            .permitAll()
                            .requestMatchers("/invalidSession*").anonymous()
                            .requestMatchers("/user/updatePassword*").hasAuthority("CHANGE_PASSWORD_PRIVILEGE")
                            .requestMatchers("/console")
                            .hasAuthority("READ_PRIVILEGE")
                            .anyRequest()
                            .hasAuthority("READ_PRIVILEGE");
                })
                .formLogin((formLogin) -> formLogin.loginPage("/login")
                        .defaultSuccessUrl("/homepage.html")
                        .failureUrl("/login?error=true")
                        .successHandler(myAuthenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .authenticationDetailsSource(authenticationDetailsSource)
                        .permitAll())
                .sessionManagement((sessionManagement) -> sessionManagement.invalidSessionUrl("/invalidSession.html")
                        .maximumSessions(1)
                        .sessionRegistry(sessionRegistry()))
                .logout((logout) -> logout.logoutSuccessHandler(myLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .logoutSuccessUrl("/logout.html?logSucc=true")
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .rememberMe((remember) -> remember.rememberMeServices(rememberMeServices()));

        return http.build();
    }

    // beans

    @Bean
    public DaoAuthenticationProvider authProvider() {
        final CustomAuthenticationProvider authProvider = new CustomAuthenticationProvider(userRepository);
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setPostAuthenticationChecks(differentLocationChecker);
        return authProvider;
    }

    @Bean
    public RememberMeServices rememberMeServices() {
        CustomRememberMeServices rememberMeServices = new CustomRememberMeServices("theKey", userDetailsService, new InMemoryTokenRepositoryImpl(), userRepository);
        return rememberMeServices;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
