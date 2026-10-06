package dominio.cadastroRede;

public class Quadra extends Estabelecimento {
    private final boolean coberta;

    public Quadra(String nomeFantasia, String cnpj, double valorPorCheckIn,
                  Endereco endereco, boolean coberta) {
        super(nomeFantasia, cnpj, valorPorCheckIn, endereco);
        this.coberta = coberta;
    }

    public boolean isCoberta() { return coberta; }
}
