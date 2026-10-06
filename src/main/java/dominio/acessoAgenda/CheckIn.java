package dominio.acessoAgenda;

import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Estabelecimento;
import java.time.LocalDateTime;

/**
 * UC03 - registro de que um aluno chegou a um estabelecimento.
 *
 * Criado por Adesao.registrarCheckIn depois de todas as validações. Nasce
 * pendente de validação; a confirmação na recepção é da iteração 3.
 *
 * As coordenadas de origem ficam registradas, mas não são expostas à empresa
 * contratante (RNF3).
 */
public class CheckIn {

    private final LocalDateTime dataHora;
    private final double latitudeOrigem;
    private final double longitudeOrigem;
    private SituacaoCheckIn situacaoValidacao;
    private final Aluno aluno;
    private final Estabelecimento estabelecimento;

    public CheckIn(Aluno aluno, Estabelecimento estabelecimento,
                   double latitudeOrigem, double longitudeOrigem) {
        this.aluno = aluno;
        this.estabelecimento = estabelecimento;
        this.latitudeOrigem = latitudeOrigem;
        this.longitudeOrigem = longitudeOrigem;
        this.dataHora = LocalDateTime.now();
        this.situacaoValidacao = SituacaoCheckIn.PENDENTE;
    }

    public LocalDateTime getDataHora() { return dataHora; }
    public double getLatitudeOrigem() { return latitudeOrigem; }
    public double getLongitudeOrigem() { return longitudeOrigem; }
    public SituacaoCheckIn getSituacaoValidacao() { return situacaoValidacao; }
    public Aluno getAluno() { return aluno; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    @Override public String toString() {
        return "Check-in de " + aluno + " em " + estabelecimento + " - " + dataHora
             + " [" + situacaoValidacao + "]";
    }
}
