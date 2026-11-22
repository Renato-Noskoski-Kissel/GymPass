package dominio.cadastroRede;

import jakarta.persistence.*;

@Entity
@Table(name = "dependentes")
@PrimaryKeyJoinColumn(name = "cpf")
@DiscriminatorValue("DEPENDENTE")
public class Dependente extends Aluno {

    @Column(name = "grau_parentesco", nullable = false, length = 30)
    private String grauParentesco;

    @ManyToOne(optional = false)
    @JoinColumn(name = "responsavel_cpf", nullable = false)
    private Funcionario responsavel;

    /** Exigido pela JPA. */
    protected Dependente() { }

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
