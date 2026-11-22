package application.agendamento;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Aluno;
import dominio.planosAdesao.Adesao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistencia.Repositorio;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static application.NaoEncontradoException.exigir;

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

    /**
     * buscarAula trava a linha da aula até o fim desta transação (UC04a 4a):
     * uma segunda reserva na mesma aula espera esta terminar e só então
     * conta as vagas, já vendo a reserva gravada aqui.
     */
    @Transactional
    public Reserva reservarVaga(String cpf, int aulaId) {
        Aluno aluno = aluno(cpf);
        Aula aula = exigir(repositorio.buscarAula(aulaId), "Aula não encontrada: " + aulaId);
        Adesao adesao = adesaoVigente(aluno);
        YearMonth mesDaAula = YearMonth.from(aula.getDataHoraInicio());
        int reservasNoMes = repositorio.totalReservasNoMes(aluno, mesDaAula);

        Reserva reserva = adesao.reservarVaga(aula, reservasNoMes);
        repositorio.adicionarReserva(reserva);
        return reserva;
    }

    /**
     * UC04b - cancela uma reserva do aluno.
     *
     * @param aceitaPenalidade o aluno já confirmou que aceita o cancelamento
     *        penalizado (extensão 4a.1). Se o prazo passou e ele não confirmou,
     *        nada é cancelado e a tela pede a confirmação.
     */
    @Transactional
    public void cancelarReserva(String cpf, int reservaId, boolean aceitaPenalidade) {
        Aluno aluno = aluno(cpf);
        Reserva reserva = exigir(repositorio.buscarReserva(reservaId),
                "Reserva não encontrada: " + reservaId);
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

    private Aluno aluno(String cpf) {
        return exigir(repositorio.buscarAluno(cpf), "Aluno não encontrado: " + cpf);
    }

    private Adesao adesaoVigente(Aluno aluno) {
        Adesao adesao = repositorio.adesaoDe(aluno);
        if (adesao == null) {
            throw new RegraDeNegocioException(aluno + " não tem adesão vigente.");
        }
        return adesao;
    }
}
