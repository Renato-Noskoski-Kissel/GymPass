package dominio.cadastro;

public class Quadra extends Estabelecimento {
    private final boolean coberta;

    public Quadra(String nomeFantasia, String cnpj, double valorPorCheckIn, boolean coberta) {
        super(nomeFantasia, cnpj, valorPorCheckIn);
        this.coberta = coberta;
    }

    public boolean isCoberta() { return coberta; }
}
