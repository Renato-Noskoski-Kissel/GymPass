package dominio.cadastro;

import dominio.RegraDeNegocioException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Classe abstrata: todo estabelecimento é uma Academia, um Estudio ou uma Quadra. */
public abstract class Estabelecimento {

    private final String nomeFantasia;
    private final String cnpj;
    private SituacaoCredenciamento situacaoCredenciamento;
    private final double valorPorCheckIn;

    /** Agregação compartilhada: a modalidade existe independentemente deste estabelecimento. */
    private final List<Modalidade> modalidades = new ArrayList<>();

    /** HU5 - instrutores contratados pelo estabelecimento. */
    private final List<Instrutor> instrutores = new ArrayList<>();

    protected Estabelecimento(String nomeFantasia, String cnpj, double valorPorCheckIn) {
        this.nomeFantasia = nomeFantasia;
        this.cnpj = cnpj;
        this.valorPorCheckIn = valorPorCheckIn;
        this.situacaoCredenciamento = SituacaoCredenciamento.PENDENTE;
    }

    public String getNomeFantasia() { return nomeFantasia; }
    public String getCnpj() { return cnpj; }
    public double getValorPorCheckIn() { return valorPorCheckIn; }
    public SituacaoCredenciamento getSituacaoCredenciamento() { return situacaoCredenciamento; }

    /** HU4 - homologação pelo administrador. */
    public void aprovarCredenciamento() {
        this.situacaoCredenciamento = SituacaoCredenciamento.APROVADO;
    }

    public void suspenderCredenciamento() {
        this.situacaoCredenciamento = SituacaoCredenciamento.SUSPENSO;
    }

    public boolean estaCredenciado() {
        return situacaoCredenciamento == SituacaoCredenciamento.APROVADO;
    }

    /** HU5 - cadastro de modalidade. */
    public void adicionarModalidade(Modalidade modalidade) {
        if (modalidades.contains(modalidade)) {
            throw new RegraDeNegocioException(
                "Modalidade já cadastrada em " + nomeFantasia + ": " + modalidade);
        }
        modalidades.add(modalidade);
    }

    public void removerModalidade(Modalidade modalidade) {
        modalidades.remove(modalidade);
    }

    public boolean oferece(Modalidade modalidade) {
        return modalidades.contains(modalidade);
    }

    /** HU5 - cadastro de instrutor. */
    public Instrutor contratarInstrutor(String nome, String registro, String especialidade) {
        Instrutor instrutor = new Instrutor(nome, registro, especialidade, this);
        instrutores.add(instrutor);
        return instrutor;
    }

    public void desligarInstrutor(Instrutor instrutor) {
        instrutores.remove(instrutor);
    }

    public List<Modalidade> getModalidades() { return Collections.unmodifiableList(modalidades); }
    public List<Instrutor> getInstrutores() { return Collections.unmodifiableList(instrutores); }

    @Override public String toString() { return nomeFantasia; }
}
