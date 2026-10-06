package dominio.cadastroRede;

public class Estudio extends Estabelecimento {
    /** Estúdio trabalha com turmas menores; usado como padrão ao criar aulas. */
    private final int capacidadePadraoTurma;

    public Estudio(String nomeFantasia, String cnpj, double valorPorCheckIn,
                   Endereco endereco, int capacidadePadraoTurma) {
        super(nomeFantasia, cnpj, valorPorCheckIn, endereco);
        this.capacidadePadraoTurma = capacidadePadraoTurma;
    }

    public int getCapacidadePadraoTurma() { return capacidadePadraoTurma; }
}
