package dominio.acessoAgenda;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Instrutor;
import dominio.cadastroRede.Modalidade;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * HU1 - aula da grade de um estabelecimento.
 *
 * Composição com Estabelecimento: uma aula não existe sem o estabelecimento
 * que a sedia — por isso o construtor exige um estabelecimento não nulo.
 *
 * A reserva de vagas (classe Reserva) entra na iteração 2, com a HU3.
 */
public class Aula {

    private LocalDateTime dataHoraInicio;
    private Duration duracao;
    private int capacidadeMaxima;

    private final Estabelecimento estabelecimento;
    private Modalidade modalidade;
    private Instrutor instrutor;

    private boolean cancelada;

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

    /** HU1 - troca do instrutor responsável pela aula. */
    public void atribuirInstrutor(Instrutor novoInstrutor) {
        if (!novoInstrutor.trabalhaEm(estabelecimento)) {
            throw new RegraDeNegocioException(
                novoInstrutor + " não é instrutor de " + estabelecimento);
        }
        this.instrutor = novoInstrutor;
    }

    /** HU1 - ajuste do limite de vagas. */
    public void alterarCapacidade(int novaCapacidade) {
        if (novaCapacidade <= 0) {
            throw new RegraDeNegocioException("A capacidade da aula deve ser maior que zero.");
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

    @Override public String toString() {
        return modalidade + " em " + estabelecimento + " - " + dataHoraInicio;
    }
}
