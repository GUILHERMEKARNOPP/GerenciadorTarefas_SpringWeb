package br.com.gerenciador.dto;

import br.com.gerenciador.model.Tarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TarefaForm {

    @NotBlank(message = "Informe o título.")
    @Size(max = 150, message = "O título pode ter no máximo 150 caracteres.")
    private String titulo;

    @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres.")
    private String descricao;

    private Long categoriaId;

    @NotNull(message = "Escolha a prioridade.")
    private Long prioridadeId;

    @NotNull(message = "Escolha o status.")
    private Long statusId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataVencimento;

    public static TarefaForm de(Tarefa tarefa) {
        TarefaForm form = new TarefaForm();
        form.titulo = tarefa.getTitulo();
        form.descricao = tarefa.getDescricao();
        form.categoriaId = tarefa.getCategoria() == null ? null : tarefa.getCategoria().getId();
        form.prioridadeId = tarefa.getPrioridade().getId();
        form.statusId = tarefa.getStatus().getId();
        form.dataVencimento = tarefa.getDataVencimento();
        return form;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getPrioridadeId() {
        return prioridadeId;
    }

    public void setPrioridadeId(Long prioridadeId) {
        this.prioridadeId = prioridadeId;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }
}
