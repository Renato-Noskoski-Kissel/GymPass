package persistencia.jpa;

import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

/** Spring Data gera a implementação. Chave: id (SERIAL). */
public interface ReservaJpa extends JpaRepository<Reserva, Integer> {

    /** Reservas do aluno que contam no limite, em aulas que começam em [inicio, fim). */
    @Query("""
           select count(r) from Reserva r
            where r.aluno = :aluno
              and (r.situacao <> dominio.acessoAgenda.SituacaoReserva.CANCELADA or r.penalizada = true)
              and r.aula.dataHoraInicio >= :inicio
              and r.aula.dataHoraInicio < :fim
           """)
    long contarNoLimite(Aluno aluno, LocalDateTime inicio, LocalDateTime fim);
}
