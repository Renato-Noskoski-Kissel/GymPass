package presentation.web;

import application.gestaoDeParceiros.EstabelecimentoService;
import org.springframework.web.bind.annotation.*;

/** HU5 - modalidades e instrutores do estabelecimento. */
@RestController
@RequestMapping("/api/estabelecimentos/{cnpj}")
public class EstabelecimentoController {

    private final EstabelecimentoService servico;

    public EstabelecimentoController(EstabelecimentoService servico) {
        this.servico = servico;
    }

    public record NovaModalidade(String nome, String descricao) {}
    public record NovoInstrutor(String nome, String registro, String especialidade) {}

    @PostMapping("/modalidades")
    public void adicionarModalidade(@PathVariable String cnpj, @RequestBody NovaModalidade dados) {
        servico.adicionarModalidade(cnpj, dados.nome(), dados.descricao());
    }

    @DeleteMapping("/modalidades/{modalidadeId}")
    public void removerModalidade(@PathVariable String cnpj, @PathVariable int modalidadeId) {
        servico.removerModalidade(cnpj, modalidadeId);
    }

    @PostMapping("/instrutores")
    public void contratarInstrutor(@PathVariable String cnpj, @RequestBody NovoInstrutor dados) {
        servico.contratarInstrutor(cnpj, dados.nome(), dados.registro(), dados.especialidade());
    }

    @DeleteMapping("/instrutores/{registro}")
    public void desligarInstrutor(@PathVariable String cnpj, @PathVariable String registro) {
        servico.desligarInstrutor(cnpj, registro);
    }
}
