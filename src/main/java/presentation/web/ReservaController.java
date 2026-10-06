package presentation.web;

import application.agendamento.ReservaService;
import org.springframework.web.bind.annotation.*;

/** UC04a - Reservar Vaga em Aula. */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService servico;

    public ReservaController(ReservaService servico) {
        this.servico = servico;
    }

    public record NovaReserva(int alunoId, int aulaId) {}

    @PostMapping
    public void reservarVaga(@RequestBody NovaReserva dados) {
        servico.reservarVaga(dados.alunoId(), dados.aulaId());
    }
}
