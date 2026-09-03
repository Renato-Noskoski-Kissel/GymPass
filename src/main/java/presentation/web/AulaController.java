package presentation.web;

import application.grade.GradeService;
import org.springframework.web.bind.annotation.*;

/** HU1 - grade de aulas do estabelecimento. */
@RestController
@RequestMapping("/api/aulas")
public class AulaController {

    private final GradeService servico;

    public AulaController(GradeService servico) {
        this.servico = servico;
    }

    public record NovaAula(int estabelecimentoId, int modalidadeId, int instrutorId,
                           String inicio, long duracaoMin, int capacidade) {}
    public record TrocaDeInstrutor(int instrutorId) {}
    public record NovaCapacidade(int capacidade) {}

    @PostMapping
    public void agendar(@RequestBody NovaAula dados) {
        servico.agendar(dados.estabelecimentoId(), dados.modalidadeId(), dados.instrutorId(),
                dados.inicio(), dados.duracaoMin(), dados.capacidade());
    }

    @PostMapping("/{id}/instrutor")
    public void trocarInstrutor(@PathVariable int id, @RequestBody TrocaDeInstrutor dados) {
        servico.trocarInstrutor(id, dados.instrutorId());
    }

    @PostMapping("/{id}/capacidade")
    public void alterarCapacidade(@PathVariable int id, @RequestBody NovaCapacidade dados) {
        servico.alterarCapacidade(id, dados.capacidade());
    }

    @PostMapping("/{id}/cancelar")
    public void cancelar(@PathVariable int id) {
        servico.cancelar(id);
    }
}
