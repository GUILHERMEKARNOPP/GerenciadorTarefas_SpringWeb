package br.com.gerenciador.dto;

import br.com.gerenciador.model.Prioridade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PrioridadeForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 30, message = "O nome pode ter no máximo 30 caracteres.")
    private String nome;

    @NotNull(message = "Informe o nível.")
    @Min(value = 1, message = "O nível vai de 1 a 99.")
    @Max(value = 99, message = "O nível vai de 1 a 99.")
    private Integer nivel;

    // A cor vai para o atributo style da página: só o formato #RRGGBB é aceito.
    @NotNull(message = "Escolha a cor.")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Use uma cor no formato #RRGGBB.")
    private String cor = "#6c757d";

    public static PrioridadeForm de(Prioridade prioridade) {
        PrioridadeForm form = new PrioridadeForm();
        form.nome = prioridade.getNome();
        form.nivel = prioridade.getNivel();
        form.cor = prioridade.getCor();
        return form;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }
}
