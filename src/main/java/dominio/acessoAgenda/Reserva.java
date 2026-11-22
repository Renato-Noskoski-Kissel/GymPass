package dominio.acessoAgenda;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Aluno;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * UC04a - vaga garantida por um aluno em uma aula.
 *
 * Classe normal, não de associação: o mesmo par aluno-aula pode se repetir.
 *
 * Composição com Aula: o construtor é de pacote, e quem cria a reserva é
 * Aula.reservar (Criador — a aula agrega as reservas e conhece a capacidade).
 *
 * UC04b - a própria reserva decide se pode ser cancelada e se o cancelamento
 * é penalizado (Especialista: ela conhece a aula e o próprio estado).
 */
@Entity
@Table(name = "reservas")
public class Reserva {

    /** RN5 - cancelamento com menos que isto de antecedência é penalizado. */
    public static final Duration ANTECEDENCIA_SEM_PENALIDADE = Duration.ofHours(2);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "data_hora_reserva", nullable = false)
    private LocalDateTime dataHoraReserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private SituacaoReserva situacao;

    @Column(name = "penalizada", nullable = false)
    private boolean penalizada;

    /** EAGER: Aluno é abstrata; carregar já a subclasse real evita proxy. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "aluno_cpf", nullable = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;

    /** Exigido pela JPA. */
    protected Reserva() { }

    Reserva(Aluno aluno, Aula aula) {
        this.aluno = aluno;
        this.aula = aula;
        this.dataHoraReserva = LocalDateTime.now();
        this.situacao = SituacaoReserva.CONFIRMADA;
        this.penalizada = false;
    }

    /** UC04b passo 2d - até quando dá para cancelar sem penalidade (RN5). */
    public LocalDateTime prazoSemPenalidade() {
        return aula.getDataHoraInicio().minus(ANTECEDENCIA_SEM_PENALIDADE);
    }

    /**
     * UC04b passos 2f e 4 - cancelar neste momento geraria penalidade?
     * Só se a reserva está confirmada, a aula não foi cancelada pelo
     * estabelecimento (4c), ainda não começou (4b) e o prazo já passou (4a).
     */
    public boolean cancelamentoSeriaPenalizado(LocalDateTime momento) {
        return situacao == SituacaoReserva.CONFIRMADA
            && !aula.estaCancelada()
            && momento.isAfter(prazoSemPenalidade())
            && momento.isBefore(aula.getDataHoraInicio());
    }

    /**
     * UC04b - cancela a reserva e libera a vaga.
     *
     * A vaga volta sozinha: Aula.vagasDisponiveis não conta reservas canceladas.
     * O momento vem de fora para que a regra possa ser testada em qualquer horário.
     *
     * A confirmação adicional do cancelamento penalizado (4a.1) é um passo da
     * interação, feito antes de chamar este método.
     *
     * Não trava a aula: cancelar só libera vaga. Se correr junto com uma
     * reserva, o pior caso é a reserva ainda contar esta vaga como ocupada
     * e responder "lotada" — nunca passar da capacidade.
     */
    public void cancelar(LocalDateTime momento) {
        if (situacao != SituacaoReserva.CONFIRMADA) {
            throw new RegraDeNegocioException("Esta reserva já foi cancelada.");
        }
        if (aula.estaCancelada()) {                                   // 4c
            this.situacao = SituacaoReserva.CANCELADA;
            return;
        }
        if (!momento.isBefore(aula.getDataHoraInicio())) {            // 4b
            throw new RegraDeNegocioException(
                "A aula já começou e a reserva não pode mais ser cancelada.");
        }
        this.penalizada = cancelamentoSeriaPenalizado(momento);       // 4a
        this.situacao = SituacaoReserva.CANCELADA;
    }

    public Integer getId() { return id; }
    public LocalDateTime getDataHoraReserva() { return dataHoraReserva; }
    public SituacaoReserva getSituacao() { return situacao; }
    public boolean isPenalizada() { return penalizada; }
    public Aluno getAluno() { return aluno; }
    public Aula getAula() { return aula; }

    @Override public String toString() {
        return "Reserva de " + aluno + " em " + aula + " [" + situacao
             + (penalizada ? ", penalizada" : "") + "]";
    }
}
