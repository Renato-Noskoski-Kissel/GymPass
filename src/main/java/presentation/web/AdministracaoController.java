package presentation.web;

import application.parceiros.GestaoParceirosService;
import org.springframework.web.bind.annotation.*;

/** HU4 - cadastro de parceiros e configuração de planos. */
@RestController
@RequestMapping("/api")
public class AdministracaoController {

    private final GestaoParceirosService servico;

    public AdministracaoController(GestaoParceirosService servico) {
        this.servico = servico;
    }

    public record NovoEstabelecimento(String nome, String tipo, double valorPorCheckIn) {}
    public record NovaEmpresa(String razaoSocial, int limiteDependentes, int diaVencimento) {}
    public record NovoPlano(String nome, int nivel, double valorMensal,
                            int limiteAulasMes, boolean permiteDependentes) {}

    @PostMapping("/estabelecimentos")
    public void cadastrarEstabelecimento(@RequestBody NovoEstabelecimento dados) {
        servico.cadastrarEstabelecimento(dados.nome(), dados.tipo(), dados.valorPorCheckIn());
    }

    @PostMapping("/estabelecimentos/{id}/aprovar")
    public void aprovar(@PathVariable int id) { servico.aprovar(id); }

    @PostMapping("/estabelecimentos/{id}/suspender")
    public void suspender(@PathVariable int id) { servico.suspender(id); }

    @PostMapping("/empresas")
    public void contratarEmpresa(@RequestBody NovaEmpresa dados) {
        servico.contratarEmpresa(dados.razaoSocial(), dados.limiteDependentes(),
                dados.diaVencimento());
    }

    @PostMapping("/planos")
    public void criarPlano(@RequestBody NovoPlano dados) {
        servico.criarPlano(dados.nome(), dados.nivel(), dados.valorMensal(),
                dados.limiteAulasMes(), dados.permiteDependentes());
    }
}
