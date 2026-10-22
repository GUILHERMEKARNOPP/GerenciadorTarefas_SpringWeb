package br.com.gerenciador.dto;

import br.com.gerenciador.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StatusForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 30, message = "O nome pode ter no máximo 30 caracteres.")
    private String nome;

    private boolean finaliza;

    public static StatusForm de(Status status) {
        StatusForm form = new StatusForm();
        form.nome = status.getNome();
        form.finaliza = status.isFinaliza();
        return form;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isFinaliza() {
        return finaliza;
    }

    public void setFinaliza(boolean finaliza) {
        this.finaliza = finaliza;
    }
}
