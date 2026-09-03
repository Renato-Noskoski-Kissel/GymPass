package presentation.web;

import application.estabelecimento.EstabelecimentoService;
import org.springframework.web.bind.annotation.*;

/** HU5 - modalidades e instrutores do estabelecimento. */
@RestController
@RequestMapping("/api/estabelecimentos/{id}")
public class EstabelecimentoController {

    private final EstabelecimentoService servico;

    public EstabelecimentoController(EstabelecimentoService servico) {
        this.servico = servico;
    }

    public record NovaModalidade(String nome, String descricao) {}
    public record NovoInstrutor(String nome, String registro, String especialidade) {}

    @PostMapping("/modalidades")
    public void adicionarModalidade(@PathVariable int id, @RequestBody NovaModalidade dados) {
        servico.adicionarModalidade(id, dados.nome(), dados.descricao());
    }

    @DeleteMapping("/modalidades/{modalidadeId}")
    public void removerModalidade(@PathVariable int id, @PathVariable int modalidadeId) {
        servico.removerModalidade(id, modalidadeId);
    }

    @PostMapping("/instrutores")
    public void contratarInstrutor(@PathVariable int id, @RequestBody NovoInstrutor dados) {
        servico.contratarInstrutor(id, dados.nome(), dados.registro(), dados.especialidade());
    }

    @DeleteMapping("/instrutores/{instrutorId}")
    public void desligarInstrutor(@PathVariable int id, @PathVariable int instrutorId) {
        servico.desligarInstrutor(id, instrutorId);
    }
}
