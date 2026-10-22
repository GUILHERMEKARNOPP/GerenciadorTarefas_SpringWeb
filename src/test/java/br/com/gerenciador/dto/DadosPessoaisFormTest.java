package br.com.gerenciador.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class DadosPessoaisFormTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void criarValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        fabrica.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@example.com", "ana.souza@mail.example.com.br", " ana@example.com "})
    void aceitaEmailValido(String email) {
        assertThat(errosNoEmail(email)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana", "ana@", "ana@example", "a@b@c.com", "ana souza@example.com", "ana@.com", "ana..x@example.com"})
    void recusaEmailInvalido(String email) {
        assertThat(errosNoEmail(email)).isTrue();
    }

    @Test
    void emailMaliciosoNaoTravaAValidacao() {
        // Com a expressão antiga, 20 mil pontos já levavam ~35 s de CPU numa única requisição.
        String malicioso = "a@" + ".".repeat(100_000) + "@";

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertThat(errosNoEmail(malicioso)).isTrue());
    }

    private static boolean errosNoEmail(String email) {
        DadosPessoaisForm form = new DadosPessoaisForm();
        form.setNome("Ana");
        form.setEmail(email);
        return validador.validateProperty(form, "email").stream()
                .anyMatch(violacao -> violacao.getMessage().equals("Informe um e-mail válido."));
    }
}
