package br.com.gerenciador.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Temas visuais do sistema. O slug é o valor guardado no cookie e o nome da pasta static/temas/{slug},
 * onde ficam o tema.css e o tema.js de cada tema (o Padrão usa só o app.css).
 */
public enum Tema {

    PADRAO("padrao", "Padrão");

    private final String slug;
    private final String nome;

    Tema(String slug, String nome) {
        this.slug = slug;
        this.nome = nome;
    }

    /** Só slugs da lista acima são aceitos: o valor vem de cookie/formulário e vai parar em URLs da página. */
    public static Optional<Tema> doSlug(String slug) {
        return Arrays.stream(values()).filter(tema -> tema.slug.equals(slug)).findFirst();
    }

    public String getSlug() {
        return slug;
    }

    public String getNome() {
        return nome;
    }

    public boolean isPadrao() {
        return this == PADRAO;
    }
}
