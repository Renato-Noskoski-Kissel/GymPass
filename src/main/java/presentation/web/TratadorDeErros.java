package presentation.web;

import dominio.RegraDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte violação de regra de negócio em resposta HTTP 400 com a mensagem
 * original, para que a tela mostre exatamente o texto definido no domínio.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Dtos.Erro> regraViolada(RegraDeNegocioException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Dtos.Erro(e.getMessage()));
    }

    @ExceptionHandler(IndexOutOfBoundsException.class)
    public ResponseEntity<Dtos.Erro> naoEncontrado(IndexOutOfBoundsException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Dtos.Erro("Registro não encontrado."));
    }
}
