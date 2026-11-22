package persistencia.jpa;

import dominio.cadastroRede.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data gera a implementação. Chave: cnpj (só dígitos). */
public interface EmpresaJpa extends JpaRepository<Empresa, String> { }
