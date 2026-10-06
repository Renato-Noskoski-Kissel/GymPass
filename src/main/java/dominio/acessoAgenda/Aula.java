package dominio.acessoAgenda;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Instrutor;
import dominio.cadastroRede.Modalidade;
import java.time.Duration;
import java.time.LocalDateTime;
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
public class Aula {

    private LocalDateTime dataHoraInicio;
    private Duration duracao;
    private int capacidadeMaxima;

    private final Estabelecimento estabelecimento;
    private Modalidade modalidade;
    private Instrutor instrutor;

    private boolean cancelada;

    /** UC04a - reservas feitas para esta aula. */
    private final List<Reserva> reservas = new ArrayList<>();

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
     * Só verifica o que é da aula: se ela está aberta e se tem vaga. Elegibilidade,
     * cobertura do plano e limite mensal são verificados antes, por
     * Adesao.reservarVaga — use aquele método, não este diretamente.
     *
     * synchronized: a verificação de vaga e a inclusão da reserva acontecem como
     * um passo só, então duas requisições simultâneas não disputam a última vaga
     * (extensão 4a).
     */
    public synchronized Reserva reservar(Aluno aluno) {
        if (cancelada) {
            throw new RegraDeNegocioException("A aula foi cancelada pelo estabelecimento.");
        }
        if (jaComecou()) {
            throw new RegraDeNegocioException("A aula já começou.");
        }
        if (vagasDisponiveis() <= 0) {
            throw new RegraDeNegocioException("A aula está lotada.");
        }
        Reserva reserva = new Reserva(aluno, this);
        reservas.add(reserva);
        return reserva;
    }

    /** UC04a passo 3 - capacidade menos as reservas que ocupam vaga (todas, menos as canceladas). */
    public synchronized int vagasDisponiveis() {
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
     */
    public synchronized void alterarCapacidade(int novaCapacidade) {
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

    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public Duration getDuracao() { return duracao; }
    public int getCapacidadeMaxima() { return capacidadeMaxima; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public Modalidade getModalidade() { return modalidade; }
    public Instrutor getInstrutor() { return instrutor; }
    public synchronized List<Reserva> getReservas() {
        return Collections.unmodifiableList(new ArrayList<>(reservas));
    }

    @Override public String toString() {
        return modalidade + " em " + estabelecimento + " - " + dataHoraInicio;
    }
}
