package br.com.gerenciador;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Endereço do banco em produção (variável WEB_DB_URL). Aceita o formato JDBC e também o que o Neon mostra
 * para copiar: postgresql://usuario:senha@host/banco?sslmode=require (até com o "psql '...'" na frente).
 */
record EnderecoBanco(String jdbcUrl, String usuario, String senha) {

    private static final String FORMATO_ESPERADO = "WEB_DB_URL deve ser jdbc:postgresql://HOST/BANCO?sslmode=require "
            + "ou o endereço do Neon, postgresql://USUARIO:SENHA@HOST/BANCO?sslmode=require";

    /** Usuário e senha escritos dentro do endereço têm prioridade sobre WEB_DB_USER e WEB_DB_PASSWORD. */
    static EnderecoBanco de(String endereco, String usuario, String senha) {
        String limpo = limpar(endereco);
        if (limpo.regionMatches(true, 0, "jdbc:", 0, 5)) {
            limpo = limpo.substring(5);
        }
        String minusculo = limpo.toLowerCase(Locale.ROOT);
        if (!minusculo.startsWith("postgresql://") && !minusculo.startsWith("postgres://")) {
            throw new IllegalStateException(FORMATO_ESPERADO + ".");
        }
        URI uri;
        try {
            uri = new URI(limpo);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(FORMATO_ESPERADO + " (o endereço informado não é válido).");
        }
        if (uri.getHost() == null) {
            throw new IllegalStateException(FORMATO_ESPERADO + " (faltou o HOST).");
        }
        String jdbc = "jdbc:postgresql://" + hostDireto(uri.getHost()) + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                + (uri.getRawPath() == null ? "" : uri.getRawPath())
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery().replace("channel_binding=", "channelBinding="));
        String credenciais = uri.getRawUserInfo();
        if (credenciais != null && credenciais.contains(":")) {
            int separador = credenciais.indexOf(':');
            return comCredenciais(jdbc, decodificar(credenciais.substring(0, separador)), decodificar(credenciais.substring(separador + 1)));
        }
        return comCredenciais(jdbc, usuario, senha);
    }

    Map<String, Object> comoPropriedades() {
        Map<String, Object> propriedades = new LinkedHashMap<>();
        propriedades.put("spring.datasource.url", jdbcUrl);
        propriedades.put("spring.datasource.username", usuario);
        propriedades.put("spring.datasource.password", senha);
        return propriedades;
    }

    /** Endereço sem usuário e senha, para mostrar no log. */
    String paraLog() {
        int parametros = jdbcUrl.indexOf('?');
        return parametros < 0 ? jdbcUrl : jdbcUrl.substring(0, parametros);
    }

    /** Tira espaços, aspas e o "psql" que vêm junto quando se copia o comando pronto do Neon. */
    private static String limpar(String endereco) {
        String limpo = endereco == null ? "" : endereco.strip();
        if (limpo.regionMatches(true, 0, "psql ", 0, 5)) {
            limpo = limpo.substring(5).strip();
        }
        if (limpo.length() >= 2 && (limpo.startsWith("'") && limpo.endsWith("'") || limpo.startsWith("\"") && limpo.endsWith("\""))) {
            limpo = limpo.substring(1, limpo.length() - 1).strip();
        }
        return limpo;
    }

    /**
     * O host "-pooler" do Neon passa por um PgBouncer: o sistema já tem pool de conexões próprio e o Flyway
     * precisa de conexão direta, que é o mesmo host sem o "-pooler".
     */
    private static String hostDireto(String host) {
        int fimDoPrimeiroNome = host.indexOf('.');
        String primeiroNome = fimDoPrimeiroNome < 0 ? host : host.substring(0, fimDoPrimeiroNome);
        return primeiroNome.endsWith("-pooler")
                ? primeiroNome.substring(0, primeiroNome.length() - "-pooler".length()) + host.substring(primeiroNome.length())
                : host;
    }

    private static EnderecoBanco comCredenciais(String jdbc, String usuario, String senha) {
        if (usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            throw new IllegalStateException("Faltou o usuário ou a senha do banco: coloque dentro de WEB_DB_URL "
                    + "(formato do Neon) ou preencha WEB_DB_USER e WEB_DB_PASSWORD.");
        }
        return new EnderecoBanco(jdbc, usuario.strip(), senha.strip());
    }

    private static String decodificar(String texto) {
        return URLDecoder.decode(texto.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
