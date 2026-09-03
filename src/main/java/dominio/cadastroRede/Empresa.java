package dominio.cadastroRede;

import dominio.RegraDeNegocioException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Empresa {

    private final String razaoSocial;
    private final String cnpj;
    private final LocalDate dataContrato;
    private final int limiteDependentes;
    private final int diaVencimentoFatura;

    /** HU2 / RF10 - quadro de beneficiários. */
    private final List<Funcionario> funcionarios = new ArrayList<>();

    public Empresa(String razaoSocial, String cnpj, LocalDate dataContrato,
                   int limiteDependentes, int diaVencimentoFatura) {
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.dataContrato = dataContrato;
        this.limiteDependentes = limiteDependentes;
        this.diaVencimentoFatura = diaVencimentoFatura;
    }

    public String getRazaoSocial() { return razaoSocial; }
    public String getCnpj() { return cnpj; }
    public LocalDate getDataContrato() { return dataContrato; }
    public int getLimiteDependentes() { return limiteDependentes; }
    public int getDiaVencimentoFatura() { return diaVencimentoFatura; }

    /**
     * HU2 - inclui um funcionário no quadro de beneficiários.
     *
     * Não deve ser chamada diretamente: use Adesao.cadastrarBeneficiario, que
     * inclui o funcionário e concede o plano na mesma operação. Assim nunca
     * existe funcionário no quadro sem plano (multiplicidade 1).
     */
    public void incluirFuncionario(Funcionario funcionario) {
        if (funcionario.getEmpresaVinculada() != this) {
            throw new RegraDeNegocioException(funcionario + " não está vinculado a " + razaoSocial);
        }
        if (funcionarios.contains(funcionario)) {
            throw new RegraDeNegocioException("Funcionário já consta no quadro: " + funcionario);
        }
        funcionarios.add(funcionario);
    }

    /** HU2 - remove do quadro; o funcionário e seus dependentes perdem a elegibilidade (RN1). */
    public void removerFuncionario(Funcionario funcionario) {
        if (!funcionarios.remove(funcionario)) {
            throw new RegraDeNegocioException("Funcionário não consta no quadro: " + funcionario);
        }
        funcionario.desvincular();
    }

    public boolean possuiFuncionario(Funcionario funcionario) {
        return funcionarios.contains(funcionario);
    }

    /** HU2 - total de funcionários no quadro. */
    public int getTotalFuncionarios() { return funcionarios.size(); }

    /** HU2 - total de dependentes de todos os funcionários. */
    public int getTotalDependentes() {
        int total = 0;
        for (Funcionario f : funcionarios) {
            total += f.getDependentes().size();
        }
        return total;
    }

    /** HU2 - total de pessoas com direito ao benefício. */
    public int getTotalBeneficiarios() {
        return getTotalFuncionarios() + getTotalDependentes();
    }

    public List<Funcionario> getFuncionarios() {
        return Collections.unmodifiableList(funcionarios);
    }

    @Override public String toString() { return razaoSocial; }
}
