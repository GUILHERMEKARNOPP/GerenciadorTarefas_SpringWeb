package br.com.gerenciador.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/** Parâmetros da URL da lista de tarefas. Campo nulo = sem filtro. categoria = 0 busca "sem categoria". */
public class FiltroTarefas {

    private String q;
    private Long status;
    private Long prioridade;
    private Long categoria;
    private OrdemTarefas ordem = OrdemTarefas.VENCIMENTO;
    private int pagina;

    /** Os mesmos filtros como parâmetros de URL, para voltar à lista do jeito que estava. */
    public Map<String, Object> comoParametros() {
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (q != null && !q.isBlank()) {
            parametros.put("q", q);
        }
        if (status != null) {
            parametros.put("status", status);
        }
        if (prioridade != null) {
            parametros.put("prioridade", prioridade);
        }
        if (categoria != null) {
            parametros.put("categoria", categoria);
        }
        if (ordem != null && ordem != OrdemTarefas.VENCIMENTO) {
            parametros.put("ordem", ordem);
        }
        if (pagina > 0) {
            parametros.put("pagina", pagina);
        }
        return parametros;
    }

    public String urlPagina(int numeroPagina) {
        Map<String, Object> parametros = comoParametros();
        parametros.remove("pagina");
        if (numeroPagina > 0) {
            parametros.put("pagina", numeroPagina);
        }
        return UrlComParametros.montar("/tarefas", parametros);
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public Long getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(Long prioridade) {
        this.prioridade = prioridade;
    }

    public Long getCategoria() {
        return categoria;
    }

    public void setCategoria(Long categoria) {
        this.categoria = categoria;
    }

    public OrdemTarefas getOrdem() {
        return ordem;
    }

    public void setOrdem(OrdemTarefas ordem) {
        this.ordem = ordem;
    }

    public int getPagina() {
        return pagina;
    }

    public void setPagina(int pagina) {
        this.pagina = Math.max(pagina, 0);
    }
}
