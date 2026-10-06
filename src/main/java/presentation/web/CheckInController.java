package presentation.web;

import application.controleDeAcesso.CheckInService;
import org.springframework.web.bind.annotation.*;

/** UC03 - Realizar Check-in. */
@RestController
@RequestMapping("/api/checkins")
public class CheckInController {

    private final CheckInService servico;

    public CheckInController(CheckInService servico) {
        this.servico = servico;
    }

    public record NovoCheckIn(int alunoId, int estabelecimentoId,
                              double latitude, double longitude) {}

    @PostMapping
    public void realizarCheckIn(@RequestBody NovoCheckIn dados) {
        servico.realizarCheckIn(dados.alunoId(), dados.estabelecimentoId(),
                dados.latitude(), dados.longitude());
    }
}
