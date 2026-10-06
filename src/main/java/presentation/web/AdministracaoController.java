package presentation.web;

import application.gestaoDeParceiros.GestaoParceirosService;
import org.springframework.web.bind.annotation.*;

/** HU4 - cadastro de parceiros e configuração de planos (incluindo a cobertura, RF5). */
@RestController
@RequestMapping("/api")
public class AdministracaoController {

    private final GestaoParceirosService servico;

    public AdministracaoController(GestaoParceirosService servico) {
        this.servico = servico;
    }

    public record NovoEstabelecimento(String nome, String tipo, double valorPorCheckIn,
                                      String logradouro, String numero, String bairro,
                                      String cidade, String cep,
                                      double latitude, double longitude) {}
    public record NovaEmpresa(String razaoSocial, int limiteDependentes, int diaVencimento) {}
    public record NovoPlano(String nome, int nivel, double valorMensal,
                            int limiteAulasMes, boolean permiteDependentes) {}
    public record LiberacaoEstabelecimento(int estabelecimentoId) {}
    public record LiberacaoModalidade(int estabelecimentoId, int modalidadeId) {}

    @PostMapping("/estabelecimentos")
    public void cadastrarEstabelecimento(@RequestBody NovoEstabelecimento dados) {
        servico.cadastrarEstabelecimento(dados.nome(), dados.tipo(), dados.valorPorCheckIn(),
                dados.logradouro(), dados.numero(), dados.bairro(), dados.cidade(), dados.cep(),
                dados.latitude(), dados.longitude());
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

    @PostMapping("/planos/{id}/estabelecimentos")
    public void liberarEstabelecimento(@PathVariable int id,
                                       @RequestBody LiberacaoEstabelecimento dados) {
        servico.liberarEstabelecimento(id, dados.estabelecimentoId());
    }

    @PostMapping("/planos/{id}/modalidades")
    public void liberarModalidade(@PathVariable int id, @RequestBody LiberacaoModalidade dados) {
        servico.liberarModalidade(id, dados.estabelecimentoId(), dados.modalidadeId());
    }
}
