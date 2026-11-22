package dominio.planosAdesao;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Dependente;
import dominio.cadastroRede.Empresa;
import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Funcionario;
import jakarta.persistence.*;
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
 *
 * Iteração 2: a Adesao valida e cria o check-in e a reserva, porque é a única
 * classe que enxerga ao mesmo tempo o aluno (elegibilidade) e o plano
 * (cobertura e limite mensal) — Especialista da Informação.
 *
 * Padrão State: o que cada operação faz depende da situação da adesão
 * (Ativa, Suspensa, Encerrada). As validações moram nos estados; esta
 * classe só delega ao estado corrente.
 */
@Entity
@Table(name = "adesoes")
public class Adesao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** EAGER: Aluno é abstrata; Ativa faz instanceof Funcionario. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "aluno_cpf", nullable = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private Plano plano;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    /** Nulo enquanto a adesão não for encerrada. */
    @Column(name = "data_fim")
    private LocalDate dataFim;

    /**
     * Estado do padrão State. Gravado como texto na coluna situacao pelo
     * persistencia.ConversorEstadoAdesao (autoApply), que reconstrói
     * Ativa/Suspensa/Encerrada ao ler.
     */
    @Column(name = "situacao", nullable = false, length = 20)
    private EstadoAdesao estado;

    /** Exigido pela JPA. */
    protected Adesao() { }

    /** Visível no pacote para que Ativa crie a adesão do dependente (RN3). */
    Adesao(Aluno aluno, Plano plano) {
        this.aluno = aluno;
        this.plano = plano;
        this.dataInicio = LocalDate.now();
        this.estado = new Ativa();
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
     * HU2 - troca do nível de plano do funcionário. Só uma adesão ativa pode
     * trocar; quem decide é o estado (ver Ativa, Suspensa e Encerrada).
     */
    public static Adesao trocarPlano(Adesao adesaoAtual, Plano novoPlano) {
        return adesaoAtual.estado.trocarPlano(adesaoAtual, novoPlano);
    }

    /** RN3 - ver Ativa.incluirDependente. */
    public Adesao incluirDependente(Dependente dependente) {
        return estado.incluirDependente(this, dependente);
    }

    /** UC03 - ver Ativa.registrarCheckIn. */
    public CheckIn registrarCheckIn(Estabelecimento estabelecimento,
                                    double latitude, double longitude, CheckIn ultimo) {
        return estado.registrarCheckIn(this, estabelecimento, latitude, longitude, ultimo);
    }

    /** UC04a - ver Ativa.reservarVaga. */
    public Reserva reservarVaga(Aula aula, int reservasNoMes) {
        return estado.reservarVaga(this, aula, reservasNoMes);
    }

    /** RN7 - suspensão por inadimplência da empresa contratante. */
    public void suspender() { estado.suspender(this); }

    public void reativar() { estado.reativar(this); }

    /** HU2 - a empresa encerra o benefício do funcionário. */
    public void encerrar() { estado.encerrar(this); }

    /** Transição de estado; só os próprios estados a disparam. */
    void mudarPara(EstadoAdesao novoEstado) { this.estado = novoEstado; }

    void registrarFim(LocalDate data) { this.dataFim = data; }

    public Integer getId() { return id; }
    public Aluno getAluno() { return aluno; }
    public Plano getPlano() { return plano; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public SituacaoAdesao getSituacao() { return estado.situacao(); }

    /** Ativa somente se a própria adesão e a elegibilidade do aluno estiverem em ordem (RN1). */
    public boolean estaAtiva() { return estado.estaAtiva() && aluno.ehElegivel(); }

    /** Adesão corrente do aluno (ativa ou suspensa), isto é, ainda não encerrada. */
    public boolean estaVigente() { return estado.estaVigente(); }

    public boolean podeSuspender() { return estado.podeSuspender(); }

    public boolean podeReativar() { return estado.podeReativar(); }

    @Override public String toString() {
        return "Adesao de " + aluno + " ao plano " + plano + " [" + estado.situacao() + "]";
    }
}
