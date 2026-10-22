package br.com.gerenciador.controller;

import br.com.gerenciador.service.RegraNegocioException;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BindingResult;

/** Mostra o erro de regra de negócio no formulário: embaixo do campo, ou no topo quando não é de um campo. */
public final class ErrosFormulario {

    private ErrosFormulario() {
    }

    public static void registrar(BindingResult resultado, RegraNegocioException erro) {
        String campo = erro.getCampo();
        boolean campoDoFormulario = campo != null && resultado.getTarget() != null
                && new BeanWrapperImpl(resultado.getTarget()).isReadableProperty(campo);
        if (campoDoFormulario) {
            resultado.rejectValue(campo, "regraNegocio", erro.getMessage());
        } else {
            resultado.reject("regraNegocio", erro.getMessage());
        }
    }
}
