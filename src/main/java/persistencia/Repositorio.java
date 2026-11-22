package persistencia;

import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Empresa;
import dominio.cadastroRede.Estabelecimento;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;

import java.time.YearMonth;
import java.util.List;

/**
 * Camada de persistência da arquitetura lógica (docs/dcp-oficial.puml).
 *
 * As camadas de cima só enxergam esta interface; quem guarda os dados é
 * RepositorioPostgres. Os buscarX devolvem null quando não encontram:
 * decidir se a ausência é erro é papel de quem chama.
 *
 * CNPJ e CPF podem vir com ou sem pontuação; a implementação compara só os
 * dígitos, que é como eles são guardados.
 */
public interface Repositorio {

    // ---------- Estabelecimento ----------
    Estabelecimento buscarEstabelecimento(String cnpj);
    List<Estabelecimento> listarEstabelecimentos();
    void adicionarEstabelecimento(Estabelecimento e);

    // ---------- Empresa ----------
    Empresa buscarEmpresa(String cnpj);
    List<Empresa> listarEmpresas();
    void adicionarEmpresa(Empresa e);

    // ---------- Plano ----------
    Plano buscarPlano(int id);
    List<Plano> listarPlanos();
    void adicionarPlano(Plano p);

    // ---------- Aula ----------
    /** Trava a linha da aula até o fim da transação (UC04a 4a: disputa pela última vaga). */
    Aula buscarAula(int id);
    List<Aula> listarAulas();
    void adicionarAula(Aula a);

    // ---------- Aluno ----------
    Aluno buscarAluno(String cpf);
    List<Aluno> listarAlunos();
    void adicionarAluno(Aluno a);

    // ---------- Adesao ----------
    /** Adesão vigente do aluno (ativa ou suspensa), ou null se não houver. */
    Adesao adesaoDe(Aluno aluno);
    void adicionarAdesao(Adesao a);

    // ---------- Reserva ----------
    Reserva buscarReserva(int id);
    List<Reserva> listarReservas();
    /**
     * UC04a - reservas do aluno em aulas do mês informado, que contam para o
     * limite do plano. O mês é o da aula, não o do dia em que a reserva foi feita.
     * Cancelada no prazo (UC04b) devolve a aula ao limite; cancelada com
     * penalidade continua contando.
     */
    int totalReservasNoMes(Aluno aluno, YearMonth mes);
    void adicionarReserva(Reserva r);

    // ---------- CheckIn ----------
    /** UC03 - check-in mais recente do aluno, ou null. Se é de hoje, quem decide é a Adesao (RN2). */
    CheckIn ultimoCheckInDe(Aluno aluno);
    /** UC07 - check-in mais recente com este código, ou null. Ignora maiúsculas e espaços. */
    CheckIn checkInPorCodigo(String codigo);
    List<CheckIn> listarCheckIns();
    void adicionarCheckIn(CheckIn c);
}
