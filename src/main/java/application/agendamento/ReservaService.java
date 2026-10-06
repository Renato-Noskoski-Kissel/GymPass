package application.agendamento;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Aluno;
import dominio.planosAdesao.Adesao;
import org.springframework.stereotype.Service;
import persistencia.Repositorio;

import java.time.YearMonth;

/**
 * UC04a - camada de aplicação para a reserva de vaga em aula.
 *
 * Busca os objetos envolvidos e quantas reservas o aluno já fez no mês,
 * entrega à Adesao (que valida e pede a reserva à aula) e guarda o resultado.
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

    private Adesao adesaoVigente(Aluno aluno) {
        Adesao adesao = repositorio.adesaoDe(aluno);
        if (adesao == null) {
            throw new RegraDeNegocioException(aluno + " não tem adesão vigente.");
        }
        return adesao;
    }
}
