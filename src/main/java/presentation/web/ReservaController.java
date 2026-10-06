package presentation.web;

import application.agendamento.ReservaService;
import org.springframework.web.bind.annotation.*;

/** UC04a - Reservar Vaga em Aula; UC04b - Cancelar Reserva. */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService servico;

    public ReservaController(ReservaService servico) {
        this.servico = servico;
    }

    public record NovaReserva(int alunoId, int aulaId) {}
    public record Cancelamento(int alunoId, boolean aceitaPenalidade) {}

    @PostMapping
    public void reservarVaga(@RequestBody NovaReserva dados) {
        servico.reservarVaga(dados.alunoId(), dados.aulaId());
    }

    @PostMapping("/{id}/cancelar")
    public void cancelarReserva(@PathVariable int id, @RequestBody Cancelamento dados) {
        servico.cancelarReserva(dados.alunoId(), id, dados.aceitaPenalidade());
    }
}
