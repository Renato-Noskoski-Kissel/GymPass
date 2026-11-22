package persistencia.jpa;

import dominio.cadastroRede.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data gera a implementação. Chave: cnpj (só dígitos). */
public interface EstabelecimentoJpa extends JpaRepository<Estabelecimento, String> { }
