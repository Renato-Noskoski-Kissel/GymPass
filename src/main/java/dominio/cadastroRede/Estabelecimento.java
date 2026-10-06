package dominio.cadastroRede;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.Aula;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Classe abstrata: todo estabelecimento é uma Academia, um Estudio ou uma Quadra. */
public abstract class Estabelecimento {

    /** RN4 - distância máxima entre o aluno e o estabelecimento para aceitar o check-in. */
    public static final double RAIO_MAXIMO_METROS = 200;

    private final String nomeFantasia;
    private final String cnpj;
    private SituacaoCredenciamento situacaoCredenciamento;
    private final double valorPorCheckIn;

    /** Composição: o endereço não existe fora do estabelecimento. */
    private final Endereco endereco;

    /** Agregação compartilhada: a modalidade existe independentemente deste estabelecimento. */
    private final List<Modalidade> modalidades = new ArrayList<>();

    /** HU5 - instrutores contratados pelo estabelecimento. */
    private final List<Instrutor> instrutores = new ArrayList<>();

    /** Composição: a aula não existe sem o estabelecimento que a sedia. */
    private final List<Aula> aulas = new ArrayList<>();

    protected Estabelecimento(String nomeFantasia, String cnpj, double valorPorCheckIn,
                              Endereco endereco) {
        if (endereco == null) {
            throw new RegraDeNegocioException("Todo estabelecimento precisa de um endereço.");
        }
        this.nomeFantasia = nomeFantasia;
        this.cnpj = cnpj;
        this.valorPorCheckIn = valorPorCheckIn;
        this.endereco = endereco;
        this.situacaoCredenciamento = SituacaoCredenciamento.PENDENTE;
    }

    public String getNomeFantasia() { return nomeFantasia; }
    public String getCnpj() { return cnpj; }
    public double getValorPorCheckIn() { return valorPorCheckIn; }
    public SituacaoCredenciamento getSituacaoCredenciamento() { return situacaoCredenciamento; }
    public Endereco getEndereco() { return endereco; }

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

    /** UC03 / RN4 - o ponto (lat, lng) está dentro do raio permitido para check-in? */
    public boolean estaNoRaio(double latitude, double longitude) {
        return endereco.distanciaAte(latitude, longitude) <= RAIO_MAXIMO_METROS;
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

    /**
     * HU1 - cria uma aula na grade deste estabelecimento (Criador: composição).
     * As validações de data, modalidade e instrutor ficam no construtor de Aula.
     */
    public Aula agendarAula(LocalDateTime inicio, Duration duracao, int capacidade,
                            Modalidade modalidade, Instrutor instrutor) {
        Aula aula = new Aula(inicio, duracao, capacidade, this, modalidade, instrutor);
        aulas.add(aula);
        return aula;
    }

    public List<Modalidade> getModalidades() { return Collections.unmodifiableList(modalidades); }
    public List<Instrutor> getInstrutores() { return Collections.unmodifiableList(instrutores); }
    public List<Aula> getAulas() { return Collections.unmodifiableList(aulas); }

    @Override public String toString() { return nomeFantasia; }
}
