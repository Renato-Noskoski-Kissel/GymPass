package dominio.cadastroRede;

import jakarta.persistence.*;

/**
 * Persistência: a FK estabelecimento_cnpj é mantida pelo lado do
 * Estabelecimento (@OneToMany unidirecional), então a Modalidade não
 * precisa conhecer o estabelecimento.
 */
@Entity
@Table(name = "modalidades")
public class Modalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    /** Exigido pela JPA. */
    protected Modalidade() { }

    public Modalidade(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }

    @Override public String toString() { return nome; }
}
