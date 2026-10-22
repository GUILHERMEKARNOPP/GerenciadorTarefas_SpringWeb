package br.com.gerenciador.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteTentativasFilterTest {

    private final RelogioDeTeste relogio = new RelogioDeTeste();
    private final LimiteTentativasFilter filtro = new LimiteTentativasFilter(relogio);

    @Test
    void bloqueiaOIpQuePassaDoLimiteDeLogin() throws Exception {
        for (int tentativa = 1; tentativa <= 30; tentativa++) {
            assertThat(enviar("POST", "/login", "200.1.1.1")).isEqualTo(200);
        }
        assertThat(enviar("POST", "/login", "200.1.1.1")).isEqualTo(429);
    }

    @Test
    void cadaIpTemOProprioLimite() throws Exception {
        for (int conta = 1; conta <= 20; conta++) {
            enviar("POST", "/cadastro", "200.1.1.1");
        }
        assertThat(enviar("POST", "/cadastro", "200.1.1.1")).isEqualTo(429);
        assertThat(enviar("POST", "/cadastro", "200.2.2.2")).isEqualTo(200);
    }

    @Test
    void liberaDeNovoQuandoAJanelaTermina() throws Exception {
        for (int tentativa = 1; tentativa <= 31; tentativa++) {
            enviar("POST", "/login", "200.1.1.1");
        }
        relogio.avancar(Duration.ofMinutes(5));

        assertThat(enviar("POST", "/login", "200.1.1.1")).isEqualTo(200);
    }

    @Test
    void naoLimitaOutrasPaginas() throws Exception {
        for (int vez = 1; vez <= 100; vez++) {
            assertThat(enviar("GET", "/login", "200.1.1.1")).isEqualTo(200);
            assertThat(enviar("POST", "/tarefas/nova", "200.1.1.1")).isEqualTo(200);
        }
    }

    private int enviar(String metodo, String caminho, String ip) throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest(metodo, caminho);
        requisicao.setRemoteAddr(ip);
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        filtro.doFilter(requisicao, resposta, new MockFilterChain());
        return resposta.getStatus();
    }

    private static final class RelogioDeTeste extends Clock {

        private Instant agora = Instant.parse("2026-10-22T12:00:00Z");

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }
}
