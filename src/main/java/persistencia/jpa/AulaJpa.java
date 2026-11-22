package persistencia.jpa;

import dominio.acessoAgenda.Aula;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Spring Data gera a implementação. Chave: id (SERIAL). */
public interface AulaJpa extends JpaRepository<Aula, Integer> {

    /**
     * UC04a 4a - lê a aula já com a linha travada (SELECT ... FOR NO KEY UPDATE)
     * até o fim da transação. Duas reservas na mesma aula esperam uma pela
     * outra, e a segunda, ao continuar, lê a aula e as reservas já atualizadas.
     * Substitui o synchronized que a Aula tinha na versão em memória.
     *
     * Por que uma consulta JPQL e não @Lock sobre findById: o findById carrega
     * as associações EAGER (estabelecimento, instrutor, modalidade) no mesmo
     * SELECT, com LEFT JOIN. O PostgreSQL não aceita FOR UPDATE do lado nulo de
     * um outer join, então o Hibernate lia tudo SEM trava e só depois travava
     * cada linha em separado ("follow-on locking"). Resultado: a aula podia ser
     * lida antes da trava (cancelada/capacidade desatualizadas) e o
     * estabelecimento inteiro ficava travado. Em JPQL as associações EAGER vêm
     * em consultas separadas, então o FOR UPDATE fica só na linha da aula.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Aula a where a.id = :id")
    Optional<Aula> buscarComTrava(Integer id);
}
