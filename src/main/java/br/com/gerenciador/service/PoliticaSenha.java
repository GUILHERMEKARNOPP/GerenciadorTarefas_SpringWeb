package br.com.gerenciador.service;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Regras de senha forte, as mesmas no cadastro, na tela do administrador e em "Meu perfil". */
public final class PoliticaSenha {

    public static final int TAMANHO_MINIMO = 8;
    public static final int TAMANHO_MAXIMO = 64;
    // O BCrypt só considera os primeiros 72 bytes: senha maior seria cortada sem aviso.
    private static final int MAXIMO_BYTES_BCRYPT = 72;
    private static final Pattern TEM_LETRA = Pattern.compile("\\p{L}");
    private static final Pattern TEM_NUMERO = Pattern.compile("\\d");

    private PoliticaSenha() {
    }

    /** Lança RegraNegocioException apontando para o campo "senha" ou "confirmacaoSenha". */
    public static void validar(String senha, String confirmacao, String login) {
        if (senha == null || senha.length() < TAMANHO_MINIMO || senha.length() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("senha",
                    "A senha deve ter de " + TAMANHO_MINIMO + " a " + TAMANHO_MAXIMO + " caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_BCRYPT) {
            throw new RegraNegocioException("senha", "A senha é longa demais. Use menos acentos ou símbolos.");
        }
        if (!TEM_LETRA.matcher(senha).find() || !TEM_NUMERO.matcher(senha).find()) {
            throw new RegraNegocioException("senha", "A senha deve ter pelo menos uma letra e um número.");
        }
        if (login != null && senha.equalsIgnoreCase(login.strip())) {
            throw new RegraNegocioException("senha", "A senha não pode ser igual ao usuário.");
        }
        if (!senha.equals(confirmacao)) {
            throw new RegraNegocioException("confirmacaoSenha", "A confirmação não confere com a senha.");
        }
    }
}
