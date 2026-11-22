package persistencia.jpa;

import dominio.cadastroRede.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data gera a implementação. Chave: cpf (só dígitos). Devolve Funcionario ou Dependente. */
public interface AlunoJpa extends JpaRepository<Aluno, String> { }
