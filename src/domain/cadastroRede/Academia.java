package dominio.cadastro;

public class Academia extends Estabelecimento {
    private final boolean funciona24Horas;

    public Academia(String nomeFantasia, String cnpj, double valorPorCheckIn,
                    boolean funciona24Horas) {
        super(nomeFantasia, cnpj, valorPorCheckIn);
        this.funciona24Horas = funciona24Horas;
    }

    public boolean isFunciona24Horas() { return funciona24Horas; }
}
