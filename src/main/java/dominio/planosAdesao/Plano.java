package dominio.planosAdesao;

import dominio.cadastroRede.Empresa;
import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Modalidade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HU4 - nível de assinatura configurado pelo administrador.
 *
 * RF5 - o plano define quais estabelecimentos e quais modalidades libera.
 * O check-in exige o estabelecimento liberado; a reserva exige o
 * estabelecimento e a modalidade da aula.
 */
public class Plano {

    private final String nome;
    private final int nivel;
    private final double valorMensal;
    private final int limiteAulasMes;
    private final boolean permiteDependentes;

    /** Empresas que oferecem este plano aos seus funcionários. */
    private final List<Empresa> empresasQueOferecem = new ArrayList<>();

    /** RF5 - cobertura do plano. */
    private final List<Estabelecimento> estabelecimentosLiberados = new ArrayList<>();
    private final List<Modalidade> modalidadesLiberadas = new ArrayList<>();

    public Plano(String nome, int nivel, double valorMensal,
                 int limiteAulasMes, boolean permiteDependentes) {
        this.nome = nome;
        this.nivel = nivel;
        this.valorMensal = valorMensal;
        this.limiteAulasMes = limiteAulasMes;
        this.permiteDependentes = permiteDependentes;
    }

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

    /** RN1 - o plano é oferecido pela empresa do aluno? */
    public boolean ehOferecidoPor(Empresa empresa) {
        return empresasQueOferecem.contains(empresa);
    }

    /** RF5 - o administrador inclui um estabelecimento na cobertura do plano. */
    public void liberar(Estabelecimento estabelecimento) {
        if (!estabelecimentosLiberados.contains(estabelecimento)) {
            estabelecimentosLiberados.add(estabelecimento);
        }
    }

    /** RF5 - o administrador inclui uma modalidade na cobertura do plano. */
    public void liberar(Modalidade modalidade) {
        if (!modalidadesLiberadas.contains(modalidade)) {
            modalidadesLiberadas.add(modalidade);
        }
    }

    /** RF5 - o plano cobre este estabelecimento? */
    public boolean libera(Estabelecimento estabelecimento) {
        return estabelecimentosLiberados.contains(estabelecimento);
    }

    /** RF5 - o plano cobre esta modalidade? */
    public boolean libera(Modalidade modalidade) {
        return modalidadesLiberadas.contains(modalidade);
    }

    public List<Empresa> getEmpresasQueOferecem() {
        return Collections.unmodifiableList(empresasQueOferecem);
    }

    public List<Estabelecimento> getEstabelecimentosLiberados() {
        return Collections.unmodifiableList(estabelecimentosLiberados);
    }

    public List<Modalidade> getModalidadesLiberadas() {
        return Collections.unmodifiableList(modalidadesLiberadas);
    }

    @Override public String toString() { return nome; }
}
