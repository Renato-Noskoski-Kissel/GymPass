package application.grade;

import dominio.acessoAgenda.Aula;
import dominio.cadastroRede.Estabelecimento;
import org.springframework.stereotype.Service;
import persistencia.Repositorio;

import java.time.Duration;
import java.time.LocalDateTime;

/** HU1 - camada de aplicação para a grade de aulas. */
@Service
public class GradeService {

    private final Repositorio repositorio;

    public GradeService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public void agendar(int estabelecimentoId, int modalidadeId, int instrutorId,
                        String inicio, long duracaoMin, int capacidade) {
        Estabelecimento est = repositorio.getEstabelecimentos().get(estabelecimentoId);
        repositorio.getAulas().add(new Aula(
                LocalDateTime.parse(inicio),
                Duration.ofMinutes(duracaoMin),
                capacidade,
                est,
                est.getModalidades().get(modalidadeId),
                est.getInstrutores().get(instrutorId)));
    }

    public void trocarInstrutor(int aulaId, int instrutorId) {
        Aula aula = repositorio.getAulas().get(aulaId);
        aula.atribuirInstrutor(aula.getEstabelecimento().getInstrutores().get(instrutorId));
    }

    public void alterarCapacidade(int aulaId, int capacidade) {
        repositorio.getAulas().get(aulaId).alterarCapacidade(capacidade);
    }

    public void cancelar(int aulaId) {
        repositorio.getAulas().get(aulaId).cancelar();
    }
}
