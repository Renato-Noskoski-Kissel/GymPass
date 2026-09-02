package dominio.planosAdesao;

import dominio.cadastroRede.Empresa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HU4 - nível de assinatura configurado pelo administrador.
 *
 * As liberações de modalidade e de estabelecimento (RF5) entram na
 * iteração 2, junto com a reserva de aulas e o check-in.
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

    public List<Empresa> getEmpresasQueOferecem() {
        return Collections.unmodifiableList(empresasQueOferecem);
    }

    @Override public String toString() { return nome; }
}
