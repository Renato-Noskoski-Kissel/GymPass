package dominio.planosAdesao;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Empresa;
import dominio.cadastroRede.Estabelecimento;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HU4 - nível de assinatura configurado pelo administrador.
 *
 * RF5 - a cobertura é por nível: o plano cobre todo estabelecimento cujo nível
 * mínimo seja igual ou menor que o nível do plano, com todas as aulas de lá.
 * Estabelecimento ou plano novo já entram na regra, sem configuração.
 */
@Entity
@Table(name = "planos")
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    @Column(name = "nivel", nullable = false)
    private int nivel;

    @Column(name = "valor_mensal", nullable = false)
    private double valorMensal;

    @Column(name = "limite_aulas_mes", nullable = false)
    private int limiteAulasMes;

    @Column(name = "permite_dependentes", nullable = false)
    private boolean permiteDependentes;

    /**
     * Empresas que oferecem este plano aos seus funcionários.
     * N:N unidirecional: Empresa não conhece Plano (dependência Planos -> Cadastro).
     */
    @ManyToMany
    @JoinTable(name = "planos_empresas",
               joinColumns = @JoinColumn(name = "plano_id"),
               inverseJoinColumns = @JoinColumn(name = "empresa_cnpj"))
    private List<Empresa> empresasQueOferecem = new ArrayList<>();

    /** Exigido pela JPA. */
    protected Plano() { }

    public Plano(String nome, int nivel, double valorMensal,
                 int limiteAulasMes, boolean permiteDependentes) {
        if (nivel < Estabelecimento.NIVEL_MIN || nivel > Estabelecimento.NIVEL_MAX) {
            throw new RegraDeNegocioException("O nível do plano deve estar entre "
                + Estabelecimento.NIVEL_MIN + " e " + Estabelecimento.NIVEL_MAX + ".");
        }
        this.nome = nome;
        this.nivel = nivel;
        this.valorMensal = valorMensal;
        this.limiteAulasMes = limiteAulasMes;
        this.permiteDependentes = permiteDependentes;
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public int getNivel() { return nivel; }
    public double getValorMensal() { return valorMensal; }
    public int getLimiteAulasMes() { return limiteAulasMes; }
    public boolean permiteDependentes() { return permiteDependentes; }

    public void oferecerPara(Empresa empresa) {
        if (!empresasQueOferecem.contains(empresa)) {
            empresasQueOferecem.add(empresa);
        }
    }

    /**
     * A empresa deixa de oferecer este plano aos funcionários. Quem ainda está
     * nele precisa ser trocado antes; essa checagem é da aplicação, que
     * consulta as adesões no repositório.
     */
    public void deixarDeOferecerPara(Empresa empresa) {
        if (!empresasQueOferecem.remove(empresa)) {
            throw new RegraDeNegocioException(empresa + " já não oferece o plano " + nome + ".");
        }
    }

    /** RN1 - o plano é oferecido pela empresa do aluno? */
    public boolean ehOferecidoPor(Empresa empresa) {
        return empresasQueOferecem.contains(empresa);
    }

    /** RF5 - o plano cobre este estabelecimento? */
    public boolean libera(Estabelecimento estabelecimento) {
        return nivel >= estabelecimento.getNivelMinimo();
    }

    public List<Empresa> getEmpresasQueOferecem() {
        return Collections.unmodifiableList(empresasQueOferecem);
    }

    @Override public String toString() { return nome; }
}
