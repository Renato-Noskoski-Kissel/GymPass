package dominio.cadastroRede;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("QUADRA")
public class Quadra extends Estabelecimento {
    @Column(name = "coberta")
    private boolean coberta;

    /** Exigido pela JPA. */
    protected Quadra() { }

    public Quadra(String nomeFantasia, String cnpj, double valorPorCheckIn,
                  int nivelMinimo, Endereco endereco, boolean coberta) {
        super(nomeFantasia, cnpj, valorPorCheckIn, nivelMinimo, endereco);
        this.coberta = coberta;
    }

    public boolean isCoberta() { return coberta; }
}
