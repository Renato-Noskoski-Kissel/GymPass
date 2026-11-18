package dominio.planosAdesao;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Dependente;
import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Funcionario;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Estado em que o benefício pode ser usado normalmente. */
public class Ativa implements EstadoAdesao {

    /**
     * RN3 - inclusão de dependente. Depende do plano (nem todo plano permite)
     * e do limite estipulado no contrato da empresa.
     *
     * O dependente recebe uma Adesao própria, sempre ao mesmo plano do
     * responsável — restrição que o diagrama de classes não expressa.
     */
    @Override
    public Adesao incluirDependente(Adesao ad, Dependente dependente) {
        Aluno aluno = ad.getAluno();
        Plano plano = ad.getPlano();
        if (!aluno.ehElegivel()) {
            throw new RegraDeNegocioException("Adesão não está ativa.");
        }
        if (!(aluno instanceof Funcionario funcionario)) {
            throw new RegraDeNegocioException("Somente funcionários podem incluir dependentes.");
        }
        if (dependente.getResponsavel() != funcionario) {
            throw new RegraDeNegocioException(dependente + " não é dependente de " + funcionario);
        }
        if (!plano.permiteDependentes()) {
            throw new RegraDeNegocioException(
                "O plano " + plano.getNome() + " não permite dependentes (RN3).");
        }
        int limite = funcionario.getEmpresaVinculada().getLimiteDependentes();
        if (funcionario.getDependentes().size() >= limite) {
            throw new RegraDeNegocioException(
                "Limite de dependentes da empresa atingido (RN3): " + limite);
        }

        funcionario.vincularDependente(dependente);
        return new Adesao(dependente, plano);
    }

    /**
     * UC03 - Realizar Check-in.
     *
     * Ordem das verificações, a mesma da UC: credenciamento (pré-condição),
     * proximidade (passo 4, inclui Validar Localização do Dispositivo) e
     * direito de acesso (passo 5, inclui Validar Elegibilidade).
     *
     * @param ultimo último check-in do aluno, ou null se nunca fez nenhum.
     *               A regra "um por dia" (RN2) é decidida aqui, não na consulta.
     */
    @Override
    public CheckIn registrarCheckIn(Adesao ad, Estabelecimento estabelecimento,
                                    double latitude, double longitude, CheckIn ultimo) {
        Aluno aluno = ad.getAluno();
        if (!estabelecimento.estaCredenciado()) {
            throw new RegraDeNegocioException(
                estabelecimento + " não tem credenciamento aprovado na rede.");
        }
        if (!estabelecimento.estaNoRaio(latitude, longitude)) {
            double distancia = estabelecimento.getEndereco().distanciaAte(latitude, longitude);
            throw new RegraDeNegocioException(String.format(
                "Você está a %.0f m de %s. O check-in só é aceito a até %.0f m (RN4).",
                distancia, estabelecimento, Estabelecimento.RAIO_MAXIMO_METROS));
        }
        if (!aluno.ehElegivel()) {
            throw new RegraDeNegocioException(
                "O vínculo de " + aluno + " com o benefício não está ativo (RN1).");
        }
        if (ultimo != null && ultimo.getDataHora().toLocalDate().equals(LocalDate.now())) {
            throw new RegraDeNegocioException(
                aluno + " já fez check-in hoje em " + ultimo.getEstabelecimento()
                + ". O limite é um por dia (RN2).");
        }
        if (!ad.getPlano().libera(estabelecimento)) {
            throw new RegraDeNegocioException(semCobertura(ad.getPlano(), estabelecimento));
        }
        return new CheckIn(aluno, estabelecimento, latitude, longitude);
    }

    /**
     * UC04a - Reservar Vaga em Aula.
     *
     * Verifica o direito de acesso (passo 2, inclui Validar Elegibilidade) e
     * delega à aula a verificação de vaga e a criação da reserva (passos 3 e 5).
     *
     * @param reservasNoMes reservas que o aluno já tem em aulas do mesmo mês desta aula.
     */
    @Override
    public Reserva reservarVaga(Adesao ad, Aula aula, int reservasNoMes) {
        Aluno aluno = ad.getAluno();
        Plano plano = ad.getPlano();
        if (!aluno.ehElegivel()) {
            throw new RegraDeNegocioException(
                "O vínculo de " + aluno + " com o benefício não está ativo (RN1).");
        }
        if (!plano.libera(aula.getEstabelecimento())) {
            throw new RegraDeNegocioException(semCobertura(plano, aula.getEstabelecimento()));
        }
        if (reservasNoMes >= plano.getLimiteAulasMes()) {
            String mes = YearMonth.from(aula.getDataHoraInicio())
                .format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", new Locale("pt", "BR")));
            throw new RegraDeNegocioException(
                "Limite de " + plano.getLimiteAulasMes() + " aulas por mês do plano "
                + plano.getNome() + " atingido para " + mes + ".");
        }
        return aula.reservar(aluno);
    }

    /**
     * HU2 - troca do nível de plano do funcionário.
     *
     * Com multiplicidade 1, a adesão anterior é substituída e não é preservada.
     * Se o histórico passar a ser necessário (cálculo de fatura por período),
     * a multiplicidade precisará mudar para 1..*.
     */
    @Override
    public Adesao trocarPlano(Adesao ad, Plano novoPlano) {
        Aluno aluno = ad.getAluno();
        if (!novoPlano.ehOferecidoPor(aluno.getEmpresaVinculada())) {
            throw new RegraDeNegocioException(
                "A empresa " + aluno.getEmpresaVinculada()
                + " não oferece o plano " + novoPlano.getNome());
        }
        if (aluno instanceof Funcionario funcionario
                && !funcionario.getDependentes().isEmpty()
                && !novoPlano.permiteDependentes()) {
            throw new RegraDeNegocioException(
                "O plano " + novoPlano.getNome() + " não permite dependentes (RN3). "
                + "Antes de trocar de plano, remova os dependentes de " + funcionario
                + " (" + funcionario.getDependentes().size() + " no total).");
        }
        encerrar(ad);
        return new Adesao(aluno, novoPlano);
    }

    /** UC03 5b e UC04a 2b - informa qual nível seria necessário. */
    private String semCobertura(Plano plano, Estabelecimento e) {
        return "O plano " + plano.getNome() + " (nível " + plano.getNivel() + ") não cobre "
             + e + ", que exige nível " + e.getNivelMinimo() + " (RF5).";
    }

    /** RN7 - suspensão por inadimplência da empresa contratante. */
    @Override
    public void suspender(Adesao ad) { ad.mudarPara(new Suspensa()); }

    @Override
    public void reativar(Adesao ad) {
        throw new RegraDeNegocioException("A adesão já está ativa.");
    }

    /** HU2 - a empresa encerra o benefício do funcionário. */
    @Override
    public void encerrar(Adesao ad) {
        ad.registrarFim(LocalDate.now());
        ad.mudarPara(new Encerrada());
    }

    @Override public boolean estaAtiva() { return true; }
    @Override public boolean estaVigente() { return true; }
    @Override public boolean podeSuspender() { return true; }
    @Override public boolean podeReativar() { return false; }
    @Override public SituacaoAdesao situacao() { return SituacaoAdesao.ATIVA; }
}
