package br.com.gerenciador.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita, por IP, os envios do login e da criação de conta: barra robôs que testam senhas em muitas contas ou
 * criam contas em massa (o bloqueio de cada conta continua em BloqueioLogin). Os limites são folgados porque
 * várias pessoas podem sair pelo mesmo IP, como na rede de uma faculdade.
 */
public class LimiteTentativasFilter extends OncePerRequestFilter {

    private static final Map<String, Limite> LIMITES = Map.of(
            "/login", new Limite(30, Duration.ofMinutes(5)),
            "/cadastro", new Limite(20, Duration.ofMinutes(10)));
    // Acima disso, as janelas vencidas são descartadas, para muitos IPs diferentes não encherem a memória.
    private static final int JANELAS_ANTES_DA_LIMPEZA = 10_000;

    private final Clock relogio;
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();

    public LimiteTentativasFilter(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        return !"POST".equals(requisicao.getMethod()) || !LIMITES.containsKey(caminho(requisicao));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        String caminho = caminho(requisicao);
        // Em produção o IP real vem do proxy do Render (server.forward-headers-strategy=native).
        if (!permitir(caminho + " " + requisicao.getRemoteAddr(), LIMITES.get(caminho))) {
            resposta.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private boolean permitir(String chave, Limite limite) {
        Instant agora = relogio.instant();
        if (janelas.size() >= JANELAS_ANTES_DA_LIMPEZA) {
            janelas.values().removeIf(janela -> janela.venceu(agora));
        }
        Janela janela = janelas.compute(chave, (k, atual) -> atual == null || atual.venceu(agora)
                ? new Janela(agora.plus(limite.duracao()), 1)
                : new Janela(atual.fim(), atual.tentativas() + 1));
        return janela.tentativas() <= limite.maximo();
    }

    private static String caminho(HttpServletRequest requisicao) {
        return requisicao.getRequestURI().substring(requisicao.getContextPath().length());
    }

    private record Limite(int maximo, Duration duracao) {
    }

    private record Janela(Instant fim, int tentativas) {

        boolean venceu(Instant agora) {
            return !fim.isAfter(agora);
        }
    }
}
