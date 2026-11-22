package persistencia;

import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Empresa;
import dominio.cadastroRede.Estabelecimento;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import persistencia.jpa.*;

import java.time.YearMonth;
import java.util.List;

/**
 * Implementação do Repositorio sobre o PostgreSQL. Delega a uma interface
 * Spring Data por agregado; o Spring gera as implementações delas.
 *
 * Não abre transação: quem demarca a transação são os Services. Assim
 * todas as leituras e escritas de uma operação de negócio caem no mesmo
 * contexto de persistência, e o mesmo registro é sempre o mesmo objeto Java
 * (as comparações com == do domínio continuam valendo).
 */
@Repository
public class RepositorioPostgres implements Repositorio {

    private final EstabelecimentoJpa estabelecimentos;
    private final EmpresaJpa empresas;
    private final PlanoJpa planos;
    private final AulaJpa aulas;
    private final AlunoJpa alunos;
    private final AdesaoJpa adesoes;
    private final ReservaJpa reservas;
    private final CheckInJpa checkIns;

    /**
     * Só para gravar as entidades de chave natural (ver inserir).
     * Injetado por campo: é um proxy compartilhado que encaminha para o
     * contexto de persistência da transação corrente.
     */
    @PersistenceContext
    private EntityManager entityManager;

    public RepositorioPostgres(EstabelecimentoJpa estabelecimentos, EmpresaJpa empresas,
                               PlanoJpa planos, AulaJpa aulas, AlunoJpa alunos,
                               AdesaoJpa adesoes, ReservaJpa reservas, CheckInJpa checkIns) {
        this.estabelecimentos = estabelecimentos;
        this.empresas = empresas;
        this.planos = planos;
        this.aulas = aulas;
        this.alunos = alunos;
        this.adesoes = adesoes;
        this.reservas = reservas;
        this.checkIns = checkIns;
    }

    /** CNPJ e CPF são guardados só com dígitos; quem chama pode mandar com pontuação. */
    private static String digitos(String chave) {
        return chave == null ? "" : chave.replaceAll("\\D", "");
    }

    /**
     * Grava uma entidade nova cuja chave já vem preenchida (cnpj, cpf).
     *
     * save() do Spring Data não serve aqui: com a chave preenchida ele acha que
     * a entidade já existe e faz merge(), que grava uma CÓPIA e deixa o objeto
     * original fora do contexto de persistência. A Adesao gravada logo depois
     * apontaria para esse original, e o Hibernate o veria como não salvo.
     * persist() coloca o próprio objeto no contexto.
     */
    private void inserir(Object entidade) {
        entityManager.persist(entidade);
    }

    // ---------- Estabelecimento ----------

    @Override
    public Estabelecimento buscarEstabelecimento(String cnpj) {
        return estabelecimentos.findById(digitos(cnpj)).orElse(null);
    }

    @Override
    public List<Estabelecimento> listarEstabelecimentos() {
        return estabelecimentos.findAll(Sort.by("nomeFantasia"));
    }

    @Override
    public void adicionarEstabelecimento(Estabelecimento e) { inserir(e); }

    // ---------- Empresa ----------

    @Override
    public Empresa buscarEmpresa(String cnpj) {
        return empresas.findById(digitos(cnpj)).orElse(null);
    }

    @Override
    public List<Empresa> listarEmpresas() {
        return empresas.findAll(Sort.by("razaoSocial"));
    }

    @Override
    public void adicionarEmpresa(Empresa e) { inserir(e); }

    // ---------- Plano ----------

    @Override
    public Plano buscarPlano(int id) { return planos.findById(id).orElse(null); }

    @Override
    public List<Plano> listarPlanos() { return planos.findAll(Sort.by("id")); }

    /** Id SERIAL ainda nulo: save() reconhece a entidade como nova e faz persist(). */
    @Override
    public void adicionarPlano(Plano p) { planos.save(p); }

    // ---------- Aula ----------

    /**
     * Trava a linha da aula até o fim da transação (ver AulaJpa.buscarComTrava).
     * Só vale como trava se a aula ainda não estiver carregada na transação;
     * por isso os Services chamam buscarAula antes de navegar até ela.
     */
    @Override
    public Aula buscarAula(int id) { return aulas.buscarComTrava(id).orElse(null); }

    @Override
    public List<Aula> listarAulas() { return aulas.findAll(Sort.by("id")); }

    @Override
    public void adicionarAula(Aula a) { aulas.save(a); }

    // ---------- Aluno ----------

    @Override
    public Aluno buscarAluno(String cpf) {
        return alunos.findById(digitos(cpf)).orElse(null);
    }

    @Override
    public List<Aluno> listarAlunos() { return alunos.findAll(Sort.by("nome")); }

    @Override
    public void adicionarAluno(Aluno a) { inserir(a); }

    // ---------- Adesao ----------

    /**
     * Busca todas as adesões do aluno e escolhe a vigente em Java.
     * "Vigente" é decisão do padrão State (estaVigente()), não da coluna
     * situacao: um WHERE situacao IN (...) duplicaria a regra no SQL.
     */
    @Override
    public Adesao adesaoDe(Aluno aluno) {
        return adesoes.findByAluno(aluno).stream()
            .filter(Adesao::estaVigente)
            .findFirst().orElse(null);
    }

    @Override
    public void adicionarAdesao(Adesao a) { adesoes.save(a); }

    // ---------- Reserva ----------

    @Override
    public Reserva buscarReserva(int id) { return reservas.findById(id).orElse(null); }

    @Override
    public List<Reserva> listarReservas() { return reservas.findAll(Sort.by("id")); }

    @Override
    public int totalReservasNoMes(Aluno aluno, YearMonth mes) {
        return (int) reservas.contarNoLimite(aluno,
            mes.atDay(1).atStartOfDay(), mes.plusMonths(1).atDay(1).atStartOfDay());
    }

    @Override
    public void adicionarReserva(Reserva r) { reservas.save(r); }

    // ---------- CheckIn ----------

    @Override
    public CheckIn ultimoCheckInDe(Aluno aluno) {
        return checkIns.findFirstByAlunoOrderByDataHoraDescIdDesc(aluno).orElse(null);
    }

    @Override
    public CheckIn checkInPorCodigo(String codigo) {
        if (codigo == null) return null;
        return checkIns.findFirstByCodigoOrderByDataHoraDescIdDesc(codigo.trim().toUpperCase())
            .orElse(null);
    }

    @Override
    public List<CheckIn> listarCheckIns() { return checkIns.findAll(Sort.by("id")); }

    @Override
    public void adicionarCheckIn(CheckIn c) { checkIns.save(c); }
}
