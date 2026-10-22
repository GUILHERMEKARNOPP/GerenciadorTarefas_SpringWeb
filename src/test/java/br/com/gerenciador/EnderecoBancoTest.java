package br.com.gerenciador;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnderecoBancoTest {

    private static final String HOST = "ep-cool-darkness-123456.us-east-1.aws.neon.tech";

    @Test
    void aceitaEnderecoJdbcComUsuarioESenhaSeparados() {
        EnderecoBanco banco = EnderecoBanco.de("jdbc:postgresql://" + HOST + "/neondb?sslmode=require", "dono", "segredo");

        assertThat(banco.jdbcUrl()).isEqualTo("jdbc:postgresql://" + HOST + "/neondb?sslmode=require");
        assertThat(banco.usuario()).isEqualTo("dono");
        assertThat(banco.senha()).isEqualTo("segredo");
    }

    @Test
    void converteOEnderecoCopiadoDoNeon() {
        EnderecoBanco banco = EnderecoBanco.de(
                "psql 'postgresql://neondb_owner:npg_A%2Bb@" + HOST + "/neondb?sslmode=require&channel_binding=require'",
                null, null);

        assertThat(banco.jdbcUrl()).isEqualTo("jdbc:postgresql://" + HOST + "/neondb?sslmode=require&channelBinding=require");
        assertThat(banco.usuario()).isEqualTo("neondb_owner");
        assertThat(banco.senha()).isEqualTo("npg_A+b");
    }

    @Test
    void usaAConexaoDiretaNoLugarDoPooler() {
        EnderecoBanco banco = EnderecoBanco.de(
                "postgresql://dono:segredo@ep-cool-darkness-123456-pooler.us-east-1.aws.neon.tech:5432/neondb", null, null);

        assertThat(banco.jdbcUrl()).isEqualTo("jdbc:postgresql://" + HOST + ":5432/neondb");
    }

    @Test
    void usuarioESenhaDoEnderecoTemPrioridade() {
        EnderecoBanco banco = EnderecoBanco.de(" \"postgres://dono:segredo@" + HOST + "/neondb\" ", "outro", "outra");

        assertThat(banco.usuario()).isEqualTo("dono");
        assertThat(banco.senha()).isEqualTo("segredo");
        assertThat(banco.paraLog()).isEqualTo("jdbc:postgresql://" + HOST + "/neondb").doesNotContain("segredo");
    }

    @Test
    void enderecoEmOutroFormatoDaErroExplicando() {
        assertThatThrownBy(() -> EnderecoBanco.de("mysql://" + HOST + "/neondb", "dono", "segredo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WEB_DB_URL deve ser");
    }

    @Test
    void semUsuarioESenhaDaErroExplicando() {
        assertThatThrownBy(() -> EnderecoBanco.de("postgresql://" + HOST + "/neondb", null, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WEB_DB_USER");
    }
}
