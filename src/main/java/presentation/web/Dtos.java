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

    public record DependenteDto(String nome, String cpf, String grauParentesco, boolean elegivel) {}

    public record FuncionarioDto(int id, String nome, String cpf, String matricula, String plano,
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

    public record CheckInDto(int id, int alunoId, String aluno, int estabelecimentoId,
                             String estabelecimento, String quando, boolean hoje,
                             String codigo, String situacao, String motivoRecusa) {}

    /** UC07 passo 4 - só o que a recepção pode ver (RNF3). */
    public record ConferenciaDto(String codigo, String aluno, String quando, String situacao) {}

    public record ReservaDto(int id, int alunoId, int aulaId, String aula,
                             String inicio, String quando, long duracaoMin,
                             String instrutor, String endereco, String feitaEm,
                             String situacao, boolean penalizada,
                             boolean aulaCancelada, boolean aulaJaComecou,
                             String prazoSemPenalidade, String tempoAtePrazo,
                             boolean cancelamentoPenalizado) {}

    public record EstadoDto(List<EstabelecimentoDto> estabelecimentos,
                            List<EmpresaDto> empresas,
                            List<PlanoDto> planos,
                            List<AulaDto> aulas,
                            List<AlunoDto> alunos,
                            List<CheckInDto> checkIns,
                            List<ReservaDto> reservas) {}

    public record Erro(String mensagem) {}
}