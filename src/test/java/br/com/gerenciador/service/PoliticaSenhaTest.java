package br.com.gerenciador.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaSenhaTest {

    @Test
    void aceitaSenhaForte() {
        assertThatCode(() -> PoliticaSenha.validar("Segura123", "Segura123", "ana")).doesNotThrowAnyException();
    }

    @Test
    void recusaSenhaCurta() {
        assertRecusa("Ab1", "Ab1", "ana", "senha", "8 a 64");
    }

    @Test
    void recusaSenhaSemNumero() {
        assertRecusa("senhafraca", "senhafraca", "ana", "senha", "uma letra e um número");
    }

    @Test
    void recusaSenhaIgualAoUsuarioMesmoComMaiusculas() {
        assertRecusa("ana.souza1", "ana.souza1", "ANA.SOUZA1", "senha", "igual ao usuário");
    }

    @Test
    void recusaConfirmacaoDiferente() {
        assertRecusa("Segura123", "Segura124", "ana", "confirmacaoSenha", "não confere");
    }

    @Test
    void recusaSenhaQueOBcryptCortaria() {
        // 42 caracteres, mas 82 bytes em UTF-8: o BCrypt ignoraria o que passa de 72 bytes.
        String senha = "ã".repeat(40) + "a1";
        assertRecusa(senha, senha, "ana", "senha", "longa demais");
    }

    private static void assertRecusa(String senha, String confirmacao, String login, String campo, String trecho) {
        assertThatThrownBy(() -> PoliticaSenha.validar(senha, confirmacao, login))
                .isInstanceOfSatisfying(RegraNegocioException.class, erro -> {
                    assertThat(erro.getCampo()).isEqualTo(campo);
                    assertThat(erro.getMessage()).contains(trecho);
                });
    }
}
