package br.compneusgppremium.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CadastroPneuException extends RuntimeException {
    private final String codigo;
    private final HttpStatus status;

    public CadastroPneuException(final String codigo, final String mensagem) {
        this(codigo, mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public CadastroPneuException(final String codigo, final String mensagem, final HttpStatus status) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
    }
}
