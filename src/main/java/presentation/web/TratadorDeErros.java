package presentation.web;

import application.NaoEncontradoException;
import dominio.RegraDeNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte as exceções em respostas HTTP com uma mensagem que a tela mostra.
 * Violação de regra de negócio vira 400 com o texto definido no domínio.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Dtos.Erro> regraViolada(RegraDeNegocioException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Dtos.Erro(e.getMessage()));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Dtos.Erro> naoEncontrado(NaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Dtos.Erro(e.getMessage()));
    }

    /**
     * O banco recusou a gravação: chave repetida (ex.: registro de instrutor já
     * usado) ou linha ainda referenciada (ex.: modalidade ou instrutor com aulas).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Dtos.Erro> restricaoDoBanco(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new Dtos.Erro("O banco recusou a operação: o registro já existe "
                        + "ou ainda é usado por outro (por exemplo, aulas na grade)."));
    }
}
