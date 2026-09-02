package dominio.cadastro;

public class Dependente extends Aluno {

    private final String grauParentesco;
    private final Funcionario responsavel;

    public Dependente(String nome, String cpf, String email,
                      String grauParentesco, Funcionario responsavel) {
        super(nome, cpf, email);
        this.grauParentesco = grauParentesco;
        this.responsavel = responsavel;
    }

    public String getGrauParentesco() { return grauParentesco; }
    public Funcionario getResponsavel() { return responsavel; }

    @Override public Empresa getEmpresaVinculada() {
        return responsavel.getEmpresaVinculada();
    }

    /** RN1 - só é elegível enquanto o responsável também for. */
    @Override public boolean ehElegivel() {
        return responsavel != null && responsavel.ehElegivel() && estaAtivo();
    }
}
