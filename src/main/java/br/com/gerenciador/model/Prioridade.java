package br.com.gerenciador.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Nível de urgência da tarefa. Vale para todos os usuários. */
@Entity
@Table(name = "prioridade")
public class Prioridade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String nome;

    /** Ordem de urgência: 1 é a mais urgente. */
    @Column(nullable = false)
    private int nivel;

    /** Cor do selo na tela, sempre no formato #RRGGBB (validado no formulário e no banco). */
    @Column(nullable = false, length = 7)
    private String cor;

    public Prioridade() {
    }

    public Prioridade(String nome, int nivel, String cor) {
        this.nome = nome;
        this.nivel = nivel;
        this.cor = cor;
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

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = nivel;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }
}
