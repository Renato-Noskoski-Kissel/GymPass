package presentation.web;

import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.acessoAgenda.SituacaoCheckIn;
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

/**
 * Converte os objetos de domínio nos DTOs que a tela consome.
 *
 * Roda fora dos Services, mas dentro da mesma requisição: com
 * spring.jpa.open-in-view o contexto de persistência continua aberto até a
 * resposta ser escrita, então as coleções preguiçosas (modalidades,
 * funcionários, reservas da aula...) ainda podem ser carregadas aqui.
 */
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
        for (Estabelecimento e : repositorio.listarEstabelecimentos()) {
            List<ModalidadeDto> mods = new ArrayList<>();
            for (Modalidade m : e.getModalidades()) {
                mods.add(new ModalidadeDto(m.getId(), m.getNome(), m.getDescricao()));
            }

            List<InstrutorDto> ins = new ArrayList<>();
            for (Instrutor t : e.getInstrutores()) {
                ins.add(new InstrutorDto(t.getRegistro(), t.getNome(), t.getEspecialidade()));
            }

            Endereco end = e.getEndereco();
            saida.add(new EstabelecimentoDto(e.getCnpj(), e.getNomeFantasia(), tipoDe(e),
                    e.getValorPorCheckIn(), e.getNivelMinimo(), e.getSituacaoCredenciamento().name(),
                    end.toString(), end.getLatitude(), end.getLongitude(), mods, ins));
        }
        return saida;
    }

    /** UC02 passos 5 e 6 - distância a partir da referência e cobertura pelo plano (RF5). */
    public ResultadoBuscaDto resultadoBusca(Estabelecimento e, Plano plano, double lat, double lng) {
        return new ResultadoBuscaDto(e.getCnpj(), e.getNomeFantasia(), tipoDe(e),
                e.getEndereco().toString(), e.getEndereco().distanciaAte(lat, lng),
                plano.libera(e), e.getNivelMinimo(),
                e.getModalidades().stream().map(Modalidade::getNome).toList());
    }

    private String tipoDe(Estabelecimento e) {
        if (e instanceof Academia) return "Academia";
        if (e instanceof Estudio) return "Estudio";
        if (e instanceof Quadra) return "Quadra";
        return "Estabelecimento";
    }

    private List<EmpresaDto> empresas() {
        List<EmpresaDto> saida = new ArrayList<>();
        for (Empresa emp : repositorio.listarEmpresas()) {
            List<FuncionarioDto> funcs = new ArrayList<>();
            for (Funcionario f : emp.getFuncionarios()) {
                Adesao ad = repositorio.adesaoDe(f);

                List<DependenteDto> deps = new ArrayList<>();
                for (Dependente d : f.getDependentes()) {
                    deps.add(new DependenteDto(d.getNome(), d.getCpf(), d.getGrauParentesco(),
                            d.ehElegivel(), situacaoDe(repositorio.adesaoDe(d))));
                }

                funcs.add(new FuncionarioDto(f.getCpf(), f.getNome(), f.getMatricula(),
                        ad == null ? null : ad.getPlano().getNome(),
                        f.ehElegivel(),
                        ad != null && ad.getPlano().permiteDependentes(),
                        situacaoDe(ad),
                        ad != null && ad.podeSuspender(),
                        ad != null && ad.podeReativar(),
                        deps));
            }

            saida.add(new EmpresaDto(emp.getCnpj(), emp.getRazaoSocial(), emp.getLimiteDependentes(),
                    emp.getTotalFuncionarios(), emp.getTotalDependentes(),
                    emp.getTotalBeneficiarios(), funcs));
        }
        return saida;
    }

    private List<PlanoDto> planos() {
        return planos(repositorio.listarPlanos());
    }

    /** UC01 passo 3 - também usado para os planos que a empresa do aluno oferece. */
    public List<PlanoDto> planos(List<Plano> planos) {
        List<PlanoDto> saida = new ArrayList<>();
        List<Estabelecimento> rede = repositorio.listarEstabelecimentos();
        for (Plano p : planos) {
            saida.add(planoDto(p, rede));
        }
        return saida;
    }

    private PlanoDto planoDto(Plano p, List<Estabelecimento> rede) {
        return new PlanoDto(p.getId(), p.getNome(), p.getNivel(), p.getValorMensal(),
                p.getLimiteAulasMes(), p.permiteDependentes(), coberturaDe(p, rede));
    }

    /** UC01 passo 7 - resumo da assinatura, com os dependentes que continuam cobertos. */
    public AssinaturaDto assinatura(Adesao a) {
        List<String> dependentes = new ArrayList<>();
        if (a.getAluno() instanceof Funcionario f) {
            for (Dependente d : f.getDependentes()) {
                if (d.estaAtivo() && repositorio.adesaoDe(d) != null) dependentes.add(d.getNome());
            }
        }
        return new AssinaturaDto(a.getId(),
                planoDto(a.getPlano(), repositorio.listarEstabelecimentos()),
                a.getDataInicio().toString(), a.getSituacao().name(), dependentes);
    }

    /** RF5 - nomes dos estabelecimentos que o nível do plano alcança. */
    private List<String> coberturaDe(Plano p, List<Estabelecimento> rede) {
        List<String> saida = new ArrayList<>();
        for (Estabelecimento e : rede) {
            if (p.libera(e)) saida.add(e.getNomeFantasia());
        }
        return saida;
    }

    private List<AulaDto> aulas() {
        List<AulaDto> saida = new ArrayList<>();
        for (Aula a : repositorio.listarAulas()) {
            saida.add(aulaDto(a));
        }
        return saida;
    }

    private AulaDto aulaDto(Aula a) {
        return new AulaDto(a.getId(),
                a.getDataHoraInicio().format(ISO),
                a.getDataHoraInicio().format(LEGIVEL),
                a.getDuracao().toMinutes(),
                a.getCapacidadeMaxima(),
                a.vagasDisponiveis(),
                a.getModalidade().getNome(),
                a.getInstrutor().getNome(),
                a.estaCancelada(),
                a.jaComecou(),
                a.getEstabelecimento().getCnpj(),
                a.getEstabelecimento().getNomeFantasia());
    }

    /** UC02 passo 7 - detalhe do estabelecimento com a grade do período (7e). */
    public DetalheEstabelecimentoDto detalhe(Estabelecimento e, Plano plano, double lat, double lng,
                                             LocalDateTime inicio, LocalDateTime fim) {
        return new DetalheEstabelecimentoDto(resultadoBusca(e, plano, lat, lng), plano.getNivel(),
                e.aulasEntre(inicio, fim).stream().map(this::aulaDto).toList());
    }

    private List<AlunoDto> alunos() {
        List<AlunoDto> saida = new ArrayList<>();
        YearMonth mes = YearMonth.now();
        for (Aluno al : repositorio.listarAlunos()) {
            Adesao ad = repositorio.adesaoDe(al);
            Empresa emp = al.getEmpresaVinculada();
            saida.add(new AlunoDto(al.getCpf(), al.getNome(),
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
        LocalDate hoje = LocalDate.now();
        for (CheckIn c : repositorio.listarCheckIns()) {
            saida.add(new CheckInDto(c.getId(),
                    c.getAluno().getCpf(),
                    c.getAluno().getNome(),
                    c.getEstabelecimento().getCnpj(),
                    c.getEstabelecimento().getNomeFantasia(),
                    c.getDataHora().format(LEGIVEL),
                    c.getDataHora().toLocalDate().equals(hoje),
                    c.getCodigo(),
                    situacaoDe(c),
                    c.getMotivoRecusa()));
        }
        return saida;
    }

    /** UC07 passo 4 - o que a recepção vê ao localizar um check-in. */
    public ConferenciaDto conferencia(CheckIn c) {
        return new ConferenciaDto(c.getCodigo(), c.getAluno().getNome(),
                c.getDataHora().format(LEGIVEL), situacaoDe(c));
    }

    /**
     * UC03 7a - um pendente vencido ainda não foi marcado como expirado (isso só
     * acontece quando alguém tenta validá-lo), mas para a tela ele já expirou.
     */
    /** ATIVA / SUSPENSA / ENCERRADA, ou null se o aluno não tem adesão vigente. */
    private String situacaoDe(Adesao ad) {
        return ad == null ? null : ad.getSituacao().name();
    }

    private String situacaoDe(CheckIn c) {
        return c.expirou(LocalDateTime.now())
            ? SituacaoCheckIn.EXPIRADO.name()
            : c.getSituacaoValidacao().name();
    }

    private List<ReservaDto> reservas() {
        List<ReservaDto> saida = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        for (Reserva r : repositorio.listarReservas()) {
            Aula a = r.getAula();
            saida.add(new ReservaDto(r.getId(),
                    r.getAluno().getCpf(),
                    a.getId(),
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
