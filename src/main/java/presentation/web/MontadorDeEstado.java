package presentation.web;

import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.*;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;
import org.springframework.stereotype.Component;
import persistencia.Repositorio;
import presentation.web.Dtos.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Converte os objetos de domínio nos DTOs que a tela consome. */
@Component
public class MontadorDeEstado {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter LEGIVEL =
        DateTimeFormatter.ofPattern("EEE dd/MM 'às' HH:mm", new Locale("pt", "BR"));

    private final Repositorio repositorio;

    public MontadorDeEstado(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public EstadoDto montar() {
        return new EstadoDto(estabelecimentos(), empresas(), planos(), aulas(),
                alunos(), checkIns(), reservas());
    }

    private List<EstabelecimentoDto> estabelecimentos() {
        List<EstabelecimentoDto> saida = new ArrayList<>();
        List<Estabelecimento> lista = repositorio.getEstabelecimentos();
        for (int i = 0; i < lista.size(); i++) {
            Estabelecimento e = lista.get(i);

            List<ModalidadeDto> mods = new ArrayList<>();
            for (int j = 0; j < e.getModalidades().size(); j++) {
                Modalidade m = e.getModalidades().get(j);
                mods.add(new ModalidadeDto(j, m.getNome(), m.getDescricao()));
            }

            List<InstrutorDto> ins = new ArrayList<>();
            for (int j = 0; j < e.getInstrutores().size(); j++) {
                Instrutor t = e.getInstrutores().get(j);
                ins.add(new InstrutorDto(j, t.getNome(), t.getRegistro(), t.getEspecialidade()));
            }

            Endereco end = e.getEndereco();
            saida.add(new EstabelecimentoDto(i, e.getNomeFantasia(), tipoDe(e),
                    e.getValorPorCheckIn(), e.getSituacaoCredenciamento().name(),
                    end.toString(), end.getLatitude(), end.getLongitude(), mods, ins));
        }
        return saida;
    }

    private String tipoDe(Estabelecimento e) {
        if (e instanceof Academia) return "Academia";
        if (e instanceof Estudio) return "Estudio";
        if (e instanceof Quadra) return "Quadra";
        return "Estabelecimento";
    }

    private List<EmpresaDto> empresas() {
        List<EmpresaDto> saida = new ArrayList<>();
        List<Empresa> lista = repositorio.getEmpresas();
        for (int i = 0; i < lista.size(); i++) {
            Empresa emp = lista.get(i);

            List<FuncionarioDto> funcs = new ArrayList<>();
            for (int j = 0; j < emp.getFuncionarios().size(); j++) {
                Funcionario f = emp.getFuncionarios().get(j);
                Adesao ad = repositorio.adesaoDe(f);

                List<DependenteDto> deps = new ArrayList<>();
                for (Dependente d : f.getDependentes()) {
                    deps.add(new DependenteDto(d.getNome(), d.getGrauParentesco(), d.ehElegivel()));
                }

                funcs.add(new FuncionarioDto(j, f.getNome(), f.getMatricula(),
                        ad == null ? null : ad.getPlano().getNome(),
                        f.ehElegivel(),
                        ad != null && ad.getPlano().permiteDependentes(),
                        deps));
            }

            saida.add(new EmpresaDto(i, emp.getRazaoSocial(), emp.getLimiteDependentes(),
                    emp.getTotalFuncionarios(), emp.getTotalDependentes(),
                    emp.getTotalBeneficiarios(), funcs));
        }
        return saida;
    }

    private List<PlanoDto> planos() {
        List<PlanoDto> saida = new ArrayList<>();
        List<Plano> lista = repositorio.getPlanos();
        for (int i = 0; i < lista.size(); i++) {
            Plano p = lista.get(i);
            saida.add(new PlanoDto(i, p.getNome(), p.getNivel(), p.getValorMensal(),
                    p.getLimiteAulasMes(), p.permiteDependentes(), coberturaDe(p)));
        }
        return saida;
    }

    /** Agrupa a cobertura do plano por estabelecimento, só para exibição. */
    private List<CoberturaDto> coberturaDe(Plano p) {
        List<CoberturaDto> saida = new ArrayList<>();
        for (Estabelecimento e : repositorio.getEstabelecimentos()) {
            List<String> mods = new ArrayList<>();
            for (Modalidade m : e.getModalidades()) {
                if (p.libera(m)) mods.add(m.getNome());
            }
            if (p.libera(e) || !mods.isEmpty()) {
                saida.add(new CoberturaDto(e.getNomeFantasia(), p.libera(e), mods));
            }
        }
        return saida;
    }

    private List<AulaDto> aulas() {
        List<AulaDto> saida = new ArrayList<>();
        List<Aula> lista = repositorio.getAulas();
        for (int i = 0; i < lista.size(); i++) {
            Aula a = lista.get(i);
            saida.add(new AulaDto(i,
                    a.getDataHoraInicio().format(ISO),
                    a.getDataHoraInicio().format(LEGIVEL),
                    a.getDuracao().toMinutes(),
                    a.getCapacidadeMaxima(),
                    a.vagasDisponiveis(),
                    a.getModalidade().getNome(),
                    a.getInstrutor().getNome(),
                    a.estaCancelada(),
                    a.jaComecou(),
                    repositorio.getEstabelecimentos().indexOf(a.getEstabelecimento()),
                    a.getEstabelecimento().getNomeFantasia()));
        }
        return saida;
    }

    private List<AlunoDto> alunos() {
        List<AlunoDto> saida = new ArrayList<>();
        List<Aluno> lista = repositorio.getAlunos();
        YearMonth mes = YearMonth.now();
        for (int i = 0; i < lista.size(); i++) {
            Aluno al = lista.get(i);
            Adesao ad = repositorio.adesaoDe(al);
            Empresa emp = al.getEmpresaVinculada();
            saida.add(new AlunoDto(i, al.getNome(),
                    al instanceof Dependente ? "Dependente" : "Funcionário",
                    emp == null ? null : emp.getRazaoSocial(),
                    ad == null ? null : ad.getPlano().getNome(),
                    ad == null ? 0 : ad.getPlano().getLimiteAulasMes(),
                    repositorio.totalReservasNoMes(al, mes),
                    ad != null && ad.estaAtiva()));
        }
        return saida;
    }

    private List<CheckInDto> checkIns() {
        List<CheckInDto> saida = new ArrayList<>();
        List<CheckIn> lista = repositorio.getCheckIns();
        LocalDate hoje = LocalDate.now();
        for (int i = 0; i < lista.size(); i++) {
            CheckIn c = lista.get(i);
            saida.add(new CheckInDto(i,
                    repositorio.getAlunos().indexOf(c.getAluno()),
                    c.getAluno().getNome(),
                    repositorio.getEstabelecimentos().indexOf(c.getEstabelecimento()),
                    c.getEstabelecimento().getNomeFantasia(),
                    c.getDataHora().format(LEGIVEL),
                    c.getDataHora().toLocalDate().equals(hoje),
                    c.getCodigo(),
                    c.getSituacaoValidacao().name(),
                    c.getMotivoRecusa()));
        }
        return saida;
    }

    /** UC07 passo 4 - o que a recepção vê ao localizar um check-in. */
    public ConferenciaDto conferencia(CheckIn c) {
        return new ConferenciaDto(c.getCodigo(), c.getAluno().getNome(),
                c.getDataHora().format(LEGIVEL), c.getSituacaoValidacao().name());
    }

    private List<ReservaDto> reservas() {
        List<ReservaDto> saida = new ArrayList<>();
        List<Reserva> lista = repositorio.getReservas();
        LocalDateTime agora = LocalDateTime.now();
        for (int i = 0; i < lista.size(); i++) {
            Reserva r = lista.get(i);
            Aula a = r.getAula();
            saida.add(new ReservaDto(i,
                    repositorio.getAlunos().indexOf(r.getAluno()),
                    repositorio.getAulas().indexOf(a),
                    a.getModalidade().getNome() + " em " + a.getEstabelecimento().getNomeFantasia(),
                    a.getDataHoraInicio().format(ISO),
                    a.getDataHoraInicio().format(LEGIVEL),
                    a.getDuracao().toMinutes(),
                    a.getInstrutor().getNome(),
                    a.getEstabelecimento().getEndereco().toString(),
                    r.getDataHoraReserva().format(LEGIVEL),
                    r.getSituacao().name(),
                    r.isPenalizada(),
                    a.estaCancelada(),
                    a.jaComecou(),
                    r.prazoSemPenalidade().format(LEGIVEL),
                    tempoAte(agora, r.prazoSemPenalidade()),
                    r.cancelamentoSeriaPenalizado(agora)));
        }
        return saida;
    }

    /** UC04b passo 2e - "faltam 1 d 3 h", "faltam 2 h 15 min"; null se já passou. */
    private String tempoAte(LocalDateTime agora, LocalDateTime limite) {
        if (!agora.isBefore(limite)) return null;
        Duration d = Duration.between(agora, limite);
        if (d.toDays() > 0) return "faltam " + d.toDays() + " d " + d.toHoursPart() + " h";
        if (d.toHours() > 0) return "faltam " + d.toHours() + " h " + d.toMinutesPart() + " min";
        return "faltam " + Math.max(1, d.toMinutes()) + " min";
    }
}
