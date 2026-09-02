package dominio.cadastroRede;

public class Instrutor {
    private final String nome;
    private final String registro;
    private final String especialidade;
    private final Estabelecimento estabelecimento;

    public Instrutor(String nome, String registro, String especialidade,
                     Estabelecimento estabelecimento) {
        this.nome = nome;
        this.registro = registro;
        this.especialidade = especialidade;
        this.estabelecimento = estabelecimento;
    }

    public String getNome() { return nome; }
    public String getRegistro() { return registro; }
    public String getEspecialidade() { return especialidade; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    /** Um instrutor só ministra aulas no estabelecimento que o contratou. */
    public boolean trabalhaEm(Estabelecimento outro) {
        return this.estabelecimento == outro;
    }

    @Override public String toString() { return nome; }
}
