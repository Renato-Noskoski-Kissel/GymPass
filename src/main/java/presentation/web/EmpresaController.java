package presentation.web;

import application.plano.BeneficiariosService;
import org.springframework.web.bind.annotation.*;

/** HU2 - gestão do quadro de beneficiários. */
@RestController
@RequestMapping("/api/empresas/{empresaId}")
public class EmpresaController {

    private final BeneficiariosService servico;

    public EmpresaController(BeneficiariosService servico) {
        this.servico = servico;
    }

    public record NovoFuncionario(String nome, String matricula, int planoId) {}
    public record TrocaDePlano(int planoId) {}
    public record NovoDependente(String nome, String grauParentesco) {}

    @PostMapping("/funcionarios")
    public void incluirFuncionario(@PathVariable int empresaId,
                                   @RequestBody NovoFuncionario dados) {
        servico.incluirFuncionario(empresaId, dados.nome(), dados.matricula(), dados.planoId());
    }

    @PostMapping("/funcionarios/{funcionarioId}/plano")
    public void trocarPlano(@PathVariable int empresaId, @PathVariable int funcionarioId,
                            @RequestBody TrocaDePlano dados) {
        servico.trocarPlano(empresaId, funcionarioId, dados.planoId());
    }

    @PostMapping("/funcionarios/{funcionarioId}/dependentes")
    public void incluirDependente(@PathVariable int empresaId, @PathVariable int funcionarioId,
                                  @RequestBody NovoDependente dados) {
        servico.incluirDependente(empresaId, funcionarioId, dados.nome(), dados.grauParentesco());
    }

    @DeleteMapping("/funcionarios/{funcionarioId}")
    public void removerFuncionario(@PathVariable int empresaId, @PathVariable int funcionarioId) {
        servico.removerFuncionario(empresaId, funcionarioId);
    }
}
