package br.com.gerenciador.dto;

import br.com.gerenciador.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Nome e e-mail: usado sozinho em "Meu perfil" e como base dos formulários de cadastro de usuário. */
public class DadosPessoaisForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
    @Pattern(regexp = "[^\\p{Cntrl}]*", message = "O nome contém caracteres inválidos.")
    private String nome;

    // Cada pedaço entre pontos não contém ponto: só existe um jeito de casar o texto, então a validação é
    // linear. A versão ambígua ([^@\s]+\.[^@\s]+) levava dezenas de segundos com "a@.....@" (ReDoS).
    @NotBlank(message = "Informe o e-mail.")
    @Size(max = 150, message = "O e-mail pode ter no máximo 150 caracteres.")
    @Pattern(regexp = "^\\s*[^@\\s.]+(\\.[^@\\s.]+)*@[^@\\s.]+(\\.[^@\\s.]+)+\\s*$", message = "Informe um e-mail válido.")
    private String email;

    public static DadosPessoaisForm de(Usuario usuario) {
        DadosPessoaisForm form = new DadosPessoaisForm();
        form.nome = usuario.getNome();
        form.email = usuario.getEmail();
        return form;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
