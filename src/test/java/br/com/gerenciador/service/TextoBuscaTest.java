package br.com.gerenciador.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextoBuscaTest {

    @Test
    void escapaCuringasDigitadosPeloUsuario() {
        assertThat(TextoBusca.padraoLike(" Meta 50%_! ")).isEqualTo("%meta 50!%!_!!%");
    }

    @Test
    void textoVazioNaoFiltra() {
        assertThat(TextoBusca.padraoLike("   ")).isNull();
        assertThat(TextoBusca.padraoLike(null)).isNull();
    }
}
