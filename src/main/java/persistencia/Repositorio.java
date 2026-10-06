package persistencia;

import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.acessoAgenda.SituacaoReserva;
import dominio.cadastroRede.*;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;
import dominio.planosAdesao.SituacaoAdesao;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Camada de persistência da arquitetura lógica.
 *
 * Nesta iteração o armazenamento é em memória: os dados vivem enquanto o
 * servidor estiver rodando. A substituição por banco de dados não afeta as
 * camadas acima, que só enxergam esta classe.
 */
@Component
public class Repositorio {

    private final List<Estabelecimento> estabelecimentos = new ArrayList<>();
    private final List<Empresa> empresas = new ArrayList<>();
    private final List<Plano> planos = new ArrayList<>();
    private final List<Aula> aulas = new ArrayList<>();
    private final List<Adesao> adesoes = new ArrayList<>();
    private final List<Aluno> alunos = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private final List<CheckIn> checkIns = new ArrayList<>();

    public Repositorio() { semear(); }

    public List<Estabelecimento> getEstabelecimentos() { return estabelecimentos; }
    public List<Empresa> getEmpresas() { return empresas; }
    public List<Plano> getPlanos() { return planos; }
    public List<Aula> getAulas() { return aulas; }
    public List<Adesao> getAdesoes() { return adesoes; }
    public List<Aluno> getAlunos() { return alunos; }
    public List<Reserva> getReservas() { return reservas; }
    public List<CheckIn> getCheckIns() { return checkIns; }

    /** Adesão vigente de um aluno, ou null se não houver. */
    public Adesao adesaoDe(Aluno aluno) {
        return adesoes.stream()
                .filter(a -> a.getAluno() == aluno)
                .filter(a -> a.getSituacao() != SituacaoAdesao.ENCERRADA)
                .findFirst().orElse(null);
    }

    /**
     * UC03 - check-in mais recente do aluno, ou null se nunca fez nenhum.
     * Se ele é de hoje ou não, quem decide é Adesao.registrarCheckIn (RN2).
     */
    public CheckIn ultimoCheckInDe(Aluno aluno) {
        CheckIn ultimo = null;
        for (CheckIn c : checkIns) {
            if (c.getAluno() == aluno) {
                ultimo = c;   // a lista está em ordem de registro
            }
        }
        return ultimo;
    }

    /**
     * UC04a - reservas que o aluno fez no mês informado e que contam para o
     * limite do plano. Conta pela data em que a reserva foi feita, não pela
     * data da aula.
     *
     * Cancelada no prazo (UC04b) devolve a aula ao limite; cancelada com
     * penalidade continua contando.
     */
    public int totalReservasNoMes(Aluno aluno, YearMonth mes) {
        int total = 0;
        for (Reserva r : reservas) {
            boolean contaNoLimite = r.getSituacao() != SituacaoReserva.CANCELADA
                    || r.isPenalizada();
            if (r.getAluno() == aluno && contaNoLimite
                    && YearMonth.from(r.getDataHoraReserva()).equals(mes)) {
                total++;
            }
        }
        return total;
    }

    /** Aluno com este CPF, ou null. Ignora a pontuação. */
    public Aluno alunoPorCpf(String cpf) {
        for (Aluno a : alunos) {
            if (a.temCpf(cpf)) {
                return a;
            }
        }
        return null;
    }

    /** UC07 - check-in com este código, ou null. Ignora maiúsculas e espaços. */
    public CheckIn checkInPorCodigo(String codigo) {
        if (codigo == null) return null;
        String procurado = codigo.trim().toUpperCase();
        for (CheckIn c : checkIns) {
            if (c.getCodigo().equals(procurado)) {
                return c;
            }
        }
        return null;
    }

