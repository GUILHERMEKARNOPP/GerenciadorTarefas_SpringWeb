package br.com.gerenciador.dto;

import br.com.gerenciador.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 50, message = "O nome pode ter no máximo 50 caracteres.")
    private String nome;

    @Size(max = 255, message = "A descrição pode ter no máximo 255 caracteres.")
    private String descricao;

    public static CategoriaForm de(Categoria categoria) {
        CategoriaForm form = new CategoriaForm();
        form.nome = categoria.getNome();
        form.descricao = categoria.getDescricao();
        return form;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
