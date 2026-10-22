package br.com.gerenciador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@SpringBootApplication
public class GerenciadorTarefasWebApplication {

    private static final Logger LOG = LoggerFactory.getLogger(GerenciadorTarefasWebApplication.class);

    public static void main(String[] args) {
        SpringApplication aplicacao = new SpringApplication(GerenciadorTarefasWebApplication.class);
        aplicacao.addInitializers(contexto -> {
            ConfigurableEnvironment ambiente = contexto.getEnvironment();
            if (ambiente.matchesProfiles("prod")) {
                configurarProducao(ambiente);
            }
        });
        aplicacao.run(args);
    }

    /**
     * Lê as variáveis do painel do Render antes de qualquer coisa: se faltar ou estiver errada alguma, o sistema
     * para logo no início explicando o problema, em vez de falhar mais adiante com um erro difícil de entender.
     */
    private static void configurarProducao(ConfigurableEnvironment ambiente) {
        ambiente.setRequiredProperties("WEB_DB_URL", "ADMIN_LOGIN");
        ambiente.validateRequiredProperties();
        EnderecoBanco banco = EnderecoBanco.de(ambiente.getProperty("WEB_DB_URL"),
                ambiente.getProperty("WEB_DB_USER"), ambiente.getProperty("WEB_DB_PASSWORD"));
        ambiente.getPropertySources().addFirst(new MapPropertySource("enderecoBanco", banco.comoPropriedades()));
        LOG.info("Banco de dados: {}", banco.paraLog());
    }
}
