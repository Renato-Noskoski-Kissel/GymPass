package persistencia.jpa;

import dominio.cadastroRede.Aluno;
import dominio.planosAdesao.Adesao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data gera a implementação. Chave: id (SERIAL). */
public interface AdesaoJpa extends JpaRepository<Adesao, Integer> {

    /** Todas as adesões do aluno, inclusive as encerradas. Quem filtra a vigente é o Java. */
    List<Adesao> findByAluno(Aluno aluno);
}
