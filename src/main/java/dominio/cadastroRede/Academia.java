package dominio.cadastroRede;

public class Academia extends Estabelecimento {
    private final boolean funciona24Horas;

    public Academia(String nomeFantasia, String cnpj, double valorPorCheckIn,
                    int nivelMinimo, Endereco endereco, boolean funciona24Horas) {
        super(nomeFantasia, cnpj, valorPorCheckIn, nivelMinimo, endereco);
        this.funciona24Horas = funciona24Horas;
    }

    public boolean isFunciona24Horas() { return funciona24Horas; }
}
