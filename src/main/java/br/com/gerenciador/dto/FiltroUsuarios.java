package br.com.gerenciador.dto;

import br.com.gerenciador.model.Perfil;

import java.util.LinkedHashMap;
import java.util.Map;

/** Parâmetros da URL da lista de usuários (área do administrador). Campo nulo = sem filtro. */
public class FiltroUsuarios {

    private String q;
    private Perfil perfil;
    private int pagina;

    public String urlPagina(int numeroPagina) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (q != null && !q.isBlank()) {
            parametros.put("q", q);
        }
        if (perfil != null) {
            parametros.put("perfil", perfil);
        }
        if (numeroPagina > 0) {
            parametros.put("pagina", numeroPagina);
        }
        return UrlComParametros.montar("/admin/usuarios", parametros);
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public int getPagina() {
        return pagina;
    }

    public void setPagina(int pagina) {
        this.pagina = Math.max(pagina, 0);
    }
}
