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
                                     String situacao, String endereco,
                                     double latitude, double longitude,
                                     List<ModalidadeDto> modalidades,
                                     List<InstrutorDto> instrutores) {}

    public record DependenteDto(String nome, String grauParentesco, boolean elegivel) {}

    public record FuncionarioDto(int id, String nome, String matricula, String plano,
                                 boolean elegivel, boolean planoPermiteDependentes,
                                 List<DependenteDto> dependentes) {}

    public record EmpresaDto(int id, String razaoSocial, int limiteDependentes,
                             int totalFuncionarios, int totalDependentes,
                             int totalBeneficiarios, List<FuncionarioDto> funcionarios) {}

    /** RF5 - o que um plano cobre em um estabelecimento. */
    public record CoberturaDto(String estabelecimento, boolean estabelecimentoLiberado,
                               List<String> modalidades) {}

    public record PlanoDto(int id, String nome, int nivel, double valorMensal,
                           int limiteAulasMes, boolean permiteDependentes,
                           List<CoberturaDto> cobertura) {}

    public record AulaDto(int id, String inicio, String inicioLegivel, long duracaoMin,
                          int capacidade, int vagasDisponiveis, String modalidade,
                          String instrutor, boolean cancelada, boolean jaComecou,
                          int estabelecimentoId, String estabelecimento) {}

    public record AlunoDto(int id, String nome, String tipo, String empresa, String plano,
                           int limiteAulasMes, int reservasNoMes, boolean elegivel) {}

    public record CheckInDto(int id, int alunoId, String estabelecimento, String quando,
                             boolean hoje, String situacao) {}

    public record ReservaDto(int id, int alunoId, int aulaId, String aula, String quando,
                             String feitaEm, String situacao) {}

    public record EstadoDto(List<EstabelecimentoDto> estabelecimentos,
                            List<EmpresaDto> empresas,
                            List<PlanoDto> planos,
                            List<AulaDto> aulas,
                            List<AlunoDto> alunos,
                            List<CheckInDto> checkIns,
                            List<ReservaDto> reservas) {}

    public record Erro(String mensagem) {}
}
