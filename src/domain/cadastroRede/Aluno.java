package dominio.cadastro;

import java.time.LocalDate;

/**
 * Classe abstrata: todo aluno é um Funcionario de empresa contratante
 * ou um Dependente de um funcionário (RN1).
 *
 * Aluno não conhece Plano nem Adesao: a dependência vai no sentido
 * Planos e Assinatura -> Cadastro e Rede, nunca o contrário.
 */
public abstract class Aluno {

    private final String nome;
    private final String cpf;
    private final String email;
    private final LocalDate dataCadastro;
    private SituacaoAluno situacao;

    protected Aluno(String nome, String cpf, String email) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.dataCadastro = LocalDate.now();
        this.situacao = SituacaoAluno.ATIVO;
    }

    public String getNome() { return nome; }
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
