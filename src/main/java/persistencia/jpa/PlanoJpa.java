package persistencia.jpa;

import dominio.planosAdesao.Plano;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data gera a implementação. Chave: id (SERIAL). */
public interface PlanoJpa extends JpaRepository<Plano, Integer> { }
