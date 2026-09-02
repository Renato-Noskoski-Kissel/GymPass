package dominio.cadastro;

public class Estudio extends Estabelecimento {
    /** Estúdio trabalha com turmas menores; usado como padrão ao criar aulas. */
    private final int capacidadePadraoTurma;

    public Estudio(String nomeFantasia, String cnpj, double valorPorCheckIn,
                   int capacidadePadraoTurma) {
        super(nomeFantasia, cnpj, valorPorCheckIn);
        this.capacidadePadraoTurma = capacidadePadraoTurma;
    }

    public int getCapacidadePadraoTurma() { return capacidadePadraoTurma; }
}
