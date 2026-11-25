package dominio.planosAdesao;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Dependente;

import java.util.List;

/**
 * UC01 5a (RN3) - o novo plano não permite dependentes e o funcionário ainda
 * tem dependentes ativos.
 *
 * O domínio só recusa: quem decide se a troca segue mesmo assim é o aluno.
 * Uma classe própria deixa a tela distinguir este caso de uma recusa comum e
 * pedir a confirmação, mostrando quem perde o acesso.
 */
public class DependentesPerdemAcessoException extends RegraDeNegocioException {

    private final List<String> dependentes;

    public DependentesPerdemAcessoException(Plano novoPlano, List<Dependente> afetados) {
        super("O plano " + novoPlano.getNome() + " não permite dependentes (RN3). "
            + "Com a troca, perdem o acesso: "
            + String.join(", ", afetados.stream().map(Dependente::getNome).toList()) + ".");
        this.dependentes = afetados.stream().map(Dependente::getNome).toList();
    }

    /** Nomes dos dependentes que perderiam o acesso. */
    public List<String> getDependentes() { return dependentes; }
}
