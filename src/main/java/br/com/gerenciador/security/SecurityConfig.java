package br.com.gerenciador.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.time.Clock;

@Configuration
public class SecurityConfig {

    // Só recursos do próprio sistema. style-src-attr libera o atributo style (cor do selo da prioridade,
    // sempre #RRGGBB validado), sem liberar <style> nem scripts embutidos.
    private static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "style-src-attr 'unsafe-inline'; img-src 'self' data:; font-src 'self'; form-action 'self'; "
            + "frame-ancestors 'none'; base-uri 'self'; object-src 'none'";

    @Bean
    SecurityFilterChain filtroSeguranca(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
                // A autorização é sempre conferida aqui no servidor, mesmo que a tela esconda o link.
                .authorizeHttpRequests(acesso -> acesso
                        .requestMatchers("/login", "/cadastro", "/tema", "/css/**", "/temas/**", "/webjars/**", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/tarefas")
                        // Login bloqueado ganha mensagem própria; qualquer outro erro mostra a mensagem genérica.
                        .failureHandler((requisicao, resposta, erro) -> resposta.sendRedirect(
                                requisicao.getContextPath() + (erro instanceof LockedException ? "/login?bloqueado" : "/login?erro"))))
                .logout(logout -> logout.logoutSuccessUrl("/login?saiu"))
                .sessionManagement(sessao -> sessao.sessionConcurrency(concorrencia -> concorrencia
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/login?expirada")))
                .headers(cabecalhos -> cabecalhos
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
                .addFilterBefore(new LimiteTentativasFilter(Clock.systemUTC()), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Hash com BCrypt. O formato "{bcrypt}..." permite trocar de algoritmo no futuro sem invalidar senhas. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Avisa o registro de sessões quando uma sessão termina, para ele não guardar sessões mortas. */
    @Bean
    static HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
