package presentation.web;

import application.plano.BeneficiariosService;
import org.springframework.web.bind.annotation.*;

/** HU2 - gestão do quadro de beneficiários. CNPJ e CPF vão na URL só com dígitos. */
@RestController
@RequestMapping("/api/empresas/{cnpjEmpresa}")
public class EmpresaController {

    private final BeneficiariosService servico;

    public EmpresaController(BeneficiariosService servico) {
        this.servico = servico;
    }

    public record NovoFuncionario(String nome, String cpf, String matricula, int planoId) {}
    public record TrocaDePlano(int planoId) {}
    public record NovoDependente(String nome, String cpf, String grauParentesco) {}

    @PostMapping("/funcionarios")
    public void incluirFuncionario(@PathVariable String cnpjEmpresa,
                                   @RequestBody NovoFuncionario dados) {
        servico.incluirFuncionario(cnpjEmpresa, dados.nome(), dados.cpf(), dados.matricula(),
                dados.planoId());
    }

    @PostMapping("/funcionarios/{cpf}/plano")
    public void trocarPlano(@PathVariable String cnpjEmpresa, @PathVariable String cpf,
                            @RequestBody TrocaDePlano dados) {
        servico.trocarPlano(cnpjEmpresa, cpf, dados.planoId());
    }

    @PostMapping("/funcionarios/{cpf}/suspender")
    public void suspenderBeneficio(@PathVariable String cnpjEmpresa, @PathVariable String cpf) {
        servico.suspenderAdesao(cnpjEmpresa, cpf);
    }

    @PostMapping("/funcionarios/{cpf}/reativar")
    public void reativarBeneficio(@PathVariable String cnpjEmpresa, @PathVariable String cpf) {
        servico.reativarAdesao(cnpjEmpresa, cpf);
    }

    @PostMapping("/funcionarios/{cpfFuncionario}/dependentes")
    public void incluirDependente(@PathVariable String cnpjEmpresa, @PathVariable String cpfFuncionario,
                                  @RequestBody NovoDependente dados) {
        servico.incluirDependente(cnpjEmpresa, cpfFuncionario, dados.nome(), dados.cpf(),
                dados.grauParentesco());
    }

    @PostMapping("/planos/{planoId}")
    public void oferecerPlano(@PathVariable String cnpjEmpresa, @PathVariable int planoId) {
        servico.oferecerPlano(cnpjEmpresa, planoId);
    }

    @DeleteMapping("/planos/{planoId}")
    public void retirarPlano(@PathVariable String cnpjEmpresa, @PathVariable int planoId) {
        servico.retirarPlano(cnpjEmpresa, planoId);
    }

    @DeleteMapping("/funcionarios/{cpf}")
    public void removerFuncionario(@PathVariable String cnpjEmpresa, @PathVariable String cpf) {
        servico.removerFuncionario(cnpjEmpresa, cpf);
    }
}
