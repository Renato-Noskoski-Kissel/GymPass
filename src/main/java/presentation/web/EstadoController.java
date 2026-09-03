package presentation.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Devolve todo o estado do sistema em uma única requisição.
 *
 * Cada operação de escrita é seguida de um GET /api/estado, e a tela é
 * redesenhada inteira a partir da resposta.
 */
@RestController
@RequestMapping("/api")
public class EstadoController {

    private final MontadorDeEstado montador;

    public EstadoController(MontadorDeEstado montador) {
        this.montador = montador;
    }

    @GetMapping("/estado")
    public Dtos.EstadoDto estado() {
        return montador.montar();
    }
}
