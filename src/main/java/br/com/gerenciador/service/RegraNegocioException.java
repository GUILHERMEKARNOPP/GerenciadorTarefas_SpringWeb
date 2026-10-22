package br.com.gerenciador.service;

/**
 * Regra de negócio violada, com mensagem pronta para mostrar na tela.
 * Quando a regra é de um campo do formulário, {@code campo} diz qual (a tela destaca o campo).
 */
public class RegraNegocioException extends RuntimeException {

    private final String campo;

    public RegraNegocioException(String mensagem) {
        this(null, mensagem);
    }

    public RegraNegocioException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    /** Nome do campo do formulário, ou null quando o erro é do formulário como um todo. */
    public String getCampo() {
        return campo;
    }
}
