package br.com.gerenciador.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemaTest {

    @Test
    void encontraTemaPeloSlug() {
        assertThat(Tema.doSlug("padrao")).contains(Tema.PADRAO);
    }

    @Test
    void slugForaDaListaNaoEhAceito() {
        assertThat(Tema.doSlug("PADRAO")).isEmpty();
        assertThat(Tema.doSlug("../padrao")).isEmpty();
        assertThat(Tema.doSlug("")).isEmpty();
        assertThat(Tema.doSlug(null)).isEmpty();
    }
}
