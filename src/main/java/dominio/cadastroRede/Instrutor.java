package dominio.cadastroRede;

import jakarta.persistence.*;

@Entity
@Table(name = "instrutores")
public class Instrutor {

    @Id
    @Column(name = "registro", length = 20)
    private String registro;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "especialidade", length = 80)
    private String especialidade;

    /** EAGER: Estabelecimento é abstrata; carregar já a subclasse real evita proxy. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estabelecimento_cnpj", nullable = false)
    private Estabelecimento estabelecimento;

    /** Exigido pela JPA. */
    protected Instrutor() { }

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
