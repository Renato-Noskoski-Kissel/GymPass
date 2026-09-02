package dominio.planos;

import dominio.RegraDeNegocioException;
import dominio.cadastro.Aluno;
import dominio.cadastro.Dependente;
import dominio.cadastro.Empresa;
import dominio.cadastro.Funcionario;
import java.time.LocalDate;

/**
 * Classe de associação entre Aluno e Plano.
 *
 * Registra a concessão do benefício: a empresa contratante concede um plano
 * a um funcionário. A data de início, a data de fim e a situação não pertencem
 * nem ao aluno nem ao plano — só existem no cruzamento dos dois.
 *
 * É a Adesao que conhece o Aluno, e não o contrário, para que a dependência
 * vá de Planos e Assinatura para Cadastro e Rede.
 */
public class Adesao {

    private final Aluno aluno;
    private final Plano plano;
    private final LocalDate dataInicio;
    private LocalDate dataFim;
    private SituacaoAdesao situacao;

    private Adesao(Aluno aluno, Plano plano) {
        this.aluno = aluno;
        this.plano = plano;
        this.dataInicio = LocalDate.now();
        this.situacao = SituacaoAdesao.ATIVA;
    }

    /**
     * HU2 - a empresa inclui o funcionário no quadro de beneficiários e concede
     * o plano na mesma operação.
     *
     * A multiplicidade 1 entre Aluno e Plano é garantida aqui: não existe caminho
     * no domínio que registre um funcionário no quadro sem uma adesão a um plano.
     */
    public static Adesao cadastrarBeneficiario(Funcionario funcionario, Plano plano) {
        if (!funcionario.ehElegivel()) {
            throw new RegraDeNegocioException(
                "Funcionário não elegível para receber o benefício (RN1): " + funcionario);
        }
        Empresa empresa = funcionario.getEmpresaVinculada();
        if (!plano.ehOferecidoPor(empresa)) {
            throw new RegraDeNegocioException(
                "A empresa " + empresa + " não oferece o plano " + plano.getNome());
        }
        empresa.incluirFuncionario(funcionario);
        return new Adesao(funcionario, plano);
    }

    /**
     * HU2 - troca do nível de plano do funcionário.
     *
     * Com multiplicidade 1, a adesão anterior é substituída e não é preservada.
     * Se o histórico passar a ser necessário (cálculo de fatura por período),
     * a multiplicidade precisará mudar para 1..*.
     */
    public static Adesao trocarPlano(Adesao adesaoAtual, Plano novoPlano) {
        Aluno aluno = adesaoAtual.getAluno();
        if (!novoPlano.ehOferecidoPor(aluno.getEmpresaVinculada())) {
            throw new RegraDeNegocioException(
                "A empresa " + aluno.getEmpresaVinculada()
                + " não oferece o plano " + novoPlano.getNome());
        }
        adesaoAtual.encerrar();
        return new Adesao(aluno, novoPlano);
    }

    /**
     * RN3 - inclusão de dependente. Depende do plano (nem todo plano permite)
     * e do limite estipulado no contrato da empresa.
     *
     * O dependente recebe uma Adesao própria, sempre ao mesmo plano do
     * responsável — restrição que o diagrama de classes não expressa.
     */
    public Adesao incluirDependente(Dependente dependente) {
        if (!estaAtiva()) {
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

    public Aluno getAluno() { return aluno; }
    public Plano getPlano() { return plano; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public SituacaoAdesao getSituacao() { return situacao; }

    /** Ativa somente se a própria adesão e a elegibilidade do aluno estiverem em ordem. */
    public boolean estaAtiva() {
        return situacao == SituacaoAdesao.ATIVA && aluno.ehElegivel();
    }

    /** HU2 - a empresa encerra o benefício do funcionário. */
    public void encerrar() {
        this.situacao = SituacaoAdesao.ENCERRADA;
        this.dataFim = LocalDate.now();
    }

    /** RN7 - suspensão por inadimplência da empresa contratante. */
    public void suspender() { this.situacao = SituacaoAdesao.SUSPENSA; }

    public void reativar() { this.situacao = SituacaoAdesao.ATIVA; }

    @Override public String toString() {
        return "Adesao de " + aluno + " ao plano " + plano + " [" + situacao + "]";
    }
}
