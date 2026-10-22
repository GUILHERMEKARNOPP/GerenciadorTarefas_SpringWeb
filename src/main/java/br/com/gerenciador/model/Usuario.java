package br.com.gerenciador.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    /** Senhas erradas seguidas que bloqueiam o login (mesma regra da versão desktop). */
    public static final int MAX_TENTATIVAS_LOGIN = 5;
    public static final Duration TEMPO_BLOQUEIO_LOGIN = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 50)
    private String login;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;

    @Column(name = "bloqueado_ate")
    private OffsetDateTime bloqueadoAte;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private OffsetDateTime dataCriacao;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String login, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.email = email;
        this.login = login;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public boolean estaBloqueado(OffsetDateTime agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    public void desbloquear() {
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public int getTentativasFalhas() {
        return tentativasFalhas;
    }

    public OffsetDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public OffsetDateTime getDataCriacao() {
        return dataCriacao;
    }
}
