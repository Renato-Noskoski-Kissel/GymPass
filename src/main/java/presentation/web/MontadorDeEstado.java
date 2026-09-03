package presentation.web;

import dominio.acessoAgenda.Aula;
import dominio.cadastroRede.*;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;
import org.springframework.stereotype.Component;
import persistencia.Repositorio;
import presentation.web.Dtos.*;

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
        return new EstadoDto(estabelecimentos(), empresas(), planos(), aulas());
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

            saida.add(new EstabelecimentoDto(i, e.getNomeFantasia(), tipoDe(e),
                    e.getValorPorCheckIn(), e.getSituacaoCredenciamento().name(), mods, ins));
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
                    p.getLimiteAulasMes(), p.permiteDependentes()));
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
                    a.getModalidade().getNome(),
                    a.getInstrutor().getNome(),
                    a.estaCancelada(),
                    repositorio.getEstabelecimentos().indexOf(a.getEstabelecimento())));
        }
        return saida;
    }
}
