package dominio.acessoAgenda;

import dominio.cadastroRede.Aluno;
import java.time.LocalDateTime;

/**
 * UC04a - vaga garantida por um aluno em uma aula.
 *
 * Classe normal, não de associação: o mesmo par aluno-aula pode se repetir.
 *
 * Composição com Aula: o construtor é de pacote, e quem cria a reserva é
 * Aula.reservar (Criador — a aula agrega as reservas e conhece a capacidade).
 */
public class Reserva {

    private final LocalDateTime dataHoraReserva;
    private SituacaoReserva situacao;
    private final Aluno aluno;
    private final Aula aula;

    Reserva(Aluno aluno, Aula aula) {
        this.aluno = aluno;
        this.aula = aula;
        this.dataHoraReserva = LocalDateTime.now();
        this.situacao = SituacaoReserva.CONFIRMADA;
    }

    public LocalDateTime getDataHoraReserva() { return dataHoraReserva; }
    public SituacaoReserva getSituacao() { return situacao; }
    public Aluno getAluno() { return aluno; }
    public Aula getAula() { return aula; }

    @Override public String toString() {
        return "Reserva de " + aluno + " em " + aula + " [" + situacao + "]";
    }
}
