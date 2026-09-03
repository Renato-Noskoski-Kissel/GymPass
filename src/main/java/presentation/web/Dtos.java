package presentation.web;

import java.util.List;

/**
 * Objetos de transferência.
 *
 * As classes de domínio NÃO são enviadas ao navegador. Funcionario aponta para
 * Empresa, que tem lista de Funcionario — serializar isso direto entraria em
 * recursão infinita. Estes records são cópias planas, só com o que a tela usa.
 */
public class Dtos {

    public record ModalidadeDto(int id, String nome, String descricao) {}

    public record InstrutorDto(int id, String nome, String registro, String especialidade) {}

    public record EstabelecimentoDto(int id, String nome, String tipo, double valorPorCheckIn,
                                     String situacao, List<ModalidadeDto> modalidades,
                                     List<InstrutorDto> instrutores) {}

    public record DependenteDto(String nome, String grauParentesco, boolean elegivel) {}

    public record FuncionarioDto(int id, String nome, String matricula, String plano,
                                 boolean elegivel, boolean planoPermiteDependentes,
                                 List<DependenteDto> dependentes) {}

    public record EmpresaDto(int id, String razaoSocial, int limiteDependentes,
                             int totalFuncionarios, int totalDependentes,
                             int totalBeneficiarios, List<FuncionarioDto> funcionarios) {}

    public record PlanoDto(int id, String nome, int nivel, double valorMensal,
                           int limiteAulasMes, boolean permiteDependentes) {}

    public record AulaDto(int id, String inicio, String inicioLegivel, long duracaoMin,
                          int capacidade, String modalidade, String instrutor,
                          boolean cancelada, int estabelecimentoId) {}

    public record EstadoDto(List<EstabelecimentoDto> estabelecimentos,
                            List<EmpresaDto> empresas,
                            List<PlanoDto> planos,
                            List<AulaDto> aulas) {}

    public record Erro(String mensagem) {}
}
