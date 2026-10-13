package dominio.cadastroRede;

public class Quadra extends Estabelecimento {
    private final boolean coberta;

    public Quadra(String nomeFantasia, String cnpj, double valorPorCheckIn,
                  int nivelMinimo, Endereco endereco, boolean coberta) {
        super(nomeFantasia, cnpj, valorPorCheckIn, nivelMinimo, endereco);
        this.coberta = coberta;
    }

    public boolean isCoberta() { return coberta; }
}
