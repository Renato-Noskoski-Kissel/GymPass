package dominio.planosAdesao;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Empresa;
import dominio.cadastroRede.Estabelecimento;
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
public class Plano {

    private final String nome;
    private final int nivel;
    private final double valorMensal;
    private final int limiteAulasMes;
    private final boolean permiteDependentes;

    /** Empresas que oferecem este plano aos seus funcionários. */
    private final List<Empresa> empresasQueOferecem = new ArrayList<>();

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

    /** RF5 - o plano cobre este estabelecimento? */
    public boolean libera(Estabelecimento estabelecimento) {
        return nivel >= estabelecimento.getNivelMinimo();
    }

    public List<Empresa> getEmpresasQueOferecem() {
        return Collections.unmodifiableList(empresasQueOferecem);
    }

    @Override public String toString() { return nome; }
}
