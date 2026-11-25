package presentation.web;

import java.util.List;

/**
 * Objetos de transferência.
 *
 * As classes de domínio NÃO são enviadas ao navegador. Funcionario aponta para
 * Empresa, que tem lista de Funcionario — serializar isso direto entraria em
 * recursão infinita. Estes records são cópias planas, só com o que a tela usa.
 *
 * Cada registro é identificado pela chave do banco: cnpj, cpf, registro ou id.
 * CNPJ e CPF vão só com dígitos; a pontuação é posta pelo app.js, na tela.
 */
public class Dtos {

    public record ModalidadeDto(int id, String nome, String descricao) {}

    public record InstrutorDto(String registro, String nome, String especialidade) {}

    public record EstabelecimentoDto(String cnpj, String nome, String tipo, double valorPorCheckIn,
                                     int nivelMinimo, String situacao, String endereco,
                                     double latitude, double longitude,
                                     List<ModalidadeDto> modalidades,
                                     List<InstrutorDto> instrutores) {}

    public record DependenteDto(String nome, String cpf, String grauParentesco, boolean elegivel,
                                String situacaoAdesao) {}

    public record FuncionarioDto(String cpf, String nome, String matricula, String plano,
                                 boolean elegivel, boolean planoPermiteDependentes,
                                 String situacaoAdesao, boolean podeSuspender,
                                 boolean podeReativar, List<DependenteDto> dependentes) {}

    public record EmpresaDto(String cnpj, String razaoSocial, int limiteDependentes,
                             int totalFuncionarios, int totalDependentes,
                             int totalBeneficiarios, List<FuncionarioDto> funcionarios,
                             List<Integer> planosOferecidos) {}

    public record PlanoDto(int id, String nome, int nivel, double valorMensal,
                           int limiteAulasMes, boolean permiteDependentes,
                           List<String> cobertura) {}

    /** UC01 passo 7 - resumo da assinatura. dependentes: os que seguem cobertos. */
    public record AssinaturaDto(int id, PlanoDto plano, String dataInicio, String situacao,
                                List<String> dependentes) {}

    /**
     * UC02 passo 6 - um item da lista. nivelMinimo é o nível de plano que libera
     * o estabelecimento (extensão 5a, quando coberto = false).
     */
    public record ResultadoBuscaDto(String cnpj, String nome, String tipo, String endereco,
                                    double distanciaMetros, boolean coberto, int nivelMinimo,
                                    List<String> modalidades) {}

    /**
     * UC02 passo 7 - os dados do item da lista (7a a 7d), o nível do plano do
     * aluno para comparar com o exigido, e a grade do período (7e).
     */
    public record DetalheEstabelecimentoDto(ResultadoBuscaDto estabelecimento, int nivelDoPlano,
                                            List<AulaDto> aulas) {}

    public record AulaDto(int id, String inicio, String inicioLegivel, long duracaoMin,
                          int capacidade, int vagasDisponiveis, String modalidade,
                          String instrutor, boolean cancelada, boolean jaComecou,
                          String estabelecimentoCnpj, String estabelecimento) {}

    public record AlunoDto(String cpf, String nome, String tipo, String empresa, String plano,
                           int limiteAulasMes, int reservasNoMes, boolean elegivel) {}

    public record CheckInDto(int id, String alunoCpf, String aluno, String estabelecimentoCnpj,
                             String estabelecimento, String quando, boolean hoje,
                             String codigo, String situacao, String motivoRecusa) {}

    /** UC07 passo 4 - só o que a recepção pode ver (RNF3). */
    public record ConferenciaDto(String codigo, String aluno, String quando, String situacao) {}

    public record ReservaDto(int id, String alunoCpf, int aulaId, String aula,
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

    /** UC01 5a - a operação só segue se o aluno confirmar a perda destes dependentes. */
    public record ConfirmacaoNecessaria(String mensagem, List<String> dependentes) {}
}
