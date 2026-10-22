package br.com.gerenciador.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Situação da tarefa (Pendente, Em andamento, Concluída...). Vale para todos os usuários. */
@Entity
@Table(name = "status")
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String nome;

    /** Marca o status de "tarefa concluída": usado pelo botão Concluir e no cálculo de atraso. */
    @Column(nullable = false)
    private boolean finaliza;

    public Status() {
    }

    public Status(String nome, boolean finaliza) {
        this.nome = nome;
        this.finaliza = finaliza;
    }

    public Long getId() {
        return id;
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
