package br.com.gerenciador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
public class GerenciadorTarefasWebApplication {

    /** Variáveis sem as quais o sistema não roda em produção (preenchidas no painel do Render). */
    private static final String[] VARIAVEIS_DE_PRODUCAO = {"WEB_DB_URL", "WEB_DB_USER", "WEB_DB_PASSWORD", "ADMIN_LOGIN"};

    public static void main(String[] args) {
        SpringApplication aplicacao = new SpringApplication(GerenciadorTarefasWebApplication.class);
        // Em produção, falta de configuração para o sistema logo no início, com o nome de cada variável que falta.
        aplicacao.addInitializers(contexto -> {
            ConfigurableEnvironment ambiente = contexto.getEnvironment();
            if (ambiente.matchesProfiles("prod")) {
                ambiente.setRequiredProperties(VARIAVEIS_DE_PRODUCAO);
                ambiente.validateRequiredProperties();
            }
        });
        aplicacao.run(args);
    }
}