    private void semear() {
        Empresa tech = new Empresa("Tech Ltda", "11.111.111/0001-11",
                LocalDate.of(2026, 1, 10), 2, 10);
        Empresa norte = new Empresa("Norte Log", "33.333.333/0001-33",
                LocalDate.of(2026, 2, 3), 1, 5);
        empresas.add(tech);
        empresas.add(norte);

        Estudio studio = new Estudio("Studio Movimento", "22.222.222/0001-22", 12.5,
                new Endereco("Rua Lauro Linhares", "1200", "Trindade", "Florianópolis",
                        "88036-002", -27.5893, -48.5196), 15);
        studio.aprovarCredenciamento();
        Academia forte = new Academia("Academia Forte", "44.444.444/0001-44", 9.9,
                new Endereco("Rua Deputado Antônio Edu Vieira", "800", "Pantanal",
                        "Florianópolis", "88040-001", -27.6036, -48.5210), true);
        forte.aprovarCredenciamento();
        Quadra areia = new Quadra("Quadra Areia", "55.555.555/0001-55", 15,
                new Endereco("Avenida das Rendeiras", "300", "Lagoa da Conceição",
                        "Florianópolis", "88062-400", -27.6037, -48.4657), false);
        estabelecimentos.add(studio);
        estabelecimentos.add(forte);
        estabelecimentos.add(areia);

        Modalidade pilates = new Modalidade("Pilates", "Aulas em grupo com aparelhos");
        Modalidade yoga = new Modalidade("Yoga", "Hatha e Vinyasa");
        studio.adicionarModalidade(pilates);
        studio.adicionarModalidade(yoga);
        Instrutor ana = studio.contratarInstrutor("Ana Reis", "CREF-1234", "Pilates");
        Instrutor caio = studio.contratarInstrutor("Caio Melo", "CREF-5678", "Yoga");

        Modalidade musculacao = new Modalidade("Musculacao", "Acesso livre a sala");
        forte.adicionarModalidade(musculacao);
        forte.contratarInstrutor("Rui Barros", "CREF-9999", "Musculacao");

        Plano premium = new Plano("Premium", 3, 189.9, 8, true);
        Plano basico = new Plano("Basico", 1, 79.9, 4, false);
        for (Plano p : List.of(premium, basico)) {
            p.oferecerPara(tech);
            p.oferecerPara(norte);
            planos.add(p);
        }

        // RF5 - Premium cobre o estúdio (só Pilates) e a academia; Basico só a academia.
        premium.liberar(studio);
        premium.liberar(pilates);
        premium.liberar(forte);
        premium.liberar(musculacao);
        basico.liberar(forte);
        basico.liberar(musculacao);

        Funcionario lara = new Funcionario("Lara Souza", "000.000.000-00", "lara@tech.com",
                "F-001", LocalDate.of(2025, 3, 1), tech);
        Funcionario bruno = new Funcionario("Bruno Dias", "222.222.222-22", "bruno@tech.com",
                "F-002", LocalDate.of(2024, 8, 15), tech);
        Adesao adLara = Adesao.cadastrarBeneficiario(lara, premium);
        adesoes.add(adLara);
        adesoes.add(Adesao.cadastrarBeneficiario(bruno, basico));

        Dependente miguel = new Dependente("Miguel Souza", "111.111.111-11",
                "miguel@mail.com", "Filho", lara);
        adesoes.add(adLara.incluirDependente(miguel));

        alunos.add(lara);
        alunos.add(bruno);
        alunos.add(miguel);

        LocalDateTime amanha = LocalDateTime.now().plusDays(1)
                .withHour(7).withMinute(0).withSecond(0).withNano(0);
        aulas.add(studio.agendarAula(amanha, Duration.ofMinutes(50), 8, pilates, ana));
        aulas.add(studio.agendarAula(amanha.plusHours(11), Duration.ofMinutes(60), 2, pilates, ana));
        aulas.add(studio.agendarAula(amanha.plusHours(12), Duration.ofMinutes(60), 10, yoga, caio));

        // UC04b - começa em 90 min: o prazo sem penalidade (2 h antes, RN5) já passou,
        // então reservar e cancelar esta aula gera cancelamento penalizado.
        LocalDateTime daquiA90 = LocalDateTime.now().plusMinutes(90).withSecond(0).withNano(0);
        aulas.add(studio.agendarAula(daquiA90, Duration.ofMinutes(50), 6, pilates, ana));
    }
}