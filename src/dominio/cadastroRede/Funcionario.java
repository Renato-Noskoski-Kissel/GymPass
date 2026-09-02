package dominio.cadastroRede;

import dominio.RegraDeNegocioException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Funcionario extends Aluno {

    private final String matricula;
    private final LocalDate dataAdmissao;
    private Empresa empresa;

    private final List<Dependente> dependentes = new ArrayList<>();

    public Funcionario(String nome, String cpf, String email,
                       String matricula, LocalDate dataAdmissao, Empresa empresa) {
        super(nome, cpf, email);
        this.matricula = matricula;
        this.dataAdmissao = dataAdmissao;
        this.empresa = empresa;
    }

    public String getMatricula() { return matricula; }
    public LocalDate getDataAdmissao() { return dataAdmissao; }

    @Override public Empresa getEmpresaVinculada() { return empresa; }

    /** RN1 - elegível enquanto estiver ativo e vinculado a uma empresa. */
    @Override public boolean ehElegivel() {
        return empresa != null && estaAtivo();
    }

    /** RF10 - chamado por Empresa.removerFuncionario; desliga o funcionário e seus dependentes. */
    void desvincular() {
        this.empresa = null;
        inativar();
        for (Dependente d : dependentes) {
            d.inativar();
        }
    }

    /**
     * A validação da RN3 (plano permite dependentes e limite do contrato)
     * é feita por Adesao.incluirDependente, que conhece o plano.
     */
    public void vincularDependente(Dependente dependente) {
        if (dependente.getResponsavel() != this) {
            throw new RegraDeNegocioException(dependente + " não é dependente de " + getNome());
        }
        if (dependentes.contains(dependente)) {
            throw new RegraDeNegocioException("Dependente já vinculado: " + dependente);
        }
        dependentes.add(dependente);
    }

    public void removerDependente(Dependente dependente) {
        if (dependentes.remove(dependente)) {
            dependente.inativar();
        }
    }

    public List<Dependente> getDependentes() {
        return Collections.unmodifiableList(dependentes);
    }
}
