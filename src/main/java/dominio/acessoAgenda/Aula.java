package dominio.acessoAgenda;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Instrutor;
import dominio.cadastroRede.Modalidade;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HU1 - aula da grade de um estabelecimento.
 *
 * Composição com Estabelecimento: uma aula não existe sem o estabelecimento
 * que a sedia — por isso o construtor exige um estabelecimento não nulo.
 * O caminho normal de criação é Estabelecimento.agendarAula.
 *
 * Composição com Reserva: a aula cria e guarda as próprias reservas, e é
 * quem garante que elas nunca passem da capacidade (UC04a).
 */
@Entity
@Table(name = "aulas")
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    /** Gravada em minutos pelo persistencia.ConversorDuracaoMinutos (autoApply). */
    @Column(name = "duracao_minutos", nullable = false)
    private Duration duracao;

    @Column(name = "capacidade_maxima", nullable = false)
    private int capacidadeMaxima;

    /** EAGER: Estabelecimento é abstrata; carregar já a subclasse real evita proxy. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estabelecimento_cnpj", nullable = false)
    private Estabelecimento estabelecimento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "modalidade_id", nullable = false)
    private Modalidade modalidade;

    @ManyToOne(optional = false)
    @JoinColumn(name = "instrutor_registro", nullable = false)
    private Instrutor instrutor;

    @Column(name = "cancelada", nullable = false)
    private boolean cancelada;

    /** UC04a - reservas feitas para esta aula. Lado inverso de Reserva.aula. */
    @OneToMany(mappedBy = "aula")
    private List<Reserva> reservas = new ArrayList<>();

    /** Exigido pela JPA. */
    protected Aula() { }

    public Aula(LocalDateTime dataHoraInicio, Duration duracao, int capacidadeMaxima,
                Estabelecimento estabelecimento, Modalidade modalidade, Instrutor instrutor) {

        if (estabelecimento == null) {
            throw new RegraDeNegocioException("Toda aula pertence a um estabelecimento.");
        }
        validar(dataHoraInicio, capacidadeMaxima, estabelecimento, modalidade, instrutor);

        this.dataHoraInicio = dataHoraInicio;
        this.duracao = duracao;
        this.capacidadeMaxima = capacidadeMaxima;
        this.estabelecimento = estabelecimento;
        this.modalidade = modalidade;
        this.instrutor = instrutor;
        this.cancelada = false;
    }

    private static void validar(LocalDateTime inicio, int capacidade,
                                Estabelecimento estabelecimento,
                                Modalidade modalidade, Instrutor instrutor) {
        if (capacidade <= 0) {
            throw new RegraDeNegocioException("A capacidade da aula deve ser maior que zero.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Não é possível agendar aula no passado.");
        }
        if (!estabelecimento.oferece(modalidade)) {
            throw new RegraDeNegocioException(
                estabelecimento + " não oferece a modalidade " + modalidade);
        }
        if (!instrutor.trabalhaEm(estabelecimento)) {
            throw new RegraDeNegocioException(
                instrutor + " não é instrutor de " + estabelecimento);
        }
    }

    /**
     * UC04a passo 5 - registra a reserva de um aluno nesta aula.
     *
     * Só verifica o que é da aula: se ela está aberta, se o aluno já não tem vaga
     * nela (3b) e se tem vaga sobrando. Elegibilidade,
     * cobertura do plano e limite mensal são verificados antes, por
     * Adesao.reservarVaga — use aquele método, não este diretamente.
     *
     * Extensão 4a (duas pessoas disputando a última vaga): quem garante que a
     * verificação de vaga e a inclusão da reserva aconteçam como um passo só é
     * a persistência. Repositorio.buscarAula trava a linha da aula no banco
     * (SELECT ... FOR UPDATE) até o fim da transação; uma segunda reserva na
     * mesma aula espera e, ao continuar, já enxerga a reserva gravada.
     * Um synchronized aqui não serviria: cada requisição carrega a sua
     * própria instância de Aula, então não haveria trava em comum.
     */
    public Reserva reservar(Aluno aluno) {
        if (cancelada) {
            throw new RegraDeNegocioException("A aula foi cancelada pelo estabelecimento.");
        }
        if (jaComecou()) {
            throw new RegraDeNegocioException("A aula já começou.");
        }
        Reserva existente = reservaConfirmadaDe(aluno);
        if (existente != null) {                                          // 3b
            throw new RegraDeNegocioException(
                aluno + " já tem vaga nesta aula (reservada em "
                + existente.getDataHoraReserva().format(DateTimeFormatter.ofPattern("dd/MM")) + ").");
        }
        if (vagasDisponiveis() <= 0) {
            throw new RegraDeNegocioException("A aula está lotada.");
        }
        Reserva reserva = new Reserva(aluno, this);
        reservas.add(reserva);
        return reserva;
    }

    /**
     * UC04a 3b - reserva confirmada do aluno nesta aula, ou null.
     * Uma reserva cancelada não conta: quem cancelou pode reservar de novo.
     */
    private Reserva reservaConfirmadaDe(Aluno aluno) {
        for (Reserva r : reservas) {
            if (r.getAluno() == aluno && r.getSituacao() == SituacaoReserva.CONFIRMADA) {
                return r;
            }
        }
        return null;
    }

    /** UC04a passo 3 - capacidade menos as reservas que ocupam vaga (todas, menos as canceladas). */
    public int vagasDisponiveis() {
        int ocupadas = 0;
        for (Reserva r : reservas) {
            if (r.getSituacao() != SituacaoReserva.CANCELADA) {
                ocupadas++;
            }
        }
        return capacidadeMaxima - ocupadas;
    }

    /** HU1 - troca do instrutor responsável pela aula. */
    public void atribuirInstrutor(Instrutor novoInstrutor) {
        if (!novoInstrutor.trabalhaEm(estabelecimento)) {
            throw new RegraDeNegocioException(
                novoInstrutor + " não é instrutor de " + estabelecimento);
        }
        this.instrutor = novoInstrutor;
    }

    /**
     * HU1 - ajuste do limite de vagas. Não pode ficar abaixo das vagas já
     * reservadas: as reservas confirmadas nunca excedem a capacidade (UC04a).
     * Também passa por buscarAula, então não corre junto com uma reserva.
     */
    public void alterarCapacidade(int novaCapacidade) {
        if (novaCapacidade <= 0) {
            throw new RegraDeNegocioException("A capacidade da aula deve ser maior que zero.");
        }
        int ocupadas = capacidadeMaxima - vagasDisponiveis();
        if (novaCapacidade < ocupadas) {
            throw new RegraDeNegocioException(
                "A aula já tem " + ocupadas + " vagas reservadas; a capacidade não pode ser menor.");
        }
        this.capacidadeMaxima = novaCapacidade;
    }

    /** HU1 - reagendamento. */
    public void reagendar(LocalDateTime novoInicio, Duration novaDuracao) {
        if (novoInicio.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Não é possível reagendar para o passado.");
        }
        this.dataHoraInicio = novoInicio;
        this.duracao = novaDuracao;
    }

    /** HU1 - cancelamento da aula pelo estabelecimento. */
    public void cancelar() {
        if (jaComecou()) {
            throw new RegraDeNegocioException("Não é possível cancelar uma aula já iniciada.");
        }
        this.cancelada = true;
    }

    public boolean jaComecou() { return LocalDateTime.now().isAfter(dataHoraInicio); }
    public boolean estaCancelada() { return cancelada; }

    public Integer getId() { return id; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public Duration getDuracao() { return duracao; }
    public int getCapacidadeMaxima() { return capacidadeMaxima; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public Modalidade getModalidade() { return modalidade; }
    public Instrutor getInstrutor() { return instrutor; }
    public List<Reserva> getReservas() {
        return Collections.unmodifiableList(reservas);
    }

    @Override public String toString() {
        return modalidade + " em " + estabelecimento + " - " + dataHoraInicio;
    }
}
