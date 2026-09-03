package dominio.cadastroRede;

public class Modalidade {
    private final String nome;
    private final String descricao;

    public Modalidade(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }

    @Override public String toString() { return nome; }
}
