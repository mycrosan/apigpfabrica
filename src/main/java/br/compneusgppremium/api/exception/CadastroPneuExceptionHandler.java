package br.compneusgppremium.api.exception;

import br.compneusgppremium.api.controller.dto.ErroPneuDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CadastroPneuExceptionHandler {
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErroPneuDTO> validar(final org.springframework.web.bind.MethodArgumentNotValidException erro) {
        List<String> campos = erro.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": " + campo.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(new ErroPneuDTO(400, "DADOS_INVALIDOS",
                "Confira os campos informados.", "Confira os campos informados.", campos));
    }
    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.DataIntegrityViolationException.class})
    public ResponseEntity<ErroPneuDTO> concorrencia(final RuntimeException erro) {
        return ResponseEntity.status(409).body(new ErroPneuDTO(409, "CONFLITO_GRAVACAO",
                "Os dados foram alterados. Consulte o resultado antes de reenviar.",
                "Os dados foram alterados. Consulte o resultado antes de reenviar.", List.of()));
    }
    @ExceptionHandler(CadastroPneuException.class)
    public ResponseEntity<ErroPneuDTO> tratar(final CadastroPneuException erro) {
        return ResponseEntity.status(erro.getStatus()).body(new ErroPneuDTO(
                erro.getStatus().value(), erro.getCodigo(), erro.getMessage(), erro.getMessage(), List.of()));
    }
}
