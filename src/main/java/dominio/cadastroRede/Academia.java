package dominio.cadastroRede;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ACADEMIA")
public class Academia extends Estabelecimento {
    @Column(name = "funciona24_horas")
    private boolean funciona24Horas;

    /** Exigido pela JPA. */
    protected Academia() { }

    public Academia(String nomeFantasia, String cnpj, double valorPorCheckIn,
                    int nivelMinimo, Endereco endereco, boolean funciona24Horas) {
        super(nomeFantasia, cnpj, valorPorCheckIn, nivelMinimo, endereco);
        this.funciona24Horas = funciona24Horas;
    }

    public boolean isFunciona24Horas() { return funciona24Horas; }
}
