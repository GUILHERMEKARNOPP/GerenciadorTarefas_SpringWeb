package br.com.gerenciador.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Registro inexistente, ou de outro usuário. Os dois casos respondem 404 de propósito,
 * para não revelar que o id existe.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException() {
        super("Registro não encontrado.");
    }
}
