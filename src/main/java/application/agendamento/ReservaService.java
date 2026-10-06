package application.agendamento;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Aluno;
import dominio.planosAdesao.Adesao;
import org.springframework.stereotype.Service;
import persistencia.Repositorio;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * UC04a e UC04b - camada de aplicação para reservar e cancelar vaga em aula.
 *
 * Busca os objetos envolvidos e entrega ao domínio. As regras de reserva
 * ficam em Adesao e Aula; as de cancelamento, em Reserva.
 */
@Service
public class ReservaService {

    private final Repositorio repositorio;

    public ReservaService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Reserva reservarVaga(int alunoId, int aulaId) {
        Aluno aluno = repositorio.getAlunos().get(alunoId);
        Aula aula = repositorio.getAulas().get(aulaId);
        Adesao adesao = adesaoVigente(aluno);
        int reservasNoMes = repositorio.totalReservasNoMes(aluno, YearMonth.now());

        Reserva reserva = adesao.reservarVaga(aula, reservasNoMes);
        repositorio.getReservas().add(reserva);
        return reserva;
    }

    /**
     * UC04b - cancela uma reserva do aluno.
     *
     * @param aceitaPenalidade o aluno já confirmou que aceita o cancelamento
     *        penalizado (extensão 4a.1). Se o prazo passou e ele não confirmou,
     *        nada é cancelado e a tela pede a confirmação.
     */
    public void cancelarReserva(int alunoId, int reservaId, boolean aceitaPenalidade) {
        Aluno aluno = repositorio.getAlunos().get(alunoId);
        Reserva reserva = repositorio.getReservas().get(reservaId);
        if (reserva.getAluno() != aluno) {
            throw new RegraDeNegocioException("Esta reserva não pertence a " + aluno + ".");
        }

        LocalDateTime agora = LocalDateTime.now();
        if (reserva.cancelamentoSeriaPenalizado(agora) && !aceitaPenalidade) {
            throw new RegraDeNegocioException(
                "O prazo para cancelar sem penalidade terminou às "
                + reserva.prazoSemPenalidade().format(DateTimeFormatter.ofPattern("HH:mm"))
                + ". Confirme o cancelamento com penalidade.");
        }
        reserva.cancelar(agora);
    }

    private Adesao adesaoVigente(Aluno aluno) {
        Adesao adesao = repositorio.adesaoDe(aluno);
        if (adesao == null) {
            throw new RegraDeNegocioException(aluno + " não tem adesão vigente.");
        }
        return adesao;
    }
}
