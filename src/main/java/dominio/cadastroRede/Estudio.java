package dominio.cadastroRede;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ESTUDIO")
public class Estudio extends Estabelecimento {
    /** Estúdio trabalha com turmas menores; usado como padrão ao criar aulas. */
    @Column(name = "capacidade_padrao_turma")
    private int capacidadePadraoTurma;

    /** Exigido pela JPA. */
    protected Estudio() { }

    public Estudio(String nomeFantasia, String cnpj, double valorPorCheckIn,
                   int nivelMinimo, Endereco endereco, int capacidadePadraoTurma) {
        super(nomeFantasia, cnpj, valorPorCheckIn, nivelMinimo, endereco);
        this.capacidadePadraoTurma = capacidadePadraoTurma;
    }

    public int getCapacidadePadraoTurma() { return capacidadePadraoTurma; }
}
