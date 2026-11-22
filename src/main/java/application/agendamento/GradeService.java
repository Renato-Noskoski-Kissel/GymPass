package application.agendamento;

import dominio.acessoAgenda.Aula;
import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Instrutor;
import dominio.cadastroRede.Modalidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistencia.Repositorio;

import java.time.Duration;
import java.time.LocalDateTime;

import static application.NaoEncontradoException.exigir;

/** HU1 - camada de aplicação para a grade de aulas. */
@Service
public class GradeService {

    private final Repositorio repositorio;

    public GradeService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void agendar(String cnpj, int modalidadeId, String registroInstrutor,
                        String inicio, long duracaoMin, int capacidade) {
        Estabelecimento est = exigir(repositorio.buscarEstabelecimento(cnpj),
                "Estabelecimento não encontrado: " + cnpj);
        Aula aula = est.agendarAula(
                LocalDateTime.parse(inicio),
                Duration.ofMinutes(duracaoMin),
                capacidade,
                modalidade(est, modalidadeId),
                instrutor(est, registroInstrutor));
        repositorio.adicionarAula(aula);
    }

    @Transactional
    public void trocarInstrutor(int aulaId, String registroInstrutor) {
        Aula aula = aula(aulaId);
        aula.atribuirInstrutor(instrutor(aula.getEstabelecimento(), registroInstrutor));
    }

    @Transactional
    public void alterarCapacidade(int aulaId, int capacidade) {
        aula(aulaId).alterarCapacidade(capacidade);
    }

    @Transactional
    public void cancelar(int aulaId) {
        aula(aulaId).cancelar();
    }

    private Aula aula(int id) {
        return exigir(repositorio.buscarAula(id), "Aula não encontrada: " + id);
    }

    private static Modalidade modalidade(Estabelecimento est, int id) {
        return exigir(est.getModalidades().stream()
                .filter(m -> m.getId() == id).findFirst().orElse(null),
                est + " não tem a modalidade " + id);
    }

    private static Instrutor instrutor(Estabelecimento est, String registro) {
        return exigir(est.getInstrutores().stream()
                .filter(i -> i.getRegistro().equals(registro)).findFirst().orElse(null),
                est + " não tem o instrutor " + registro);
    }
}
