package persistencia.jpa;

import dominio.acessoAgenda.CheckIn;
import dominio.cadastroRede.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data gera a implementação. Chave: id (SERIAL). */
public interface CheckInJpa extends JpaRepository<CheckIn, Integer> {

    /** Usa o índice idx_check_ins_aluno_data. O id desempata dois check-ins no mesmo instante. */
    Optional<CheckIn> findFirstByAlunoOrderByDataHoraDescIdDesc(Aluno aluno);

    /** O código pode repetir (é sorteado sem checar colisão): fica o mais recente. */
    Optional<CheckIn> findFirstByCodigoOrderByDataHoraDescIdDesc(String codigo);
}
