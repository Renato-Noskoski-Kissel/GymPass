package dominio.cadastroRede;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;

/**
 * Classe abstrata: todo aluno é um Funcionario de empresa contratante
 * ou um Dependente de um funcionário (RN1).
 *
 * Aluno não conhece Plano nem Adesao: a dependência vai no sentido
 * Planos e Assinatura -> Cadastro e Rede, nunca o contrário.
 *
 * Persistência: herança em tabelas separadas. "alunos" tem o que é comum;
 * "funcionarios" e "dependentes" têm o cpf como PK e FK para alunos.
 * A coluna "tipo" registra a subclasse.
 */
@Entity
@Table(name = "alunos")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Aluno {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDate dataCadastro;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private SituacaoAluno situacao;

    /** Exigido pela JPA. */
    protected Aluno() { }

    protected Aluno(String nome, String cpf, String email) {
        this.nome = nome;
        this.cpf = Documentos.cpf(cpf);
        this.email = email;
        this.dataCadastro = LocalDate.now();
        this.situacao = SituacaoAluno.ATIVO;
    }

    /** UC07 extensão 2a - este aluno tem este CPF? Ignora a pontuação. */
    public boolean temCpf(String outroCpf) {
        return cpf.equals(Documentos.apenasDigitos(outroCpf));
    }

    public String getNome() { return nome; }
    /** Só os 11 dígitos; a formatação com pontuação é feita na tela. */
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public LocalDate getDataCadastro() { return dataCadastro; }
    public SituacaoAluno getSituacao() { return situacao; }

    public boolean estaAtivo() { return situacao == SituacaoAluno.ATIVO; }

    /** RN7 - bloqueio por inadimplência da empresa contratante. */
    public void bloquear() { this.situacao = SituacaoAluno.BLOQUEADO; }

    public void reativar() { this.situacao = SituacaoAluno.ATIVO; }

    public void inativar() { this.situacao = SituacaoAluno.INATIVO; }

    /** RN1 - cada subclasse define seu próprio critério de elegibilidade. */
    public abstract boolean ehElegivel();

    /** Empresa à qual o aluno está vinculado, direta ou indiretamente. */
    public abstract Empresa getEmpresaVinculada();

    @Override public String toString() { return nome; }
}